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
- Enter records the operation today; a long press can record it today, yesterday
  or the day before yesterday. Live preview makes a separate Equals key unnecessary.
- A separate name action reveals a focused text field and the system keyboard.
- Expenses are grouped by local calendar day.
- A custom expense name replaces the category name in the list.
- Tapping an expense opens its card with name, description and photo controls.

## Search and navigation

- Search opens from the compact header action.
- The search field immediately receives focus and opens the system keyboard.
- Search matches custom names, descriptions, categories and formatted amounts.
- Date navigation is visible only while search mode is open.

## Accounts and categories

- Every expense belongs to one account and one category.
- The displayed balance is the selected account's opening balance minus its
  expenses.
- Accounts can be created, edited and deleted through Settings.
- Categories can be created, renamed, assigned an emoji and deleted through
  Settings.
- Expense and income categories have independent drag ordering. A long drag on a
  calculator category changes that same order.
- Category icons come from a large built-in, grouped emoji library.
- Destructive changes require confirmation and must not silently orphan data.

## Analytics

- The current calendar month is selected by default.
- Week, month, quarter and year periods can be navigated backward and forward.
- Analytics show category totals and daily spending, including zero-value days
  where needed for a continuous chart.

## Settings and data transfer

- Theme choices apply immediately and persist: device, light or dark.
- The application version is centered below the GitHub repository action.
- Excel export contains the selected ArkMoney accounts, categories and operations.
- Excel import is temporarily unavailable while its interaction and validation flow is redesigned.
- A `.arkmoney` backup contains all structured data and attached photos. Restore
  validates references before replacement and keeps an internal safety copy.
- Haptic feedback and the informational daily spending limit are optional.

## Privacy and safety

ArkMoney contains no account system, analytics SDK or network service. The Room
database and expense photos stay in private storage. Excel transfer is always an
explicit user action.

---

## Русский

ArkMoney работает локально без аккаунта и сети. Поддерживаются расходы, доходы
и переводы между счетами. Перевод является одной связанной операцией и не входит
в сумму доходов или расходов.

Запись содержит сумму, счёт, категорию, дату, время, необязательные название,
описание и фотографию. Записи удаляются только после подтверждения. Удаление
счёта удаляет связанные операции и фотографии после явного предупреждения. При
удалении категории пользователь выбирает категорию назначения того же типа.

Текущий месяц выбран в аналитике по умолчанию. Доступны неделя, месяц, квартал,
год и произвольный диапазон. Доходы и расходы отображаются раздельно.

Excel сохраняет тип операции, точное время, выбранные счета и категории. Импорт
временно недоступен до переработки его сценария и проверки. Передача файлов всегда
выполняется по явному действию пользователя.

Enter сохраняет операцию, а долгое нажатие позволяет выбрать день. Категории
расходов и доходов сортируются независимо перетаскиванием, в том числе удержанием
на главном экране. Иконки выбираются из большой библиотеки эмодзи.

Файл `.arkmoney` содержит счета, категории, операции и фотографии. Перед
восстановлением проверяются связи, а текущие данные сохраняются во внутреннюю
страховочную копию. Дневной лимит носит информационный характер и по умолчанию
выключен; виброотдачу также можно отключить.
