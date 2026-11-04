package com.diekof.gtakeyhunter
import android.app.*
import android.content.*
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import kotlinx.coroutines.*
class ScreenCaptureService : Service() {
    companion object {
        const val ACTION_STOP="stop"
        const val EXTRA_RESULT="result"
        const val EXTRA_DATA="data"
    }
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
    private var reader:ImageReader?=null
    private var projection:MediaProjection?=null
    private var display:android.hardware.display.VirtualDisplay?=null
    @Volatile private var busy=false
    private var lastScan=0L
    override fun onBind(intent:Intent?):IBinder?=null
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(intent?.action==ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        val nm=getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("capture","Captura de tela",NotificationManager.IMPORTANCE_LOW))
        val stop=PendingIntent.getService(this,2,Intent(this,ScreenCaptureService::class.java).setAction(ACTION_STOP),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification=Notification.Builder(this,"capture").setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("GTA Key Hunter capturando tela").setContentText("Use Parar para encerrar")
            .addAction(Notification.Action.Builder(null,"Parar",stop).build()).build()
        startForeground(17,notification,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        val data=if(Build.VERSION.SDK_INT>=33) intent?.getParcelableExtra(EXTRA_DATA,Intent::class.java)
            else intent?.getParcelableExtra<Intent>(EXTRA_DATA)
        if(data==null) { stopSelf(); return START_NOT_STICKY }
        val mgr=getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection=mgr.getMediaProjection(intent?.getIntExtra(EXTRA_RESULT,Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED,data)
        val p=projection ?: run { stopSelf();return START_NOT_STICKY }
        p.registerCallback(object:MediaProjection.Callback() { override fun onStop(){stopSelf()} },Handler(Looper.getMainLooper()))
        val dm=resources.displayMetrics
        val width=dm.widthPixels;val height=dm.heightPixels
        reader=ImageReader.newInstance(width,height,PixelFormat.RGBA_8888,2)
        reader!!.setOnImageAvailableListener({ r ->
            val image=r.acquireLatestImage() ?: return@setOnImageAvailableListener
            if(busy || System.currentTimeMillis()-lastScan<1500L){image.close();return@setOnImageAvailableListener}
            busy=true;lastScan=System.currentTimeMillis()
            try {
                val plane=image.planes[0]
                val padded=Bitmap.createBitmap(plane.rowStride/plane.pixelStride,height,Bitmap.Config.ARGB_8888)
                padded.copyPixelsFromBuffer(plane.buffer)
                val bitmap=Bitmap.createBitmap(padded,0,0,width,height)
                padded.recycle()
                scope.launch {
                    try {
                        val codes=KeyEngine.recognizeGameKey(bitmap)
                        if(codes.isNotEmpty()){
                            val store=Store(this@ScreenCaptureService)
                            val found=store.add(codes,"Captura autorizada")
                            if(found>0){
                                ClipboardHelper.copyCode(this@ScreenCaptureService, codes.first())
                                store.lastEvent="$found código(s) novo(s); código copiado"
                                Events.ping()
                                notifyKey()
                            }
                        }
                    } finally {bitmap.recycle();busy=false}
                }
            }catch(_:Exception){busy=false}
            finally{image.close()}
        },Handler(Looper.getMainLooper()))
        display=p.createVirtualDisplay("GTAKeyHunter",width,height,dm.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader!!.surface,null,null)
        return START_NOT_STICKY
    }
    private fun notifyKey(){
        val nm=getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("found","Códigos encontrados",NotificationManager.IMPORTANCE_HIGH))
        val pending=PendingIntent.getActivity(this,3,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        nm.notify(19,Notification.Builder(this,"found").setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Código copiado para a área de transferência")
            .setContentText("Abra o PS App e cole para resgatar manualmente")
            .setContentIntent(pending).setAutoCancel(true).build())
    }
    override fun onDestroy(){
        reader?.setOnImageAvailableListener(null,null)
        display?.release();reader?.close();projection?.stop();scope.cancel()
        super.onDestroy()
    }
}
