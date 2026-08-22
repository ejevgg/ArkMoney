package com.arkulz.arkmoney

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arkulz.arkmoney.data.Account
import com.arkulz.arkmoney.data.ArkMoneyBackup
import com.arkulz.arkmoney.data.BackupArchive
import com.arkulz.arkmoney.data.Category
import com.arkulz.arkmoney.data.ExcelExporter
import com.arkulz.arkmoney.data.Expense
import com.arkulz.arkmoney.data.TransactionType
import com.arkulz.arkmoney.data.transactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.io.File

@Composable
fun SettingsScreen(
    modifier: Modifier,
    expenses: List<Expense>,
    categories: List<Category>,
    accounts: List<Account>,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    appLanguage: AppLanguage,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onAddCategory: (String, String, TransactionType) -> Unit,
    onUpdateCategory: (Category) -> Unit,
    onDeleteCategory: (Category, Category, (Boolean) -> Unit) -> Unit,
    onDeleteCategoryWithExpenses: (Category, (Boolean) -> Unit) -> Unit,
    onAddAccount: (String, Long) -> Unit,
    onUpdateAccount: (Account) -> Unit,
    onDeleteAccount: (Account) -> Unit,
    onGenerateDemoData: () -> Unit,
    onDeleteDemoData: () -> Unit,
    hapticsEnabled: Boolean,
    onHapticsEnabledChange: (Boolean) -> Unit,
    showCategoryEmoji: Boolean,
    onShowCategoryEmojiChange: (Boolean) -> Unit,
    dailyLimitEnabled: Boolean,
    dailyLimitCents: Long,
    onDailyLimitChange: (Boolean, Long) -> Unit,
    onCategoryOrderChanged: (List<Category>) -> Unit,
    onRestoreBackup: (BackupArchive) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var categoryEditor by remember { mutableStateOf<Category?>(null) }
    var addingCategory by remember { mutableStateOf(false) }
    var accountEditor by remember { mutableStateOf<Account?>(null) }
    var addingAccount by remember { mutableStateOf(false) }
    var deleteCategory by remember { mutableStateOf<Category?>(null) }
    var deleteAccount by remember { mutableStateOf<Account?>(null) }
    var confirmDemoFill by remember { mutableStateOf(false) }
    var confirmDemoDelete by remember { mutableStateOf(false) }
    var versionTaps by remember { mutableIntStateOf(0) }
    var showTesting by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var exportAccountIds by remember(accounts) { mutableStateOf(accounts.map { it.id }.toSet()) }
    var exportCategoryIds by remember(categories) { mutableStateOf(categories.map { it.id }.toSet()) }
    var pendingBackup by remember { mutableStateOf<BackupArchive?>(null) }
    var dailyLimitText by remember { mutableStateOf(formatLimitInput(dailyLimitCents)) }
    var dailyLimitFocused by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(dailyLimitCents, dailyLimitFocused) { if (!dailyLimitFocused) dailyLimitText = formatLimitInput(dailyLimitCents) }
    var displayedCategories by remember { mutableStateOf(categories) }
    LaunchedEffect(categories) { displayedCategories = categories }
    val uriHandler = LocalUriHandler.current
    val fileAccessError = tr("Нет доступа к файлу", "File access denied")
    val excelSavedText = tr("Excel-файл сохранён", "Excel file saved")
    val exportErrorText = tr("Не удалось экспортировать данные", "Could not export data")
    val backupSavedText = tr("Резервная копия сохранена", "Backup saved")
    val backupCreateErrorText = tr("Не удалось создать резервную копию", "Could not create backup")
    val backupOpenErrorText = tr("Не удалось открыть копию", "Could not open backup")
    val safetyBackupErrorText = tr("Страховочная копия повреждена", "Safety backup is damaged")
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        if (uri != null) scope.launch {
            val succeeded = withContext(Dispatchers.IO) {
                val selectedExpenses = expenses.filter {
                    it.accountId in exportAccountIds && when (it.transactionType) {
                        TransactionType.TRANSFER -> it.transferAccountId in exportAccountIds
                        else -> it.categoryId in exportCategoryIds
                    }
                }
                val selectedCategories = categories.filter { it.id in exportCategoryIds }
                val selectedAccounts = accounts.filter { it.id in exportAccountIds }
                runCatching { context.contentResolver.openOutputStream(uri)?.use { ExcelExporter.write(it, selectedExpenses, selectedCategories, selectedAccounts) } ?: error(fileAccessError) }.isSuccess
            }
            Toast.makeText(context, if (succeeded) excelSavedText else exportErrorText, Toast.LENGTH_SHORT).show()
        }
    }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) scope.launch { val ok = withContext(Dispatchers.IO) { runCatching { context.contentResolver.openOutputStream(uri)?.use { ArkMoneyBackup.write(it, accounts, categories, expenses) } ?: error(fileAccessError) }.isSuccess }; Toast.makeText(context, if (ok) backupSavedText else backupCreateErrorText, Toast.LENGTH_SHORT).show() }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { withContext(Dispatchers.IO) { runCatching { context.contentResolver.openInputStream(uri)?.use(ArkMoneyBackup::read) ?: error(fileAccessError) } }.onSuccess { pendingBackup = it }.onFailure { Toast.makeText(context, "$backupOpenErrorText: ${it.message}", Toast.LENGTH_LONG).show() } }
    }
    BackHandler(showTesting) { showTesting = false }

    if (showTesting) {
        LazyColumn(modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton({ showTesting = false }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Назад", "Back")) }; Text(tr("Тестирование", "Testing"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) } }
            item { SettingsSection(tr("Демо-данные", "Demo data")) {
                Text(tr("Создаёт отдельный демо-счёт с реалистичными расходами и доходами за последние 365 дней.", "Creates a separate demo account with realistic expenses and income for the last 365 days."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button({ confirmDemoFill = true }, Modifier.padding(top = 12.dp)) { Text(tr("Заполнить демо-данными за год", "Generate one year of demo data")) }
                if (expenses.any { it.isDemo } || accounts.any { it.isDemo }) TextButton({ confirmDemoDelete = true }) { Text(tr("Удалить демо-данные", "Delete demo data"), color = MaterialTheme.colorScheme.error) }
            } }
        }
        if (confirmDemoFill) ConfirmDelete(tr("Заполнить данные за год?", "Generate a year of data?"), tr("Существующие демо-данные будут заменены. Личные расходы не изменятся.", "Existing demo data will be replaced. Personal operations will not change."), { confirmDemoFill = false }, confirmLabel = tr("Создать", "Create"), destructive = false) { onGenerateDemoData(); confirmDemoFill = false }
        if (confirmDemoDelete) ConfirmDelete(tr("Удалить демо-данные?", "Delete demo data?"), tr("Будут удалены только созданные для тестирования расходы и демо-счёт.", "Only generated demo operations and the demo account will be deleted."), { confirmDemoDelete = false }) { onDeleteDemoData(); confirmDemoDelete = false }
        return
    }

    LazyColumn(modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Text(tr("Настройки", "Settings"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item {
            SettingsSection(tr("Оформление", "Appearance")) {
                ThemeMode.entries.forEach { mode ->
                    val label = when (mode) { ThemeMode.SYSTEM -> tr("Как на устройстве", "Use device theme"); ThemeMode.LIGHT -> tr("Светлая", "Light"); ThemeMode.DARK -> tr("Тёмная", "Dark") }
                    Row(Modifier.fillMaxWidth().clickable { onThemeModeChange(mode) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(themeMode == mode, { onThemeModeChange(mode) })
                        Text(label, Modifier.padding(start = 8.dp))
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(tr("Виброотдача", "Haptic feedback"), fontWeight = FontWeight.Medium); Text(tr("Клавиатура и сортировка категорий", "Calculator and category ordering"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Switch(hapticsEnabled, onHapticsEnabledChange) }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(tr("Эмодзи в калькуляторе", "Emoji in calculator"), fontWeight = FontWeight.Medium); Text(tr("Скрывает иконки только на панели быстрого ввода", "Hide icons only on the quick-entry panel"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Switch(showCategoryEmoji, onShowCategoryEmojiChange) }
            }
        }
        item {
            SettingsSection(tr("Язык", "Language")) {
                AppLanguage.entries.forEach { language ->
                    val label = when (language) {
                        AppLanguage.SYSTEM -> tr("Как на устройстве", "Use device language")
                        AppLanguage.RUSSIAN -> "Русский"
                        AppLanguage.ENGLISH -> "English"
                    }
                    Row(Modifier.fillMaxWidth().clickable { onAppLanguageChange(language) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(appLanguage == language, { onAppLanguageChange(language) })
                        Text(label, Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
        item {
            SettingsSection(tr("Дневной лимит", "Daily limit")) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(tr("Показывать дневной лимит", "Show daily limit"), fontWeight = FontWeight.Medium); Text(tr("Лимит не запрещает добавлять расходы", "The limit does not block expenses"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Switch(dailyLimitEnabled, { enabled -> onDailyLimitChange(enabled, parseMoneyCents(dailyLimitText) ?: dailyLimitCents) }) }
                if (dailyLimitEnabled) OutlinedTextField(
                    value = dailyLimitText,
                    onValueChange = { value ->
                        val separator = value.indexOfFirst { it == ',' || it == '.' }
                        dailyLimitText = value.filterIndexed { index, char -> char.isDigit() || ((char == ',' || char == '.') && index == separator) }.replace('.', ',').let { filtered ->
                            val parts = filtered.split(',', limit = 2)
                            if (parts.size == 2) "${parts[0].take(9)},${parts[1].take(2)}" else filtered.take(9)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).onFocusChanged { state ->
                        dailyLimitFocused = state.isFocused
                        if (!state.isFocused) parseMoneyCents(dailyLimitText)?.let { onDailyLimitChange(true, it) }
                    },
                    label = { Text(tr("Лимит на день", "Daily limit")) },
                    placeholder = { Text(tr("Например, 1 500", "For example, 1,500")) },
                    suffix = { Text("₽") },
                    supportingText = { Text(tr("Покажем прогресс рядом с расходами за день", "Progress is shown next to the daily total")) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { parseMoneyCents(dailyLimitText)?.let { onDailyLimitChange(true, it) }; keyboardController?.hide() }),
                )
            }
        }
        item {
            SettingsSection(tr("Счета", "Accounts")) {
                accounts.forEach { account ->
                    ManageRow(account.name, formatMoney(account.currentBalance(expenses)), { accountEditor = account }, if (accounts.size > 1) {{ deleteAccount = account }} else null)
                }
                TextButton({ addingAccount = true }) { Text(tr("+ Добавить счёт", "+ Add account")) }
            }
        }
        item {
            SettingsSection(tr("Категории", "Categories")) {
                Text(tr("Удерживайте карточку и перетаскивайте, чтобы изменить порядок.", "Hold and drag a card to change its order."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 10.dp))
                TransactionType.entries.filter { it != TransactionType.TRANSFER }.forEach { type ->
                    Text(if (type == TransactionType.EXPENSE) tr("Расходы", "Expenses") else tr("Доходы", "Income"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    ReorderableCategoryList(
                        categories = displayedCategories.filter { it.type == type.name },
                        hapticsEnabled = hapticsEnabled,
                        onEdit = { categoryEditor = it },
                        onOrderChanged = { ordered ->
                            displayedCategories = displayedCategories.filter { it.type != type.name } + ordered
                            onCategoryOrderChanged(ordered.mapIndexed { index, item -> item.copy(sortOrder = index) })
                        },
                    )
                }
                TextButton({ addingCategory = true }) { Text(tr("+ Добавить категорию", "+ Add category")) }
            }
        }
        item {
            SettingsSection(tr("Данные", "Data")) {
                Text(tr("Все данные хранятся только на устройстве.", "All data is stored only on this device."), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Button({ showExport = true }, modifier = Modifier.padding(top = 12.dp)) { Text(tr("Экспортировать в Excel", "Export to Excel")) }
                TextButton({ backupLauncher.launch("ArkMoney-${LocalDate.now()}.arkmoney") }) { Text(tr("Создать резервную копию", "Create backup")) }
                TextButton({ restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }) { Text(tr("Восстановить из копии", "Restore backup")) }
                if (File(context.filesDir, "last_before_restore.arkmoney").isFile) TextButton({ scope.launch { withContext(Dispatchers.IO) { runCatching { File(context.filesDir, "last_before_restore.arkmoney").inputStream().use(ArkMoneyBackup::read) } }.onSuccess { pendingBackup = it }.onFailure { Toast.makeText(context, safetyBackupErrorText, Toast.LENGTH_LONG).show() } } }) { Text(tr("Отменить последнее восстановление", "Undo last restore")) }
            }
        }
        item { Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { TextButton({ uriHandler.openUri("https://github.com/ejevgg/ArkMoney") }) { Text("GitHub") }; Text("ArkMoney 0.4.1", Modifier.clickable { val next = nextVersionTap(versionTaps); versionTaps = next.first; showTesting = next.second }.padding(vertical = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) } }
    }

    val editingCategory = categoryEditor
    if (addingCategory || editingCategory != null) CategoryDialog(
        category = editingCategory,
        onDismiss = { addingCategory = false; categoryEditor = null },
        onDelete = editingCategory?.takeIf { selected -> categories.count { it.type == selected.type } > 1 }?.let { selected ->
            {
                categoryEditor = null
                deleteCategory = selected
            }
        },
    ) { name, emoji, type ->
        editingCategory?.let { onUpdateCategory(it.copy(name = name, emoji = emoji)) } ?: onAddCategory(name, emoji, type)
        addingCategory = false; categoryEditor = null
    }
    if (addingAccount || accountEditor != null) AccountDialog(accountEditor, expenses, { addingAccount = false; accountEditor = null }) { name, currentBalance ->
        accountEditor?.let { account ->
            val movement = account.currentBalance(expenses) - account.openingBalanceCents
            onUpdateAccount(account.copy(name = name, openingBalanceCents = currentBalance - movement))
        } ?: onAddAccount(name, currentBalance)
        addingAccount = false; accountEditor = null
    }
    deleteCategory?.let { category ->
        CategoryDeleteDialog(
            category = category,
            replacements = categories.filter { it.id != category.id && it.type == category.type },
            onDismiss = { deleteCategory = null },
            onConfirm = { replacement, completed ->
                onDeleteCategory(category, replacement) { success ->
                    completed(success)
                    if (success) deleteCategory = null
                }
            },
            expenseCount = expenses.count { it.categoryId == category.id },
            onDeleteWithExpenses = { completed -> onDeleteCategoryWithExpenses(category) { success -> completed(success); if (success) deleteCategory = null } },
        )
    }
    deleteAccount?.let { account -> val count = expenses.count { it.accountId == account.id || it.transferAccountId == account.id }; ConfirmDelete(tr("Удалить счёт «${account.name}»?", "Delete account “${account.name}”?"), tr("Будут удалены счёт и $count связанных операций вместе с фотографиями. Отменить действие нельзя.", "The account and $count linked operations with photos will be deleted. This cannot be undone."), { deleteAccount = null }) { onDeleteAccount(account); deleteAccount = null } }
    pendingBackup?.let { archive -> ConfirmDelete(tr("Восстановить резервную копию?", "Restore backup?"), tr("Счета: ${archive.accounts.size}, категории: ${archive.categories.size}, операции: ${archive.expenses.size}, фотографии: ${archive.photos.size}. Текущие данные будут заменены, а их страховочная копия сохранится внутри приложения.", "Accounts: ${archive.accounts.size}, categories: ${archive.categories.size}, operations: ${archive.expenses.size}, photos: ${archive.photos.size}. Current data will be replaced after an internal safety copy is saved."), { pendingBackup = null }, confirmLabel = tr("Восстановить", "Restore"), destructive = true) { onRestoreBackup(archive); pendingBackup = null } }
    if (showExport) ExportDialog(
        accounts = accounts,
        expenses = expenses,
        categories = categories,
        selectedAccountIds = exportAccountIds,
        selectedCategoryIds = exportCategoryIds,
        onAccountToggle = { id -> exportAccountIds = exportAccountIds.toMutableSet().apply { if (!add(id)) remove(id) } },
        onCategoryToggle = { id -> exportCategoryIds = exportCategoryIds.toMutableSet().apply { if (!add(id)) remove(id) } },
        onDismiss = { showExport = false },
        onExport = { showExport = false; exportLauncher.launch("ArkMoney-${LocalDate.now()}.xlsx") },
    )
}

@Composable
private fun ExportDialog(
    accounts: List<Account>,
    expenses: List<Expense>,
    categories: List<Category>,
    selectedAccountIds: Set<Long>,
    selectedCategoryIds: Set<Long>,
    onAccountToggle: (Long) -> Unit,
    onCategoryToggle: (Long) -> Unit,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
) {
    var categoriesExpanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Экспорт в Excel", "Excel export")) },
        text = { Column(Modifier.heightIn(max = 480.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
            Text(tr("Счета", "Accounts"), style = MaterialTheme.typography.titleSmall)
            accounts.forEach { account -> Row(Modifier.fillMaxWidth().clickable { onAccountToggle(account.id) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(account.id in selectedAccountIds, { onAccountToggle(account.id) }); Column { Text(account.name); Text(formatMoney(account.currentBalance(expenses)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            TextButton({ categoriesExpanded = !categoriesExpanded }, Modifier.align(Alignment.End)) { Text(if (categoriesExpanded) tr("Скрыть категории", "Hide categories") else tr("Выбрать категории (${selectedCategoryIds.size})", "Select categories (${selectedCategoryIds.size})")) }
            if (categoriesExpanded) categories.forEach { category -> Row(Modifier.fillMaxWidth().clickable { onCategoryToggle(category.id) }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(category.id in selectedCategoryIds, { onCategoryToggle(category.id) }); Text("${category.emoji}  ${category.name}") } }
        } },
        confirmButton = { TextButton(onExport, enabled = selectedAccountIds.isNotEmpty()) { Text(tr("Экспортировать", "Export")) } },
        dismissButton = { TextButton(onDismiss) { Text(tr("Отмена", "Cancel")) } },
    )
}

@Composable private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(16.dp), content = { content() }) }
    }
}

@Composable private fun ManageRow(title: String, subtitle: String?, onEdit: () -> Unit, onDelete: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Medium); if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        TextButton(onEdit) { Text(tr("Изменить", "Edit")) }
        if (onDelete != null) TextButton(onDelete) { Text(tr("Удалить", "Delete"), color = MaterialTheme.colorScheme.error) }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f))
}

@Composable
private fun ReorderableCategoryList(
    categories: List<Category>,
    hapticsEnabled: Boolean,
    onEdit: (Category) -> Unit,
    onOrderChanged: (List<Category>) -> Unit,
) {
    var ordered by remember(categories.map { it.id }) { mutableStateOf(categories) }
    var draggedId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowHeight = 64.dp
    val density = androidx.compose.ui.platform.LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val rowHeightPx = with(density) { rowHeight.toPx() }
    LaunchedEffect(categories) { if (draggedId == null) ordered = categories }

    Column {
        ordered.forEach { category ->
            key(category.id) {
              val isDragged = draggedId == category.id
              CategoryManageRow(
                category = category,
                dragging = isDragged,
                modifier = Modifier.zIndex(if (isDragged) 1f else 0f).graphicsLayer {
                    translationY = if (isDragged) dragOffset else 0f
                    scaleX = if (isDragged) 1.015f else 1f
                    scaleY = if (isDragged) 1.015f else 1f
                },
                onEdit = { onEdit(category) },
                onDragStart = {
                    draggedId = category.id
                    dragOffset = 0f
                },
                onDrag = { delta ->
                    val currentIndex = ordered.indexOfFirst { it.id == category.id }
                    if (currentIndex >= 0) {
                        dragOffset += delta
                        when {
                            dragOffset > rowHeightPx / 2 && currentIndex < ordered.lastIndex -> {
                                ordered = ordered.toMutableList().apply { add(currentIndex + 1, removeAt(currentIndex)) }
                                dragOffset -= rowHeightPx
                                if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            dragOffset < -rowHeightPx / 2 && currentIndex > 0 -> {
                                ordered = ordered.toMutableList().apply { add(currentIndex - 1, removeAt(currentIndex)) }
                                dragOffset += rowHeightPx
                                if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }
                },
                onDragEnd = {
                    draggedId = null
                    dragOffset = 0f
                    onOrderChanged(ordered)
                },
                hapticsEnabled = hapticsEnabled,
              )
            }
        }
    }
}

@Composable private fun CategoryManageRow(category: Category, dragging: Boolean, modifier: Modifier = Modifier, hapticsEnabled: Boolean, onEdit: () -> Unit, onDragStart: () -> Unit, onDrag: (Float) -> Unit, onDragEnd: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(16.dp)
    Card(
        modifier.fillMaxWidth().height(64.dp).padding(vertical = 4.dp),
        shape = shape,
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = if (dragging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(0.dp),
        border = if (dragging) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f)),
    ) {
        Row(
            Modifier.fillMaxWidth().clip(shape).pointerInput(category.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart(); if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                    onDrag = { change, amount -> change.consume(); onDrag(amount.y) },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd,
                )
            }.clickable(onClick = onEdit).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) { Text(category.emoji, style = MaterialTheme.typography.titleLarge); Text(category.name, Modifier.padding(start = 12.dp).weight(1f), fontWeight = FontWeight.Medium); Text("≡", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable private fun CategoryDialog(category: Category?, onDismiss: () -> Unit, onDelete: (() -> Unit)?, onSave: (String, String, TransactionType) -> Unit) {
    var name by remember(category) { mutableStateOf(category?.name ?: "") }
    var emoji by remember(category) { mutableStateOf(category?.emoji ?: "•••") }
    var type by remember(category) { mutableStateOf(category?.let { TransactionType.valueOf(it.type) } ?: TransactionType.EXPENSE) }
    var emojiOpen by remember { mutableStateOf(false) }
    var emojiGroup by remember { mutableStateOf(categoryEmojiGroups.first()) }
    AlertDialog(onDismiss, title = { Text(if (category == null) tr("Новая категория", "New category") else tr("Изменить категорию", "Edit category")) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.material3.Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.clickable { emojiOpen = !emojiOpen }) { Text(emoji, Modifier.padding(16.dp), style = MaterialTheme.typography.headlineMedium) }
                OutlinedTextField(name, { name = it }, modifier = Modifier.weight(1f), label = { Text(tr("Название", "Name")) }, singleLine = true, shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
            }
            if (category == null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(TransactionType.EXPENSE to tr("Расход", "Expense"), TransactionType.INCOME to tr("Доход", "Income")).forEach { (value, label) -> SelectionTab(label, type == value, Modifier.weight(1f)) { type = value } } }
            TextButton({ emojiOpen = !emojiOpen }) { Text(if (emojiOpen) tr("Скрыть библиотеку эмодзи", "Hide emoji library") else tr("Выбрать эмодзи", "Choose emoji")) }
            if (emojiOpen) androidx.compose.material3.Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) { Column(Modifier.padding(8.dp)) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { categoryEmojiGroups.forEach { group -> SelectionTab(group.title, group == emojiGroup) { emojiGroup = group } } }
                LazyVerticalGrid(GridCells.Fixed(6), Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 260.dp)) { items(emojiGroup.emojis) { item -> Text(item, Modifier.clip(RoundedCornerShape(12.dp)).background(if (item == emoji) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent).clickable { emoji = item }.padding(9.dp), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center) } }
            } }
        }
    }, confirmButton = { TextButton({ if (name.isNotBlank()) onSave(name.trim(), emoji.ifBlank { "•••" }, type) }) { Text(tr("Сохранить", "Save")) } }, dismissButton = {
        Row {
            if (onDelete != null) TextButton(onClick = onDelete) { Text(tr("Удалить", "Delete"), color = MaterialTheme.colorScheme.error) }
            TextButton(onClick = onDismiss) { Text(tr("Отмена", "Cancel")) }
        }
    })
}

@Composable private fun SelectionTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    androidx.compose.material3.Surface(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) colors.primaryContainer else colors.surfaceContainerHigh,
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        tonalElevation = if (selected) 2.dp else 0.dp,
    ) { Text(label, Modifier.padding(horizontal = 13.dp, vertical = 9.dp), textAlign = TextAlign.Center, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) }
}

@Composable private fun CategoryDeleteDialog(category: Category, replacements: List<Category>, onDismiss: () -> Unit, onConfirm: (Category, (Boolean) -> Unit) -> Unit, expenseCount: Int, onDeleteWithExpenses: ((Boolean) -> Unit) -> Unit) {
    var replacement by remember(category.id) { mutableStateOf(replacements.firstOrNull()) }
    var deleting by remember(category.id) { mutableStateOf(false) }
    var confirmDeleteAll by remember(category.id) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!deleting) onDismiss() },
        title = { Text(tr("Удалить «${category.name}»?", "Delete “${category.name}”?")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tr("Все операции этой категории будут перенесены в выбранную категорию.", "All operations in this category will be moved to the selected category."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(replacements.size, key = { replacements[it].id }) { index ->
                        val item = replacements[index]
                        val selected = replacement?.id == item.id
                        androidx.compose.material3.Surface(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { replacement = item },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected, { replacement = item })
                                Text(item.emoji, style = MaterialTheme.typography.titleMedium)
                                Text(item.name, Modifier.padding(start = 10.dp), fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button({
            replacement?.let { target ->
                deleting = true
                onConfirm(target) { deleting = false }
            }
        }, enabled = replacement != null && !deleting) { Text(if (deleting) tr("Удаление…", "Deleting…") else tr("Перенести и удалить", "Move and delete")) } },
        dismissButton = { Row { TextButton({ confirmDeleteAll = true }, enabled = !deleting) { Text(tr("Удалить всё", "Delete all"), color = MaterialTheme.colorScheme.error) }; TextButton(onDismiss, enabled = !deleting) { Text(tr("Отмена", "Cancel")) } } },
    )
    if (confirmDeleteAll) AlertDialog(
        onDismissRequest = { confirmDeleteAll = false },
        title = { Text(tr("Удалить категорию и операции?", "Delete category and operations?")) },
        text = { Text(tr("Будут безвозвратно удалены категория «${category.name}», $expenseCount операций и прикреплённые к ним фотографии.", "The category “${category.name}”, $expenseCount operations, and their attached photos will be permanently deleted.")) },
        confirmButton = { TextButton({ confirmDeleteAll = false; deleting = true; onDeleteWithExpenses { deleting = false } }) { Text(tr("Удалить всё", "Delete all"), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton({ confirmDeleteAll = false }) { Text(tr("Отмена", "Cancel")) } },
    )
}

@Composable private fun AccountDialog(account: Account?, expenses: List<Expense>, onDismiss: () -> Unit, onSave: (String, Long) -> Unit) {
    val spent = account?.let { selected -> expenses.filter { it.accountId == selected.id }.sumOf { it.amountCents } } ?: 0
    var name by remember(account) { mutableStateOf(account?.name ?: "") }
    var balance by remember(account) { mutableStateOf(account?.let { ((it.openingBalanceCents - spent) / 100.0).toString() } ?: "0") }
    AlertDialog(onDismiss, title = { Text(if (account == null) tr("Новый счёт", "New account") else tr("Изменить счёт", "Edit account")) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text(tr("Название", "Name")) }, singleLine = true)
            OutlinedTextField(balance, { balance = it.filter { char -> char.isDigit() || char == ',' || char == '.' } }, label = { Text(tr("Текущий баланс", "Current balance")) }, suffix = { Text("₽") }, singleLine = true)
        }
    }, confirmButton = { TextButton({ parseMoneyCents(balance)?.let { if (name.isNotBlank()) onSave(name.trim(), it) } }) { Text(tr("Сохранить", "Save")) } }, dismissButton = { TextButton(onDismiss) { Text(tr("Отмена", "Cancel")) } })
}

@Composable private fun ConfirmDelete(title: String, body: String, onDismiss: () -> Unit, confirmLabel: String? = null, destructive: Boolean = true, onConfirm: () -> Unit) {
    AlertDialog(onDismiss, title = { Text(title) }, text = { Text(body) }, confirmButton = { TextButton(onConfirm) { Text(confirmLabel ?: tr("Удалить", "Delete"), color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) } }, dismissButton = { TextButton(onDismiss) { Text(tr("Отмена", "Cancel")) } })
}

private fun formatLimitInput(cents: Long): String = when {
    cents <= 0 -> ""
    cents % 100L == 0L -> (cents / 100L).toString()
    else -> "%.2f".format(java.util.Locale.US, cents / 100.0).replace('.', ',')
}
