package com.arkulz.arkmoney

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arkulz.arkmoney.data.Account
import com.arkulz.arkmoney.data.Category
import com.arkulz.arkmoney.data.ExcelExporter
import com.arkulz.arkmoney.data.ExcelImporter
import com.arkulz.arkmoney.data.ImportedWorkbook
import com.arkulz.arkmoney.data.Expense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun SettingsScreen(
    modifier: Modifier,
    expenses: List<Expense>,
    categories: List<Category>,
    accounts: List<Account>,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAddCategory: (String, String) -> Unit,
    onUpdateCategory: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onAddAccount: (String, Long) -> Unit,
    onUpdateAccount: (Account) -> Unit,
    onDeleteAccount: (Account) -> Unit,
    onGenerateDemoData: () -> Unit,
    onDeleteDemoData: () -> Unit,
    onImportWorkbook: (ImportedWorkbook) -> Unit,
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
    var pendingImport by remember { mutableStateOf<ImportedWorkbook?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        if (uri != null) scope.launch {
            val succeeded = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openOutputStream(uri)?.use { ExcelExporter.write(it, expenses, categories, accounts) } ?: error("Нет доступа к файлу") }.isSuccess
            }
            Toast.makeText(context, if (succeeded) "Excel-файл сохранён" else "Не удалось экспортировать данные", Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val imported = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use(ExcelImporter::read) ?: error("Нет доступа к файлу") }
            }
            imported.onSuccess { pendingImport = it }
                .onFailure { Toast.makeText(context, "Не удалось импортировать файл ArkMoney", Toast.LENGTH_LONG).show() }
        }
    }

    if (showTesting) {
        LazyColumn(modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item { TextButton({ showTesting = false }) { Text("‹ Назад") }; Text("Тестирование", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
            item { SettingsSection("Демо-данные") {
                Text("Создаёт отдельный демо-счёт и реалистичные расходы за последние 365 дней.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    val spent = expenses.filter { it.accountId == account.id }.sumOf { it.amountCents }
                    ManageRow(account.name, formatMoney(account.openingBalanceCents - spent), { accountEditor = account }, if (accounts.size > 1) {{ deleteAccount = account }} else null)
                }
                TextButton({ addingAccount = true }) { Text("+ Добавить счёт") }
            }
        }
        item {
            SettingsSection("Категории") {
                categories.forEach { category -> ManageRow("${category.emoji}  ${category.name}", null, { categoryEditor = category }, if (categories.size > 1) {{ deleteCategory = category }} else null) }
                TextButton({ addingCategory = true }) { Text("+ Добавить категорию") }
            }
        }
        item {
            SettingsSection("Данные") {
                Text("Все данные хранятся только на устройстве.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Button({ exportLauncher.launch("ArkMoney-${LocalDate.now()}.xlsx") }, modifier = Modifier.padding(top = 12.dp)) { Text("Экспортировать в Excel") }
                TextButton({ importLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) }) { Text("Импортировать из Excel") }
            }
        }
        item { Text("ArkMoney 0.1.0", Modifier.clickable { val next = nextVersionTap(versionTaps); versionTaps = next.first; showTesting = next.second }.padding(vertical = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }

    if (addingCategory || categoryEditor != null) CategoryDialog(categoryEditor, { addingCategory = false; categoryEditor = null }) { name, emoji ->
        categoryEditor?.let { onUpdateCategory(it.copy(name = name, emoji = emoji)) } ?: onAddCategory(name, emoji)
        addingCategory = false; categoryEditor = null
    }
    if (addingAccount || accountEditor != null) AccountDialog(accountEditor, expenses, { addingAccount = false; accountEditor = null }) { name, currentBalance ->
        accountEditor?.let { account ->
            val spent = expenses.filter { it.accountId == account.id }.sumOf { it.amountCents }
            onUpdateAccount(account.copy(name = name, openingBalanceCents = currentBalance + spent))
        } ?: onAddAccount(name, currentBalance)
        addingAccount = false; accountEditor = null
    }
    deleteCategory?.let { category -> ConfirmDelete("Удалить категорию «${category.name}»?", "Её расходы будут перенесены в другую категорию.", { deleteCategory = null }) { onDeleteCategory(category); deleteCategory = null } }
    deleteAccount?.let { account -> ConfirmDelete("Удалить счёт «${account.name}»?", "Его расходы будут перенесены на другой счёт.", { deleteAccount = null }) { onDeleteAccount(account); deleteAccount = null } }
    pendingImport?.let { workbook -> ConfirmDelete("Импортировать ${workbook.expenses.size} операций?", "Новые расходы будут добавлены к существующим. Фотографии в Excel-файл не входят.", { pendingImport = null }, confirmLabel = "Импортировать", destructive = false) { onImportWorkbook(workbook); Toast.makeText(context, "Импортировано операций: ${workbook.expenses.size}", Toast.LENGTH_SHORT).show(); pendingImport = null } }
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

@Composable private fun CategoryDialog(category: Category?, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember(category) { mutableStateOf(category?.name ?: "") }
    var emoji by remember(category) { mutableStateOf(category?.emoji ?: "•••") }
    AlertDialog(onDismiss, title = { Text(if (category == null) "Новая категория" else "Изменить категорию") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Название") }, singleLine = true)
            OutlinedTextField(emoji, { emoji = it }, label = { Text("Эмодзи") }, supportingText = { Text("Откройте панель эмодзи на клавиатуре телефона") }, singleLine = true)
        }
    }, confirmButton = { TextButton({ if (name.isNotBlank()) onSave(name.trim(), emoji.ifBlank { "•••" }) }) { Text("Сохранить") } }, dismissButton = { TextButton(onDismiss) { Text("Отмена") } })
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
