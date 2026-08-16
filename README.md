# ArkMoney

ArkMoney is a private, local-first expense tracker for Android, built with
Material 3.

> Current release: **0.1.0**. Published APKs are available through GitHub
> Releases.

## Download

Install the latest APK from
[GitHub Releases](https://github.com/ejevgg/ArkMoney/releases). Android may ask
for permission to install applications from the browser or file manager used to
open the APK.

## Features

- fast expense entry with a built-in expression calculator;
- custom categories with emoji icons;
- multiple accounts and a live balance for the selected account;
- expense names, descriptions and private photo attachments;
- history grouped by day, search and date navigation;
- week, month, quarter and year analytics;
- Excel export and round-trip import;
- reversible demo data covering the latest year;
- system, light and dark themes;
- Material 3 interface and Android 12 themed icon support.

## Privacy

ArkMoney works without an account, analytics or network access. Financial data
and attached photos remain in local application storage unless the user
explicitly exports an Excel workbook.

Uninstalling the application removes its local database and private photos.
Export current data before uninstalling or changing devices.

## Requirements

- Android 10 (API 29) or newer.

## Building

Open the repository in a current Android Studio version and let Gradle Sync
finish. To run the local verification suite:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Release builds are signed only when a local `signing/signing.properties` file
is present. Signing credentials and key stores are excluded from version
control. Preserve the release key securely: the same key is required for
compatible updates.

## Technology

- Kotlin;
- Jetpack Compose and Material 3;
- Room;
- Gradle Kotlin DSL;
- XML-based `.xlsx` import and export without a network service.

## Project status

ArkMoney 0.1.0 is an early local-first release. See [CHANGELOG.md](CHANGELOG.md)
for release notes.

- [Contributing](CONTRIBUTING.md)
- [Security policy](SECURITY.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Product behavior](docs/PRODUCT.md)
