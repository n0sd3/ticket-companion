# Ticket (app Android)

App Android 6+ (API 23) do Ticket CRM com duas funções independentes, no mesmo APK. O app se chama **Ticket** (`br.com.ticket.app`); "Companion" é o nome da função de captura de PIX, e o código Kotlin continua no pacote `br.com.ticket.companion`.

- **Companion (captura de PIX):** captura notificações dos bancos escolhidos pelo usuário e envia eventos de **PIX recebido** para uma empresa do Ticket CRM. O CRM decide a conciliação; o Companion não confirma cobranças sozinho.
- **Atendimento web:** abre o Ticket (a SPA) da empresa dentro do app, com upload/câmera, downloads, localização e notificações nativas — substitui o antigo app WebView do CRM.

## Configurar

1. No Ticket da empresa, abra a gestão de Companion e gere um token de aparelho.
2. Informe no app `https://empresa.seu-dominio` e o token `tcmp_…`.
3. Toque em **Verificar**, confira o nome da empresa e depois em **Confirmar**.
4. Conceda o acesso a notificações pela Home e selecione os bancos em **Configurações → Adicionar app**. Nenhum aplicativo é monitorado por padrão.
5. Se desejar alertas de envio, habilite a permissão correspondente em Configurações.

O servidor precisa resolver o subdomínio da empresa e expor `/backend/companion/me`, `/backend/companion/pix-events` e `/backend/companion/heartbeat`. O domínio principal sem tenant é recusado. O token é vinculado ao UUID desta instalação.

**Trocar empresa** aposenta a conexão e abre o setup completo. Seus eventos antigos continuam no histórico, identificados pela empresa anterior; nunca são enviados para a nova empresa. Após verificar novamente o mesmo host e slug, é possível escolher **Confirmar e reenviar pendentes desta empresa**. O `eventId` é preservado.

## Atendimento web

- **Aparelho só de atendimento** (atendente, entregador): informe `https://empresa.seu-dominio` no setup e toque em **Abrir só o atendimento**. Nas próximas aberturas o app entra direto no atendimento; **Ajustes do app** (menu ⋮ do atendimento) volta ao setup. Nesse modo o listener de notificações permanece **desligado**: o app nem aparece em "Acesso às notificações".
- **Aparelho do caixa** (com Companion vinculado): o botão de globo na Home abre o atendimento da mesma empresa do Companion — o endereço vem da conexão verificada e não é digitado de novo.
- O WebView fica **travado na origem da empresa** (esquema, host e porta). Outros sites abrem no navegador do aparelho; esquemas como `file:`, `content:`, `intent:` e `javascript:` são bloqueados. Câmera, microfone, localização e a ponte de notificações só valem para essa origem (câmera/microfone/localização são pedidos em tempo de execução, no primeiro uso).
- Roda no processo `:web`, separado do Companion: um estouro de memória do WebView não derruba a captura de PIX. Enquanto o atendimento está aberto em segundo plano, um serviço em primeiro plano ("Atendimento ativo") mantém o processo vivo; ele para quando você sai do atendimento e o Android 15 limita esse tipo de serviço (cerca de 6 h por dia).
- A ponte `window.Notification` → notificação nativa continua igual à do app antigo, mas com texto limitado em tamanho e aceita só da origem travada.

Limite conhecido: sem token FCM, notificações com o app fechado dependem de a sessão web continuar conectada. Web Push não existe dentro de WebView.

## Captura e entrega

- O parser genérico pt-BR reconhece entradas de PIX com valor em centavos inteiros. Regras de falha, devolução, pendência e saída têm prioridade. Só um crédito (`PIX_RECEIVED` ou `TRANSFER_RECEIVED`) `+ INCOMING + valor único positivo + MEDIUM/HIGH` é enviado.
- Alguns bancos nunca escrevem "Pix": o Nubank PJ avisa "Transferência recebida na conta PJ". Isso é gravado localmente como `TRANSFER_RECEIVED` (o que o banco disse) e vai ao CRM como `PIX_RECEIVED`, o único crédito que o CRM concilia. Como o app não consegue distinguir Pix de TED nesse texto, a confiança é sempre `MEDIUM`.
- Um evento que fica em "Capturado (não enviado)" mostra, nos detalhes, **por que** não foi enviado.
- **Não existem parsers bancários HIGH nesta versão:** faltam amostras reais anonimizadas. O genérico produz MEDIUM; casos não reconhecidos ficam no histórico e não são enviados.
- O fingerprint inclui pacote, título/texto normalizados, valor e minuto da notificação. Repostagens no mesmo minuto são deduplicadas. Dois recebimentos de conteúdo idêntico no mesmo minuto também podem colidir: esta é uma limitação conhecida dessa heurística.
- O evento é persistido antes do envio imediato (timeout HTTP de 10 segundos). Um worker expedited já fica agendado como recuperação. Listener e worker compartilham o mesmo sincronizador e `eventId`.
- A fila envia até 20 eventos por chamada. O próximo horário elegível de cada evento usa backoff de 30 segundos até 1 hora (ou `Retry-After` maior) e termina após 20 tentativas por evento. O backoff nativo do WorkManager e o Android podem executar depois desse horário: não há garantia de execução dentro de 1 hora. Falhas terminais podem ser reenviadas explicitamente no histórico.
- 401/403 e redirects pedem reconexão. 400/409 são terminais; `ERROR` de um item não invalida os irmãos do lote. `REJECTED` é um resultado entregue ao CRM.
- Heartbeat informa acesso ao listener e versão do app a cada evento e pelo WorkManager a cada 15 minutos. O Android pode adiar trabalho em segundo plano; não é um relógio exato.

