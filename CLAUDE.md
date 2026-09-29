# Ticket Companion — instruções para o Claude

Leia `AGENTS.md` (regras do app, plataforma e versionamento). Regras extras abaixo.

## Sempre entregue o APK atualizado

Toda alteração de código no app termina com um APK novo, testado e pronto para instalar. Não pare no código.

1. **Suba a versão** em `app/build.gradle.kts` (`versionName` + `versionCode`, este sempre maior que o anterior),
   e atualize `CHANGELOG.md`, o `README.md` e o texto de fallback da versão em `SettingsViewModel`.
   O botão "Atualizar aplicativo" só enxerga versão MAIOR que a instalada; sem bump o celular não atualiza.
2. **Teste primeiro** (regra do `AGENTS.md`) e depois gere, da raiz do repositório:
   ```bash
   docker run --rm -v "$PWD":/workspace -v ticket-companion-gradle:/root/.gradle -v ticket-companion-m2:/root/.m2 \
     -v ticket-companion-android:/root/.android -w /workspace ticket-companion-build:local \
     ./gradlew --no-daemon --console=plain testDebugUnitTest lintDebug assembleDebug > build.log 2>&1
   ```
   O volume `ticket-companion-android` guarda a chave de debug persistente: sem ele o APK não instala por cima do
   anterior. Só entregue com `BUILD SUCCESSFUL`. Se a imagem `ticket-companion-build:local` sumir, recrie
   (JDK 17 + cmdline-tools + `platforms;android-35` + `build-tools;35.0.0`).
3. **Nomeie** o APK `ticket-vX.Y.Z.apk` (copie de `app/build/outputs/apk/debug/app-debug.apk`) e mande ao usuário.
4. **Publique a release sem GitHub Actions** (o token do `gh` no servidor não tem o escopo `workflow`):
   ```bash
   GH_CONFIG_DIR=/DATA/.local/gh-config gh release create vX.Y.Z ticket-vX.Y.Z.apk \
     --repo n0sd3/ticket-companion --target master --title "Ticket X.Y.Z" --notes-file <trecho do CHANGELOG>
   ```
   O app procura o `.apk` na última release em `github.com/n0sd3/ticket-companion/releases/download/`. Publicar
   (push, tag, release) só com autorização do usuário; gerar e enviar o APK é sempre.

## Git e push

- Não alterar `.github/workflows/*` daqui: o push é recusado sem o escopo `workflow`.
- Push sem `credential.helper` configurado: `git -c credential.helper= -c 'credential.helper=!gh auth git-credential' push`
  com `GH_CONFIG_DIR=/DATA/.local/gh-config`.
- Sem identidade git no repositório: use `-c user.name=n0sd3 -c user.email=ed.cleubert@gmail.com` por comando.

## Depurar no aparelho

`adb` moderno no container `adb-modern` (porta 5038): `docker exec adb-modern /opt/pt/platform-tools/adb -P 5038 ...`.
Pareamento/conexão por Wi-Fi (Tailscale): `adb pair IP:PORTA CODIGO`, depois `adb connect IP:PORTA_DE_CONEXAO`.
Nunca imprima texto de notificação bancária nem nome de pagador.
