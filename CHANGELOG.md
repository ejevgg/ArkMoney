# Changelog

All notable changes to ArkMoney are documented in this file.

## 0.4.0 — 2026-08-21

### Added / Добавлено

- selectable Russian, English and device-language interfaces / выбор русского,
  английского языка или языка устройства;
- swipe-up calculator opening, scroll-to-hide behavior and Android Back handling /
  открытие калькулятора свайпом, скрытие при прокрутке и обработка кнопки «Назад»;
- Snackbar undo after adding an operation / быстрая отмена новой операции;
- analytics summary for income, expenses, net result, comparison and projection /
  сводка аналитики по доходам, расходам, итогу, сравнению и прогнозу;
- optional category emoji visibility in the calculator / настройка отображения
  эмодзи категорий в калькуляторе.

### Changed / Изменено

- transaction type selection is now a compact Material 3 menu / выбор типа
  операции перенесён в компактное меню Material 3;
- operation name action shares the amount row and calculator keys are larger /
  кнопка названия расположена рядом с суммой, клавиши калькулятора увеличены;
- the account chooser is a balance-aware Material 3 bottom sheet / выбор счёта
  переделан в Material 3 панель с балансами;
- category deletion can explicitly remove linked operations and photos / категория
  может быть удалена вместе со связанными операциями и фотографиями;
- `.arkmoney` restore rejects oversized, unknown and unreferenced archive entries /
  восстановление `.arkmoney` отклоняет лишние и некорректные элементы архива.

## 0.3.0 — 2026-08-18

### Added / Добавлено

- drag reordering for expense and income categories in Settings and by holding a
  calculator category / сортировка категорий перетаскиванием в настройках и
  удержанием категории в калькуляторе;
- a large grouped emoji library / большая библиотека эмодзи по тематическим группам;
- versioned `.arkmoney` backups containing accounts, categories, operations and
  photos, with validated restore and an automatic safety copy / резервные копии
  `.arkmoney` со счетами, категориями, операциями и фотографиями, проверкой и
  страховочной копией;
- optional daily spending limit display and configurable haptic feedback /
  необязательное отображение дневного лимита и настройка виброотдачи;
- full-screen zoomable photo viewing and saving to the Android gallery /
  полноэкранный просмотр фотографий с масштабированием и сохранением в галерею.

### Changed / Изменено

- newly attached photos are resized, orientation-corrected, stripped of metadata
  and compressed / новые фотографии уменьшаются, корректно поворачиваются,
  очищаются от метаданных и сжимаются;
- the category editor uses sentence capitalization and a Material 3 card list /
  редактор категорий использует заглавную букву и карточки Material 3;
- search focuses immediately and opens the system keyboard / поиск сразу получает
  фокус и открывает системную клавиатуру;
- the calculator no longer has Equals: Enter occupies its grid cell and keeps the
  long-press date action / кнопка «Равно» удалена, Enter находится внутри сетки и
  сохраняет выбор даты по долгому нажатию;
- category deletion now transfers linked operations atomically and offers a
  scrollable destination list / удаление категории атомарно переносит связанные
  операции и показывает прокручиваемый список категорий назначения;
- calculator layout, collapse animation, active tabs, category drag-and-drop and
  photo controls were polished for Material 3 / улучшены раскладка и анимация
  калькулятора, активные вкладки, перетаскивание категорий и элементы просмотра фото;
- fresh installations receive a frequency-oriented default category set / новые
  установки получают обновлённый набор категорий по частоте использования.

## 0.2.0 — 2026-08-16

### Added / Добавлено

- income and linked transfers between accounts / доходы и связанные переводы;
- transaction deletion, editable date and time, camera photos / удаление
  операций, редактирование даты и времени, фотографии с камеры;
- custom analytics ranges and income charts / произвольные периоды и графики
  доходов;
- controlled category reassignment / выбор категории назначения при удалении;
- Excel 0.2 with types, precise time, transfers and category metadata / новый
  формат Excel с типами, временем, переводами и категориями;
- backward-compatible database and Excel migration / совместимость с данными и
  файлами 0.1.0;
- refreshed adaptive and monochrome icon / новая адаптивная и монохромная иконка.
- editable operation amounts and a rebuilt Material 3 operation dialog / изменение
  суммы и полностью обновлённое окно операции в стиле Material 3;
- scoped Excel export by accounts and optional categories / выбор счетов и
  категорий для экспорта;
- realistic demo income and corrected seven-day chart alignment / доходы в
  демо-данных и исправленное выравнивание недельного графика.

### Changed / Изменено

- Excel import is temporarily hidden while its workflow is redesigned / импорт
  Excel временно скрыт до переработки сценария;
- the first navigation destination is now “Finance” / раздел «Расходы» переименован
  в «Финансы»;
- calculator collapse waits for the system keyboard to close / калькулятор
  сворачивается после закрытия системной клавиатуры.

## 0.1.0 — 2026-08-16

### Added

- fast expense entry through an in-app expression calculator;
- editable emoji categories and multiple accounts with per-account balances;
- expense history grouped by day, compact search and date navigation;
- custom expense names, descriptions and private photo attachments;
- week, month, quarter and year analytics with category and daily charts;
- Excel export and import of ArkMoney workbooks;
- system, light and dark themes with Material 3 dynamic color;
- adaptive full-color and monochrome application icons;
- automated unit tests, Android lint and debug APK workflow.

## Known limitations

- synchronization between devices is not available;
- Excel import is temporarily unavailable in the application interface;
- attached photos are private local files and are not embedded in Excel exports;
- automatic backups and encrypted archives are not yet available;
- analytics and database migrations require further device-level testing before
  a stable release.