O histórico fica na Home, com resultado do CRM e detalhes locais. Configurações contém os últimos 100 logs de envio (retenção de 30 dias) e exportação de diagnóstico escolhida pelo seletor de arquivos do Android. A exportação contém IDs técnicos, valor, classificação e status; **não contém texto bancário, nome do pagador, referências bancárias ou credenciais**.

## Segurança e armazenamento

App novo `br.com.ticket.app`, Room v1 `ticket_companion.db` e preferências criptografadas `ticket_companion_secure_prefs`: não há migração de dados de versões anteriores. O schema está em `app/schemas` e inclui `captured_events`, `app_configs` e `sync_logs`.

O WebView do atendimento não tem `allowBackup`, não acessa `file:`/`content:` e bloqueia conteúdo misto. O release aceita HTTPS, sem IP literal, userinfo, query ou fragmento. O path digitado é descartado: o prefixo da API é sempre `/backend/`. Credenciais só são anexadas ao origin verificado (esquema, host, porta). Redirects são bloqueados; release não tem HTTP logging. Debug libera cleartext apenas para `localhost` e `10.0.2.2`. Backups e transferência das credenciais/banco estão desabilitados.

## Compatibilidade (Android 6 em diante)

- `minSdk 23`, o mesmo piso do app WebView antigo: atendentes com Android 6–10 usam o app normalmente. `java.time` usa *core library desugaring*.
- Trabalho *expedited* do WorkManager só é pedido no Android 12+ (antes exigiria serviço em primeiro plano); nas versões anteriores o envio imediato do listener e o worker comum cobrem o caso. Canais de notificação só existem no Android 8+. O ícone adaptativo é do Android 8+; antes disso vale um ícone simples.
- `usesCleartextTraffic="false"` é explícito porque a configuração de rede (`network_security_config`) só vale a partir do Android 7.
- Riscos que só um aparelho antigo confirma: chaves do Android Keystore mais frágeis no Android 6–7 (o `EncryptedSharedPreferences` pode falhar em alguns fabricantes e o app não tem plano B), cadeia de certificados da Let's Encrypt em Android anterior ao 7.1.1 (depende do certificado que o Cloudflare serve) e um *Android System WebView* desatualizado, que pode não rodar a SPA.
- Instalar este app **não atualiza** o antigo `uk.edsonnet.ticketz`: os IDs são diferentes, então os dois coexistem até você desinstalar o antigo (a sessão web precisa de novo login).

## Desenvolvimento

Kotlin/Compose, Hilt, Room, Retrofit/OkHttp e WorkManager; JDK 17 e Android SDK 35.

```bash
./gradlew testDebugUnitTest lintDebug
./gradlew assembleRelease \
  -Pandroid.injected.signing.store.file=/caminho/keystore.jks \
  -Pandroid.injected.signing.store.password=... \
  -Pandroid.injected.signing.key.alias=... \
  -Pandroid.injected.signing.key.password=...
```

Testes locais usam JUnit, Robolectric, Room em memória e MockWebServer. A compilação release usa R8; as classes Gson da API e das conexões possuem regras de preservação. O APK sai em `app/build/outputs/apk/release/app-release.apk`.

Versão atual: `versionName=1.5.5`, `versionCode=12` (a atualização pelo app só reconhece versão maior que a instalada). Não representa uma publicação até existir a tag. Um APK de validação assinado com chave debug local não é um release distribuível.

Antes de produção, validar em aparelho real: acesso ao listener, notificação de banco real anonimizada, captura durante suspensão, recuperação offline, token de outra empresa, troca de empresa e os resultados MATCHED/AMBIGUOUS/UNMATCHED no CRM. Não houve deploy ou teste E2E contra a stack nesta etapa.

## Licença

Distribuído sob a **GNU Affero General Public License v3.0** (`LICENSE`), a mesma do Ticket CRM. Quem
distribui uma versão modificada precisa oferecer o código-fonte correspondente aos usuários. O
[código-fonte](https://github.com/n0sd3/ticket-companion) também é acessível dentro do app, em
**Configurações**.
