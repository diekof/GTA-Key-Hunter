# GTA Key Hunter

Aplicativo Android experimental em Kotlin para detecção local de possíveis vouchers PlayStation.

## Funcionalidades
- Seleção de imagem e OCR com Google ML Kit
- Detecção de códigos alfanuméricos de 12 caracteres
- Filtro de notificações do WhatsApp pelo nome do canal
- Captura de tela autorizada com MediaProjection (sessão iniciada pelo usuário)
- Histórico local e deduplicação
- Abertura do PS App e cópia de código; resgate final manual

## Limitações
Notificações de canais do WhatsApp podem não conter imagens nem chegar para cada publicação. O serviço de acessibilidade não realiza resgate automático; não há acesso privado a APIs da Sony. O usuário deve confirmar qualquer resgate. A captura de tela exige autorização explícita e não funciona invisivelmente em segundo plano.

## Build e instalação no Android
O projeto é exclusivo para Android. O build usa Java 17, Android SDK 35 e Gradle 8.9.

No GitHub, execute o workflow `Android debug APK` em **Actions > Run workflow**. Depois baixe o artifact `GTAKeyHunter-debug-apk` e extraia o `app-debug.apk`.

Com um celular conectado por USB e a depuração USB autorizada, a instalação pode ser feita com:

```bash
adb install -r app-debug.apk
```

Após instalar, o usuário precisa habilitar manualmente o acesso às notificações e, quando usar esse recurso, a acessibilidade e a captura de tela. O app não executa resgates automaticamente.
