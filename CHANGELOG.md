# Changelog

All notable changes to ArkMoney are documented in this file.

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
