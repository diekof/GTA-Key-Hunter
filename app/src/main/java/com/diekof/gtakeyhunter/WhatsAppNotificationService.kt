package com.diekof.gtakeyhunter
import android.app.Notification
import android.graphics.Bitmap
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

class WhatsAppNotificationService : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn?.packageName !in setOf("com.whatsapp", "com.whatsapp.w4b")) return
        val store = Store(this)
        if (store.channel.isBlank()) return
        val extras = sbn!!.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val message = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (!title.contains(store.channel, ignoreCase = true)) return

        scope.launch {
            val picture = notificationPicture(extras)
            val imageCodes = picture?.let { bitmap ->
                try {
                    KeyEngine.recognizeGameKey(bitmap)
                } finally {
                    bitmap.recycle()
                }
            }.orEmpty()
            val codes = (KeyEngine.extract(message) + imageCodes).distinct()
            val found = store.add(codes, "Notificação: ${store.channel}")
            if (found > 0) {
                ClipboardHelper.copyCode(this@WhatsAppNotificationService, codes.first())
                store.lastEvent = "$found código(s) na notificação; código copiado"
            } else if (picture != null) {
                store.lastEvent = "Imagem da notificação analisada; nenhum código encontrado"
            } else {
                store.lastEvent = "Notificação recebida; imagem não disponível para OCR"
            }
            Events.ping()
        }
    }

    private fun notificationPicture(extras: android.os.Bundle): Bitmap? {
        return if (Build.VERSION.SDK_INT >= 33) {
            extras.getParcelable(Notification.EXTRA_PICTURE, Bitmap::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable(Notification.EXTRA_PICTURE)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
