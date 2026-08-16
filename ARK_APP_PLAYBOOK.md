# Ark App Playbook

This document records the shared baseline for applications in the Ark family.
Product-specific behavior and safety rules live in `docs/PRODUCT.md` and
`AGENTS.md`.

## Identity and philosophy

- Product names begin with `Ark` and each product has its own Android project,
  application ID and GitHub repository.
- Each application solves one clear task and works locally without a mandatory
  account.
- Privacy is the default: no analytics, network access or broad permissions
  without a documented product requirement.
- User data is preserved by default and destructive actions are explicit.

## Technical baseline

- Kotlin, Jetpack Compose, Material 3 and Gradle Kotlin DSL;
- Room/SQLite for structured local data;
- Android 10 / API 29 minimum;
- edge-to-edge layout with system insets;
- system, light and dark themes;
- private application storage for local files;
- no secrets, signing material, exports or personal data in Git.

## Design system

- Dynamic Material color is used where Android supports it.
- Common actions remain reachable with one hand and advanced controls appear
  contextually.
- Related content uses tonal surfaces with consistent spacing and corner radii.
- Destructive actions are visually separated and require confirmation when data
  cannot be recovered.
- Icons use a dark forest-green Ark family base, a calm architectural arch and
  one simple product-specific symbol inside the adaptive-icon safe zone.
- The icon must remain legible at 48 px and under circle, square, squircle and
  themed monochrome masks.

## Data and releases

- Structured schema changes use controlled migrations.
- Export or backup is required before a public stable release.
- Each distributed APK increments `versionCode`, receives a Git tag and is
  attached to a GitHub Release.
- Early releases may be marked pre-release; release notes must state automatic
  checks, manual checks and known limitations precisely.
- Release APKs use a private product-specific key that never enters Git.

## Verification baseline

Before handoff or release, run:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Use an isolated emulator for instrumentation and never test destructively on a
device containing important user data.

---

## Русский

Приложения семейства Ark имеют отдельные проекты, идентификаторы и репозитории,
решают одну понятную задачу и работают локально без обязательного аккаунта.
Основной стек: Kotlin, Jetpack Compose, Material 3, Room и Android 10+.

Пользовательские данные сохраняются по умолчанию, опасные действия требуют
подтверждения, а изменения схемы выполняются через миграции. Секреты, ключи,
экспорты и личные данные не добавляются в Git.

Иконки используют спокойную тёмно-зелёную основу, простой узнаваемый символ,
контролируемый свет и безопасную зону adaptive icon. Каждый выпущенный APK
увеличивает `versionCode`, подписывается постоянным приватным ключом, получает
Git-тег, контрольную сумму и GitHub Release.
