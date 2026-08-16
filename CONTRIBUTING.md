# Contributing to ArkMoney

ArkMoney is in early development. Small, focused changes are easiest to review
and verify.

## Before opening a change

1. Check existing issues and pull requests.
2. Describe the user-facing problem before proposing an implementation.
3. Keep unrelated formatting and dependency updates out of the same change.
4. Never use real financial exports, databases or receipt photos as fixtures.

## Local checks

Run the following before opening a pull request:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Do not run connected or instrumented tests on a phone containing personal
ArkMoney data. Use an isolated emulator or test-only installation.

## Pull requests

- explain behavior before and after the change;
- include reproduction steps for bug fixes;
- attach redacted screenshots for visual changes;
- add or update tests when financial, import or persistence logic changes;
- do not commit APK files, signing keys, device backups, Excel exports, local
  databases, receipt photos or personal financial data.

---

## Русский

Перед изменением проверьте существующие задачи, опишите пользовательскую
проблему и не смешивайте несвязанные правки. Никогда не используйте реальные
финансовые данные, фотографии чеков или экспорты как тестовые файлы.

Перед pull request выполните:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

В pull request опишите поведение до и после изменения, добавьте обезличенные
скриншоты интерфейса и тесты для финансовой логики, импорта и хранения. Не
добавляйте APK, ключи, базы, резервные копии и персональные данные.
