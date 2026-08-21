# ArkMoney 0.4.0 testing / Тестирование ArkMoney 0.4.0

Use an isolated emulator or disposable test installation. Do not use a phone
containing the only copy of real financial data. / Используйте изолированный
эмулятор или тестовую установку без единственной копии реальных данных.

## Finance / Финансы

- [ ] Android Back hides the built-in calculator before leaving the app. / Кнопка
  «Назад» сначала скрывает встроенный калькулятор.
- [ ] Scrolling operations hides the calculator; tap and swipe up restore it. /
  Прокрутка скрывает калькулятор, нажатие и свайп вверх открывают его.
- [ ] Expense, income and transfer modes save correct balances. / Расход, доход и
  перевод корректно изменяют балансы.
- [ ] A new operation scrolls into view and Snackbar Undo removes only that row. /
  Новая операция видна, а отмена удаляет только её.
- [ ] Account bottom sheet shows every account and its current balance. / Панель
  счетов показывает все счета и актуальные балансы.

## Settings and language / Настройки и язык

- [ ] Device, Russian and English modes update all main screens and dialogs. /
  Системный, русский и английский режимы обновляют экраны и диалоги.
- [ ] Hiding category emoji affects only calculator chips. / Отключение эмодзи
  влияет только на категории калькулятора.
- [ ] Category deletion works both by reassignment and by deleting linked records. /
  Категория удаляется как с переносом, так и вместе с операциями.
- [ ] Android Back returns from Testing to Settings. / Кнопка «Назад» возвращает
  со страницы тестирования в настройки.

## Data / Данные

- [ ] Export selected accounts and categories; verify amounts, times and transfers
  in Excel. / Экспортируйте выбранные счета и категории и проверьте Excel.
- [ ] Create and restore an `.arkmoney` backup including a photo. / Создайте и
  восстановите резервную копию `.arkmoney` с фотографией.
- [ ] Confirm the internal pre-restore backup can undo the restore. / Проверьте
  отмену восстановления через страховочную копию.

## Automated checks / Автоматические проверки

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```
