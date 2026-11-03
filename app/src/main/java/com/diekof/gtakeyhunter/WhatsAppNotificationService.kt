package com.diekof.gtakeyhunter
import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
class WhatsAppNotificationService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn?.packageName !in setOf("com.whatsapp", "com.whatsapp.w4b")) return
        val store = Store(this)
        if (store.channel.isBlank()) return
        val extras = sbn!!.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val message = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (!title.contains(store.channel, ignoreCase = true)) return
        val found = store.add(KeyEngine.extract(message), "Notificação: ${store.channel}")
        store.lastEvent = if (found > 0) "$found código(s) na notificação" else "Notificação do canal recebida; imagem pode estar indisponível"
        Events.ping()
    }
}