# ArkMoney 0.4.1 testing / Тестирование ArkMoney 0.4.1

Use an isolated installation or update the signed release without clearing app
data. / Используйте отдельную установку или обновляйте подписанный релиз без
очистки данных приложения.

- [ ] Drag the calculator handle down slowly: the panel follows the finger and
  settles smoothly. / Медленно потяните ручку вниз: панель следует за пальцем и
  плавно завершает движение.
- [ ] Drag upward from the collapsed control and also open it by tapping. /
  Откройте калькулятор свайпом вверх и обычным нажатием.
- [ ] Add the first operation of a new day and verify it is visible immediately. /
  Добавьте первую операцию нового дня и проверьте её появление на экране.
- [ ] Verify the notification is above the calculator and disappears after three
  seconds. / Проверьте положение уведомления и исчезновение через три секунды.
- [ ] Verify the notification order: message, Undo, close icon. Elements must not
  overlap in light, dark or dynamic theme. / Проверьте порядок: сообщение,
  «Отменить», крестик — без наложений во всех темах.
- [ ] Tap Undo and confirm only the newly added operation is removed. / Нажмите
  «Отменить» и убедитесь, что удалена только новая операция.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```
