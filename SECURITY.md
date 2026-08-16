# Security policy

## Supported versions

Security fixes are currently applied to the latest published ArkMoney release.

## Reporting a vulnerability

Do not publish sensitive vulnerability details or financial data in a public
issue. Open a private security advisory in the GitHub repository and include:

- affected version;
- reproduction steps using synthetic data;
- expected and observed behavior;
- potential impact;
- any suggested mitigation.

## Data model

ArkMoney stores accounts, categories and expenses in a local Room database.
Receipt photos are copied into private application storage. Network sync and
analytics are not included. Excel files are created or read only through
explicit user actions and should be treated as sensitive financial documents.

---

## Русский

Исправления безопасности применяются к последней опубликованной версии. Не
публикуйте сведения об уязвимости или финансовые данные в открытой задаче —
создайте приватное security advisory с версией, шагами воспроизведения на
синтетических данных, влиянием и возможным исправлением.

ArkMoney хранит операции в локальной Room-базе, а фотографии — во внутреннем
хранилище приложения. Синхронизация и аналитика отсутствуют. Excel-файлы
создаются и читаются только по явному действию и должны считаться
конфиденциальными финансовыми документами.
