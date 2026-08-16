package com.arkulz.arkmoney

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arkulz.arkmoney.data.Account
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

@Composable
fun SettingsScreen(
    modifier: Modifier,
    expenses: List<Expense>,
    categories: List<Category>,
    accounts: List<Account>,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAddCategory: (String, String, TransactionType) -> Unit,
    onUpdateCategory: (Category) -> Unit,
    onDeleteCategory: (Category, Category) -> Unit,
    onAddAccount: (String, Long) -> Unit,
    onUpdateAccount: (Account) -> Unit,
    onDeleteAccount: (Account) -> Unit,
    onGenerateDemoData: () -> Unit,
    onDeleteDemoData: () -> Unit,
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
    val uriHandler = LocalUriHandler.current
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
                runCatching { context.contentResolver.openOutputStream(uri)?.use { ExcelExporter.write(it, selectedExpenses, selectedCategories, selectedAccounts) } ?: error("Нет доступа к файлу") }.isSuccess
            }
            Toast.makeText(context, if (succeeded) "Excel-файл сохранён" else "Не удалось экспортировать данные", Toast.LENGTH_SHORT).show()
        }
    }

    if (showTesting) {
        LazyColumn(modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton({ showTesting = false }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }; Text("Тестирование", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) } }
            item { SettingsSection("Демо-данные") {
                Text("Создаёт отдельный демо-счёт с реалистичными расходами и доходами за последние 365 дней.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button({ confirmDemoFill = true }, Modifier.padding(top = 12.dp)) { Text("Заполнить демо-данными за год") }
                if (expenses.any { it.isDemo } || accounts.any { it.isDemo }) TextButton({ confirmDemoDelete = true }) { Text("Удалить демо-данные", color = MaterialTheme.colorScheme.error) }
            } }
        }
        if (confirmDemoFill) ConfirmDelete("Заполнить данные за год?", "Существующие демо-данные будут заменены. Личные расходы не изменятся.", { confirmDemoFill = false }, confirmLabel = "Создать", destructive = false) { onGenerateDemoData(); confirmDemoFill = false }
        if (confirmDemoDelete) ConfirmDelete("Удалить демо-данные?", "Будут удалены только созданные для тестирования расходы и демо-счёт.", { confirmDemoDelete = false }) { onDeleteDemoData(); confirmDemoDelete = false }
        return
    }

    LazyColumn(modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Text("Настройки", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item {
            SettingsSection("Оформление") {
                ThemeMode.entries.forEach { mode ->
                    val label = when (mode) { ThemeMode.SYSTEM -> "Как на устройстве"; ThemeMode.LIGHT -> "Светлая"; ThemeMode.DARK -> "Тёмная" }
                    Row(Modifier.fillMaxWidth().clickable { onThemeModeChange(mode) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(themeMode == mode, { onThemeModeChange(mode) })
                        Text(label, Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
        item {
            SettingsSection("Счета") {
                accounts.forEach { account ->
                    ManageRow(account.name, formatMoney(account.currentBalance(expenses)), { accountEditor = account }, if (accounts.size > 1) {{ deleteAccount = account }} else null)
                }
                TextButton({ addingAccount = true }) { Text("+ Добавить счёт") }
            }
        }
        item {
            SettingsSection("Категории") {
                TransactionType.entries.filter { it != TransactionType.TRANSFER }.forEach { type ->
                    Text(if (type == TransactionType.EXPENSE) "Расходы" else "Доходы", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    categories.filter { it.type == type.name }.forEach { category -> ManageRow("${category.emoji}  ${category.name}", null, { categoryEditor = category }, if (categories.count { it.type == category.type } > 1) {{ deleteCategory = category }} else null) }
                }
                TextButton({ addingCategory = true }) { Text("+ Добавить категорию") }
            }
        }
        item {
            SettingsSection("Данные") {
                Text("Все данные хранятся только на устройстве.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Button({ showExport = true }, modifier = Modifier.padding(top = 12.dp)) { Text("Экспортировать в Excel") }
            }
        }
        item { Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { TextButton({ uriHandler.openUri("https://github.com/ejevgg/ArkMoney") }) { Text("GitHub") }; Text("ArkMoney 0.2.0", Modifier.clickable { val next = nextVersionTap(versionTaps); versionTaps = next.first; showTesting = next.second }.padding(vertical = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) } }
    }

    if (addingCategory || categoryEditor != null) CategoryDialog(categoryEditor, { addingCategory = false; categoryEditor = null }) { name, emoji, type ->
        categoryEditor?.let { onUpdateCategory(it.copy(name = name, emoji = emoji)) } ?: onAddCategory(name, emoji, type)
        addingCategory = false; categoryEditor = null
    }
    if (addingAccount || accountEditor != null) AccountDialog(accountEditor, expenses, { addingAccount = false; accountEditor = null }) { name, currentBalance ->
        accountEditor?.let { account ->
            val movement = account.currentBalance(expenses) - account.openingBalanceCents
            onUpdateAccount(account.copy(name = name, openingBalanceCents = currentBalance - movement))
        } ?: onAddAccount(name, currentBalance)
        addingAccount = false; accountEditor = null
    }
    deleteCategory?.let { category -> CategoryDeleteDialog(category, categories.filter { it.id != category.id && it.type == category.type }, { deleteCategory = null }) { replacement -> onDeleteCategory(category, replacement); deleteCategory = null } }
    deleteAccount?.let { account -> val count = expenses.count { it.accountId == account.id || it.transferAccountId == account.id }; ConfirmDelete("Удалить счёт «${account.name}»?", "Будут удалены счёт и $count связанных операций вместе с фотографиями. Отменить действие нельзя.", { deleteAccount = null }) { onDeleteAccount(account); deleteAccount = null } }
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
        title = { Text("Экспорт в Excel") },
        text = { Column(Modifier.heightIn(max = 480.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
            Text("Счета", style = MaterialTheme.typography.titleSmall)
            accounts.forEach { account -> Row(Modifier.fillMaxWidth().clickable { onAccountToggle(account.id) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(account.id in selectedAccountIds, { onAccountToggle(account.id) }); Column { Text(account.name); Text(formatMoney(account.currentBalance(expenses)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            TextButton({ categoriesExpanded = !categoriesExpanded }, Modifier.align(Alignment.End)) { Text(if (categoriesExpanded) "Скрыть категории" else "Выбрать категории (${selectedCategoryIds.size})") }
            if (categoriesExpanded) categories.forEach { category -> Row(Modifier.fillMaxWidth().clickable { onCategoryToggle(category.id) }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(category.id in selectedCategoryIds, { onCategoryToggle(category.id) }); Text("${category.emoji}  ${category.name}") } }
        } },
        confirmButton = { TextButton(onExport, enabled = selectedAccountIds.isNotEmpty()) { Text("Экспортировать") } },
        dismissButton = { TextButton(onDismiss) { Text("Отмена") } },
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
        TextButton(onEdit) { Text("Изменить") }
        if (onDelete != null) TextButton(onDelete) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f))
}

@Composable private fun CategoryDialog(category: Category?, onDismiss: () -> Unit, onSave: (String, String, TransactionType) -> Unit) {
    var name by remember(category) { mutableStateOf(category?.name ?: "") }
    var emoji by remember(category) { mutableStateOf(category?.emoji ?: "•••") }
    var type by remember(category) { mutableStateOf(category?.let { TransactionType.valueOf(it.type) } ?: TransactionType.EXPENSE) }
    var emojiOpen by remember { mutableStateOf(false) }
    AlertDialog(onDismiss, title = { Text(if (category == null) "Новая категория" else "Изменить категорию") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Название") }, singleLine = true)
            if (category == null) Row { listOf(TransactionType.EXPENSE to "Расход", TransactionType.INCOME to "Доход").forEach { (value, label) -> TextButton({ type = value }) { Text(if (type == value) "● $label" else label) } } }
            TextButton({ emojiOpen = true }) { Text("$emoji  Выбрать эмодзи") }
            if (emojiOpen) Column { listOf("🛒","☕","🚕","🏠","❤️","🎬","💼","🛠️","🎁","📈","💰","✈️","🐾","📚").chunked(7).forEach { row -> Row { row.forEach { item -> Text(item, Modifier.clickable { emoji = item; emojiOpen = false }.padding(8.dp)) } } } }
        }
    }, confirmButton = { TextButton({ if (name.isNotBlank()) onSave(name.trim(), emoji.ifBlank { "•••" }, type) }) { Text("Сохранить") } }, dismissButton = { TextButton(onDismiss) { Text("Отмена") } })
}

@Composable private fun CategoryDeleteDialog(category: Category, replacements: List<Category>, onDismiss: () -> Unit, onConfirm: (Category) -> Unit) {
    var replacement by remember(category.id) { mutableStateOf(replacements.firstOrNull()) }
    AlertDialog(onDismiss, title = { Text("Удалить «${category.name}»?") }, text = { Column { Text("Выберите категорию, в которую будут перенесены операции:"); replacements.forEach { item -> Row(Modifier.fillMaxWidth().clickable { replacement = item }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(replacement?.id == item.id, { replacement = item }); Text("${item.emoji} ${item.name}") } } } }, confirmButton = { TextButton({ replacement?.let(onConfirm) }, enabled = replacement != null) { Text("Перенести и удалить", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onDismiss) { Text("Отмена") } })
}

@Composable private fun AccountDialog(account: Account?, expenses: List<Expense>, onDismiss: () -> Unit, onSave: (String, Long) -> Unit) {
    val spent = account?.let { selected -> expenses.filter { it.accountId == selected.id }.sumOf { it.amountCents } } ?: 0
    var name by remember(account) { mutableStateOf(account?.name ?: "") }
    var balance by remember(account) { mutableStateOf(account?.let { ((it.openingBalanceCents - spent) / 100.0).toString() } ?: "0") }
    AlertDialog(onDismiss, title = { Text(if (account == null) "Новый счёт" else "Изменить счёт") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Название") }, singleLine = true)
            OutlinedTextField(balance, { balance = it.filter { char -> char.isDigit() || char == ',' || char == '.' } }, label = { Text("Текущий баланс") }, suffix = { Text("₽") }, singleLine = true)
        }
    }, confirmButton = { TextButton({ parseMoneyCents(balance)?.let { if (name.isNotBlank()) onSave(name.trim(), it) } }) { Text("Сохранить") } }, dismissButton = { TextButton(onDismiss) { Text("Отмена") } })
}

@Composable private fun ConfirmDelete(title: String, body: String, onDismiss: () -> Unit, confirmLabel: String = "Удалить", destructive: Boolean = true, onConfirm: () -> Unit) {
    AlertDialog(onDismiss, title = { Text(title) }, text = { Text(body) }, confirmButton = { TextButton(onConfirm) { Text(confirmLabel, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) } }, dismissButton = { TextButton(onDismiss) { Text("Отмена") } })
}
