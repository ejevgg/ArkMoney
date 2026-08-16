# ArkMoney

ArkMoney is a private, local-first expense tracker for Android, built with
Material 3.

> Current release: **0.2.0**. Published APKs are available through GitHub
> Releases.

## Download

Install the latest APK from
[GitHub Releases](https://github.com/ejevgg/ArkMoney/releases). Android may ask
for permission to install applications from the browser or file manager used to
open the APK.

## Features

- fast expense and income entry with a built-in expression calculator;
- linked transfers between accounts;
- typed custom categories with emoji icons;
- multiple accounts and a live balance for the selected account;
- operation names, descriptions, editable time and private camera/gallery photos;
- history grouped by day, search and date navigation;
- week, month, quarter, year and custom-range analytics;
- Excel export with account and category selection;
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
- XML-based `.xlsx` export without a network service; importer code is retained
  for redesign but is not exposed in version 0.2.0.

## Project status

ArkMoney 0.2.0 is the current stable local-first release. Excel import is
temporarily unavailable while its interaction and validation flow is redesigned.
See [CHANGELOG.md](CHANGELOG.md)
for release notes.

- [Contributing](CONTRIBUTING.md)
- [Security policy](SECURITY.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Product behavior](docs/PRODUCT.md)

---

## Русский

ArkMoney — локальное Android-приложение для приватного учёта расходов, доходов
и переводов между счетами.

> Текущая стабильная версия: **0.2.0**.

### Возможности

- быстрый ввод операций через встроенный калькулятор;
- категории с эмодзи и несколько счетов с актуальными балансами;
- названия, описания, дата, время и фотографии операций;
- история по дням, поиск и переход к дате;
- аналитика расходов и доходов за стандартные и произвольные периоды;
- экспорт Excel с выбором счетов и категорий;
- светлая, тёмная и системная темы Material 3.

Импорт Excel временно недоступен до переработки интерфейса и проверки данных.

ArkMoney работает без аккаунта, аналитики и сетевого доступа. Данные остаются
во внутреннем хранилище приложения до явного экспорта. Требуется Android 10 или
новее. Последние APK доступны в
[GitHub Releases](https://github.com/ejevgg/ArkMoney/releases).

Для локальной проверки:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```
