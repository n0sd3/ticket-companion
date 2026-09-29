# Changelog

Mudanças notáveis do app Android do Ticket CRM. Formato baseado em
[Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).

## 1.5.5 (código 12) — local, sem tag

- **Captura:** o app pede o vínculo do listener de notificações ao abrir, a cada heartbeat (15 min) e no
  boot quando ele está habilitado e desconectado. Num Xiaomi/HyperOS o listener ficava aprovado mas fora da
  lista de listeners vivos do Android, e a captura de PIX parava sem nenhum aviso.

## 1.5.4 (código 11) — local, sem tag

- **Diagnóstico da captura:** Ajustes → "Diagnóstico da captura" mostra cada etapa (acesso a notificações,
  listener conectado, empresa vinculada, apps monitorados) e o destino da última notificação vista
  (sem texto, app não monitorado, capturada…). Guarda só pacote e horário, nunca o texto.

## 1.5.3 (código 10) — local, sem tag

- **Captura:** o listener volta a pedir reconexão quando o Android o desconecta e passa a receber também
  notificações silenciosas (o Nubank deixou de capturar). Lê o texto de `TEXT_LINES`/`SUB_TEXT` quando
  `BIG_TEXT`/`TEXT` vêm vazios e registra no log as notificações descartadas.
- **Atendimento:** a barra de título só aparece na tela de login.
- **Atualização pelo app:** Ajustes → "Atualizar aplicativo" baixa a última release do GitHub e abre o
  instalador do Android (exige `REQUEST_INSTALL_PACKAGES`).

## 1.5.2 (código 9) — base local, sem publicação

Versão preservada por decisão do projeto. Esta entrada resume o app até aqui; nada foi
publicado, marcado com tag nem distribuído.

### Captura de PIX (Companion)

- Cada instalação é vinculada ao subdomínio de **uma** empresa do Ticket CRM. O setup verifica
  o par endereço + token (`/backend/companion/me`) e só grava a conexão depois de confirmado.
- Classifica notificações de banco em tipo, direção e confiança, com valores em centavos
  inteiros. Só PIX recebido, com valor positivo e confiança média ou alta, é enviado.
- Fila persistente (Room) com deduplicação por fingerprint, envio imediato + worker, lotes de
  20, retry com backoff limitado e resultado da conciliação por evento.
- Rede presa ao host verificado: sem redirects, sem `Authorization` para outro destino, sem
  log HTTP no release. Eventos capturados sob uma empresa nunca são enviados a outra.
- Heartbeat, histórico com resultado do CRM, logs de envio e exportação de diagnóstico sem
  texto bancário, pagador ou credenciais.

### Atendimento web

- Abre o Ticket da empresa dentro do app (upload, câmera, download, localização e notificações
  nativas), substituindo o app WebView antigo. Roda no processo `:web`, com a origem travada.
- Aparelho só de atendimento não liga o listener de notificações.

### Plataforma

- `minSdk` 23 (Android 6), `targetSdk` 35. `applicationId` `br.com.ticket.app`, rótulo "Ticket".
- Interface, tema, ícones e camada de dados escritos para este projeto.
- Testes: JUnit, Robolectric (SDK 23 e 35), Room em memória e MockWebServer.
