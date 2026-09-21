# Repository instructions

Android app for the Ticket CRM (`applicationId` `br.com.ticket.app`, label "Ticket"). One APK, two
independent features: PIX capture (the "Companion") and the web attendance client. Code lives in the
Kotlin package `br.com.ticket.companion` (namespace); the application id is different on purpose.
Licensed under AGPL-3.0 (see `LICENSE`).

## Capture (Companion)

- One installation is bound to one company subdomain. Verify `/backend/companion/me` before storing
  credentials; a `connectionId` is created per binding and events never cross connections. Pending
  events may only be reassigned explicitly, for the same canonical host and company slug.
- Money is integer cents (`Long`). Only incoming, received PIX with a positive amount and MEDIUM/HIGH
  confidence may sync. Do not invent bank fixtures or HIGH-confidence templates.
- Never log or export banking text, payer names or credentials.
- The notification listener ships `enabled=false`; only `CompanionComponents` turns it on, once a
  company was bound.

## Web attendance (`ui/web`, `domain/web`)

- Runs in the `:web` process. Do not inject Hilt/Room/`SecureStorage`/`ConnectionManager` into
  `WebActivity`, `TicketWebService` or `WebNotifications` (SharedPreferences does not sync across
  processes). The origin arrives through the intent and is re-validated.
- Every navigation and permission decision goes through `WebNavigationPolicy`. Do not add a JS
  interface, a permission grant or a navigation path that ignores the locked origin.

## Platform

- `minSdk` is 23. Anything above it needs a guard or a compat call; lint `NewApi`/`InlinedApi` must
  stay at zero. Never request expedited work below Android 12 (`SyncScheduling`).
- Keep the Room schema exported in `app/schemas`, the Gson models and the `@JavascriptInterface`
  method under R8 rules (`proguard-rules.pro`).

## Workflow

- Write the failing test first, see it fail, then implement.
- Verify with `./gradlew testDebugUnitTest lintDebug assembleRelease`; release needs a signing key
  passed as `-Pandroid.injected.signing.*` properties (a local debug key is fine for validation only).
- Versioning: `versionName`/`versionCode` in `app/build.gradle.kts`. Change them only for an authorized
  release, then update `CHANGELOG.md`, commit as `chore(release): X.Y.Z` and tag `vX.Y.Z`; the tag
  triggers `.github/workflows/build-release.yml`. Do not tag, push or move tags without authorization.
