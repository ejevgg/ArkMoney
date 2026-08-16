# ArkMoney 0.2.0 release testing / Тестирование ArkMoney 0.2.0

This checklist covers the manual checks required for 0.2.0. Test the signed 0.2.0 build over the public 0.1.0 build; do not uninstall the application first. Export a backup before starting.

Этот чек-лист содержит ручные проверки версии 0.2.0. Устанавливайте подписанную версию 0.2.0 поверх публичной версии 0.1.0, не удаляя приложение. Перед началом сделайте экспорт данных.

## Upgrade and data safety / Обновление и сохранность данных

- [ ] Install 0.2.0 over 0.1.0 without clearing application data. / Установить 0.2.0 поверх 0.1.0 без очистки данных.
- [ ] Existing accounts, categories, operations, balances, names, dates, and photos remain intact. / Проверить сохранность счетов, категорий, операций, балансов, названий, дат и фотографий.
- [ ] Open every main page once and restart the application. / Открыть все основные страницы и перезапустить приложение.

## Operations and balances / Операции и балансы

- [ ] Add an expense and income; verify the selected account balance changes in opposite directions. / Добавить расход и доход; проверить противоположное изменение баланса.
- [ ] Transfer between two accounts; verify the source decreases, destination increases, and total funds stay unchanged. / Выполнить перевод; проверить оба счёта и неизменность общей суммы.
- [ ] The transfer menu shows account names and current balances. / Меню перевода показывает названия и текущие балансы счетов.
- [ ] Add operations for today, yesterday, and the day before yesterday by holding Enter. / Добавить операции за сегодня, вчера и позавчера удержанием Enter.
- [ ] Edit an operation date and time; verify history order and displayed time. / Изменить дату и время операции; проверить порядок и отображение.
- [ ] Delete an operation with and without a photo. / Удалить операции с фотографией и без неё.
- [ ] Delete an account after confirmation; verify all its operations and linked transfers are removed. / Удалить счёт после подтверждения; проверить удаление его операций и связанных переводов.
- [ ] Delete a used category and select a replacement; verify its operations move to that category. / Удалить используемую категорию с выбором замены; проверить перенос операций.

## Photos / Фотографии

- [ ] Add, replace, open, and remove a gallery photo. / Добавить, заменить, открыть и удалить фото из галереи.
- [ ] Take and save a camera photo; cancel camera capture once and verify no broken attachment appears. / Сделать фото камерой; один раз отменить съёмку и проверить отсутствие битого вложения.
- [ ] Verify photo action buttons remain readable in light and dark themes. / Проверить кнопки фото в светлой и тёмной темах.

## Export / Экспорт

- [ ] Select one account and a subset of categories, export data, and open the workbook: Transactions, Accounts, and Categories sheets are present. / Выбрать один счёт и часть категорий, экспортировать данные и проверить листы Transactions, Accounts и Categories.
- [ ] Verify operation type, source/destination accounts, amount, name, note, and millisecond-precision time. / Проверить тип, счета, сумму, название, заметку и точное время.
- [ ] Verify excluded accounts and categories are absent and that transfers remain internally consistent. / Проверить отсутствие исключённых счетов и категорий и целостность переводов.

## Analytics and interface / Аналитика и интерфейс

- [ ] Check current month, last 7 days, and a custom date range containing expenses and income. / Проверить текущий месяц, 7 дней и произвольный период с расходами и доходами.
- [ ] Verify charts, legends, empty states, totals, and category colors in both themes. / Проверить графики, легенды, пустые состояния, суммы и цвета в обеих темах.
- [ ] Check smooth calculator show/hide and title input with the system keyboard. / Проверить плавное скрытие калькулятора и ввод названия системной клавиатурой.
- [ ] Create and edit categories using only the emoji picker. / Создать и изменить категории через выбор эмодзи.
- [ ] Check compact search, date navigation, account selector, GitHub button, centered version, and Testing-page back button. / Проверить поиск, переход к дате, выбор счёта, GitHub, версию и кнопку назад страницы тестирования.
- [ ] Check the adaptive icon with circle, squircle, and themed Android 12+ icon modes. / Проверить адаптивную и тематическую иконку с разными масками Android 12+.

Record the device model, Android version, result, and reproduction steps for every failure. Manual checks are not complete until every box is confirmed or the exception is documented.

Для каждой ошибки запишите модель устройства, версию Android, результат и шаги воспроизведения. Ручное тестирование завершено только после подтверждения всех пунктов либо документирования исключений.
