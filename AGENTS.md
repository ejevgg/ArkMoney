# ArkMoney agent guide

This file defines mandatory rules for coding agents in this repository. Read
`README.md`, `docs/PRODUCT.md`, and `docs/ARCHITECTURE.md` before changing
user-facing behavior or financial data handling.

## Project identity

- Product name: ArkMoney.
- Android application ID and namespace: `com.arkulz.arkmoney`.
- Minimum Android version: Android 10 / API 29.
- UI: Jetpack Compose with Material 3 and dynamic color where available.
- Data model: local-first Room database plus private expense photo files.
- The application does not require an account, analytics or network access.

Do not change the application ID, database name, signing identity, minimum SDK,
Excel format or storage model without explicit approval and a migration plan.

## Data safety

- Expenses, accounts and attachments are important user data; preserve them.
- Never clear app data, uninstall the installed app, delete its database or run
  destructive ADB commands without explicit approval.
- Never run connected tests on a device containing personal financial data.
- Do not commit or print signing keys, exports, databases, backups, receipt
  photos, passwords or personal content.
- Destructive actions must explain their scope and require confirmation.
- Database schema changes require explicit Room migrations. Destructive fallback
  is not acceptable.
- `.arkmoney` restore must validate the complete archive before replacing data
  and must keep the pre-restore safety copy recoverable from the UI.

## Financial and transfer invariants

- Money is stored as integer cents. Do not persist floating-point currency.
- Account balances must include only expenses belonging to that account.
- Date period boundaries are inclusive and use the user's local calendar.
- Excel import is not user-facing in 0.2.0. Retained importer code must validate
  ArkMoney's workbook structure before any future database write.
- Importer and export changes require format tests, including special text.
- Deleting an expense must also remove its private photo when appropriate.

## Verification

Run before handing off a code change:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Report the exact checks performed. Real-device installation is optional and
must never be combined with instrumentation on a personal-data installation.

## Git and releases

- Preserve unrelated and untracked user files.
- Never commit `signing/`, APKs, exports, databases, photos, backups or local
  configuration.
- Update `CHANGELOG.md` for user-visible changes.
- Increase `versionCode` for every distributed APK.
- Release APKs must use the existing private release key; verify their signature
  and SHA-256 checksum before publication.
- Do not publish, tag, rewrite history or create a release unless explicitly
  requested.

## Style

- User-facing strings are currently Russian.
- Prefer clear Material 3 patterns and accessible content descriptions.
- Optimize common expense entry for speed and one-handed use.
- Keep changes focused and avoid speculative dependencies or architecture churn.

---

## Русский

- Не изменяйте `com.arkulz.arkmoney`, `arkmoney.db`, минимальную версию Android,
  формат Excel или ключ подписи без явного согласования и плана миграции.
- Сохраняйте пользовательские операции, счета, категории и фотографии. Не
  очищайте данные приложения и не запускайте инструментальные тесты на личной
  установке.
- Деньги хранятся только в целых копейках. Перевод не является доходом или
  расходом и должен атомарно учитывать оба счёта.
- Изменения Room требуют явной миграции; изменения экспорта и сохранённого кода
  импортера — тестов формата и проверки ошибочных файлов. В версии 0.2.0 импорт
  не показывается пользователю.
- Перед передачей изменений выполните `./gradlew testDebugUnitTest lintDebug
  assembleDebug` и точно сообщите результат.
- Не добавляйте в Git ключи, базы, экспорты, фотографии, резервные копии и
  персональные данные.
