package com.diekof.gtakeyhunter
import android.Manifest
import android.app.Activity
import android.content.*
import android.graphics.BitmapFactory
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
class MainActivity : ComponentActivity() {
    private lateinit var store: Store
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store=Store(this)
        if (Build.VERSION.SDK_INT>=33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),8)
        setContent {
            var channel by remember { mutableStateOf(store.channel) }
            var event by remember { mutableStateOf(store.lastEvent) }
            var history by remember { mutableStateOf(store.recent()) }
            var manual by remember { mutableStateOf("") }
            fun refresh() { event=store.lastEvent; history=store.recent() }
            LaunchedEffect(Unit) { Events.changed.collect { refresh() } }
            val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri!=null) lifecycleScope.launch {
                    try {
                        val bitmap=contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                        if (bitmap!=null) {
                            val codes=KeyEngine.recognize(bitmap); bitmap.recycle()
                            val count=store.add(codes,"Imagem selecionada")
                            store.lastEvent="OCR: ${codes.size} candidato(s), $count novo(s)"
                            refresh()
                        }
                    } catch (e: Exception) { store.lastEvent="Falha OCR: ${e.javaClass.simpleName}"; refresh() }
                }
            }
            val projection=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode==Activity.RESULT_OK && result.data!=null) {
                    val intent=Intent(this,ScreenCaptureService::class.java)
                        .putExtra(ScreenCaptureService.EXTRA_RESULT,result.resultCode)
                        .putExtra(ScreenCaptureService.EXTRA_DATA,result.data)
                    startForegroundService(intent)
                }
            }
            MaterialTheme(colorScheme=darkColorScheme()) {
                Scaffold(topBar={ Text("GTA Key Hunter • MVP", modifier=Modifier.padding(20.dp),style=MaterialTheme.typography.titleLarge) }) { padding ->
                    LazyColumn(modifier=Modifier.fillMaxSize().padding(padding).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        item {
                            Text("Nome do canal do WhatsApp")
                            OutlinedTextField(value=channel,onValueChange={channel=it},modifier=Modifier.fillMaxWidth(),label={Text("Canal")})
                            Button(onClick={store.channel=channel; toast("Canal salvo")}) { Text("Salvar canal") }
                        }
                        item {
                            Text("Último evento: $event")
                            Button(onClick={startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))}) { Text("Permitir notificações") }
                            Text("Imagens podem não estar disponíveis em notificações.")
                        }
                        item {
                            Button(onClick={picker.launch("image/*")}) { Text("Selecionar imagem e executar OCR") }
                            Button(onClick={
                                val mgr=getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                                projection.launch(mgr.createScreenCaptureIntent())
                            }) { Text("Iniciar captura autorizada") }
                            OutlinedButton(onClick={startService(Intent(this@MainActivity,ScreenCaptureService::class.java).setAction(ScreenCaptureService.ACTION_STOP))}) { Text("Parar captura") }
                        }
                        item {
                            HorizontalDivider()
                            Text("PlayStation • resgate manual assistido")
                            Button(onClick={
                                val launch=listOf("com.scee.psxandroid", "com.playstation")
                                    .asSequence()
                                    .mapNotNull(packageManager::getLaunchIntentForPackage)
                                    .firstOrNull()
                                if(launch!=null) startActivity(launch) else toast("PS App não encontrado")
                            }) { Text("Abrir PS App") }
                            OutlinedButton(onClick={startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}) { Text("Configurar acessibilidade") }
                            OutlinedTextField(value=manual,onValueChange={manual=it},label={Text("Código para copiar")},modifier=Modifier.fillMaxWidth())
                            Button(onClick={
                                val code=KeyEngine.extract(manual).firstOrNull()
                                if(code==null) toast("Código inválido") else {
                                    val clipboard=getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Voucher PlayStation",KeyEngine.formatted(code)))
                                    toast("Código copiado")
                                }
                            }) { Text("Copiar código") }
                        }
                        item { HorizontalDivider(); Text("Histórico mascarado") }
                        items(history) { Text(it) }
                        item { OutlinedButton(onClick={store.clear();refresh()}) { Text("Apagar histórico") } }
                        item { Text("Não resgata automaticamente. Captura exige consentimento e pode parar ao mudar de aplicativo.") }
                    }
                }
            }
        }
    }
    private fun toast(message:String)=Toast.makeText(this,message,Toast.LENGTH_SHORT).show()
}
