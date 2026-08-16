# ArkMoney architecture

## Overview

ArkMoney is a single-module Android application written in Kotlin. Jetpack
Compose renders the interface, Room stores structured financial records and
private application files store attached expense photos.

## Application shell

`MainActivity.kt` owns the main navigation and coordinates Room flows for:

- expense entry and history;
- analytics;
- settings, accounts and categories;
- import, export and testing tools.

`ExpensesScreen.kt`, `AnalyticsScreen.kt` and `SettingsScreen.kt` contain the
three primary UI areas. `Theme.kt` applies Material 3 dynamic colors on Android
12 and newer with static fallback schemes.

## Persistence

`data/ArkMoneyDatabase.kt` defines the Room database `arkmoney.db` and its
migrations. The principal records are:

- `Expense`: integer amount in cents, account and category references, local
  timestamp, optional name and description, photo path and demo marker;
- `Account`: display name and opening balance in cents;
- `Category`: display name, emoji and ordering metadata.

`ExpenseDao.kt` exposes reactive lists and mutation operations. Schema changes
must use explicit migrations and preserve existing rows.

## Expense photos

`data/ExpensePhotoStorage.kt` copies user-selected images into private app
storage. Room stores only the resulting private path. Photos are intentionally
excluded from Excel workbooks.

## Calculator and financial logic

`ExpenseCalculator.kt` implements expression entry, precedence, live preview
and integer-cent conversion. `FinancialLogic.kt` calculates account-specific
balances. Currency must remain integer-based at persistence boundaries.

## Analytics and search

`AnalyticsLogic.kt` defines calendar periods, navigation, inclusive filtering
and daily totals. `ExpenseSearch.kt` normalizes queries across expense names,
descriptions, category labels and monetary amounts.

## Import, export and demo data

- `ExcelExporter.kt` writes the ArkMoney `.xlsx` package.
- `ExcelImporter.kt` reads that same format and reconstructs core records.
- `DemoDataGenerator.kt` produces deterministic, identifiable synthetic data.

Import/export compatibility is guarded by JVM round-trip tests. Import remains
an explicit user action and must validate input before database mutation.

## Verification boundaries

Local JVM tests cover calculator behavior, financial isolation, period logic,
search, formatting, Excel round trips and demo generation. Android lint checks
platform and Compose integration. Room migrations, document pickers, photo
handling, adaptive icon masks and full UI flows still require manual testing on
an isolated emulator or disposable test installation.
