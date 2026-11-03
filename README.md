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

## Build
GitHub Actions: Actions > Android debug APK > Run workflow, ou push na main. Baixe o artifact GTAKeyHunter-debug-apk e extraia o app-debug.apk. O build não está validado até o workflow terminar com sucesso.
