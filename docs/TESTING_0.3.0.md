# ArkMoney 0.3.0 testing / Тестирование ArkMoney 0.3.0

## Categories / Категории

- [ ] Drag expense and income categories independently in Settings. / Изменить порядок расходов и доходов независимо.
- [ ] Long-press and drag a calculator category in both directions. / Перетащить категорию удержанием в калькуляторе.
- [ ] Restart the app and verify order and default first category. / Перезапустить приложение и проверить порядок и первую категорию.
- [ ] Check every emoji group and create/edit/delete a category. / Проверить все группы эмодзи и создание, изменение, удаление категории.
- [ ] Verify the category keyboard starts with a capital letter. / Проверить заглавную букву клавиатуры.

## Backup / Резервная копия

- [ ] Create a `.arkmoney` file containing expenses, income, transfers, Unicode text and photos. / Создать копию со всеми типами операций, текстом и фото.
- [ ] Restore it after changing local data and compare all totals, order and photos. / Восстановить после изменений и сверить данные.
- [ ] Use “Undo last restore” and verify the previous state returns. / Проверить отмену последнего восстановления.
- [ ] Try a renamed random ZIP and a damaged backup; no data may change. / Проверить случайный ZIP и повреждённую копию без изменения данных.

## Photos / Фотографии

- [ ] Attach large portrait and landscape images from camera and gallery; verify orientation and file size. / Проверить ориентацию и размер больших фото.
- [ ] Open full screen, pinch, double-tap, pan and save to `Pictures/ArkMoney`. / Проверить масштабирование и сохранение в галерею.

## Entry and settings / Ввод и настройки

- [ ] Search opens the keyboard and closing search hides it without layout jumps. / Поиск открывает и закрывает клавиатуру без рывков.
- [ ] Enter fits the 4×4 grid, evaluates live expressions and supports the long-press date menu. / Проверить Enter и выбор даты.
- [ ] Toggle haptics and verify calculator and category dragging. / Проверить включение и отключение виброотдачи.
- [ ] Enable a daily limit, exceed it, restart, then disable it. / Проверить дневной лимит, превышение, сохранение и отключение.
