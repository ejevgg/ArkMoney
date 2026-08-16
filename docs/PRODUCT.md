# ArkMoney product behavior

## Product principles

- Local-first and usable without an account or connection.
- Recording a normal expense should require minimal taps.
- Financial state must remain predictable and recoverable.
- Material 3 visual language with system, light and dark themes.
- Advanced controls appear contextually instead of crowding the main screen.

## Expense entry and history

- The main screen opens with ArkMoney's own calculator visible.
- The selected account balance is centered in the header.
- A category is always selected; the first available category is the fallback.
- The calculator keeps the entered expression visible and shows a live result.
- A normal press on `+` records the expense today.
- A long press on `+` can record it today, yesterday or the day before yesterday.
- A separate name action reveals a focused text field and the system keyboard.
- Expenses are grouped by local calendar day.
- A custom expense name replaces the category name in the list.
- Tapping an expense opens its card with name, description and photo controls.

## Search and navigation

- Search opens from the compact header action.
- Search matches custom names, descriptions, categories and formatted amounts.
- Date navigation is visible only while search mode is open.

## Accounts and categories

- Every expense belongs to one account and one category.
- The displayed balance is the selected account's opening balance minus its
  expenses.
- Accounts can be created, edited and deleted through Settings.
- Categories can be created, renamed, assigned an emoji and deleted through
  Settings.
- Destructive changes require confirmation and must not silently orphan data.

## Analytics

- The current calendar month is selected by default.
- Week, month, quarter and year periods can be navigated backward and forward.
- Analytics show category totals and daily spending, including zero-value days
  where needed for a continuous chart.

## Settings, testing and data transfer

- Theme choices apply immediately and persist: device, light or dark.
- The application version is shown as `0.1.0`.
- Ten taps on the version name reveal the Testing page.
- Demo generation creates identifiable synthetic expenses across the latest
  year; some include custom names and none include photos.
- Removing demo data must preserve all non-demo records.
- Excel export contains ArkMoney accounts, categories and expenses.
- Import accepts workbooks produced by ArkMoney and preserves core fields.

## Privacy and safety

ArkMoney contains no account system, analytics SDK or network service. The Room
database and expense photos stay in private storage. Excel transfer is always an
explicit user action.
