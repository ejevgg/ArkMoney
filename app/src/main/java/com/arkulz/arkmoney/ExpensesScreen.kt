package com.arkulz.arkmoney

import android.net.Uri
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.arkulz.arkmoney.data.Account
import com.arkulz.arkmoney.data.Category
import com.arkulz.arkmoney.data.Expense
import com.arkulz.arkmoney.data.TransactionType
import com.arkulz.arkmoney.data.transactionType
import com.arkulz.arkmoney.data.storeExpensePhoto
import com.arkulz.arkmoney.data.createExpensePhotoTarget
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

@Composable
fun ExpensesScreen(
    modifier: Modifier,
    expenses: List<Expense>,
    categories: List<Category>,
    accounts: List<Account>,
    selectedAccount: Account?,
    onAccountSelected: (Account) -> Unit,
    onAddExpense: (Long, Category?, String, Long, TransactionType, Long?) -> Unit,
    onUpdateExpense: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var calculator by remember { mutableStateOf(CalculatorState()) }
    var calculatorVisible by rememberSaveable { mutableStateOf(true) }
    var selectedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var transactionTypeName by rememberSaveable { mutableStateOf(TransactionType.EXPENSE.name) }
    val transactionType = TransactionType.valueOf(transactionTypeName)
    var transferAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var quickTitle by rememberSaveable { mutableStateOf("") }
    var titleEditorVisible by rememberSaveable { mutableStateOf(false) }
    var chooseExpenseDay by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var selectedExpense by remember { mutableStateOf<Expense?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    val visibleCategories = categories.filter { it.type == transactionType.name }
    val selectedCategory = visibleCategories.firstOrNull { it.id == selectedCategoryId }
        ?: visibleCategories.firstOrNull().also { selectedCategoryId = it?.id }
    val allAccountExpenses = expenses.filter { it.accountId == selectedAccount?.id || it.transferAccountId == selectedAccount?.id }
    val accountExpenses = allAccountExpenses.filter { expense -> expense.matchesQuery(searchQuery, categories.firstOrNull { it.id == expense.categoryId }) }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val expense = selectedExpense
        if (uri != null && expense != null) scope.launch {
            runCatching { withContext(Dispatchers.IO) { storeExpensePhoto(context, uri) } }
                .onSuccess { path ->
                    if (expense.photoPath.isNotBlank() && expense.photoPath != path) runCatching { File(expense.photoPath).delete() }
                    val updated = expense.copy(photoPath = path)
                    onUpdateExpense(updated)
                    selectedExpense = updated
                }
                .onFailure { android.widget.Toast.makeText(context, "Не удалось добавить фотографию", android.widget.Toast.LENGTH_SHORT).show() }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val expense = selectedExpense
        val file = pendingCameraFile
        pendingCameraFile = null
        if (saved && expense != null && file != null) {
            if (expense.photoPath.isNotBlank() && expense.photoPath != file.absolutePath) runCatching { File(expense.photoPath).delete() }
            val updated = expense.copy(photoPath = file.absolutePath)
            onUpdateExpense(updated)
            selectedExpense = updated
        } else file?.delete()
    }

    fun save(daysAgo: Int) {
        val cents = calculator.amountCents() ?: return
        val category = selectedCategory
        if (transactionType != TransactionType.TRANSFER && category == null) return
        val destination = if (transactionType == TransactionType.TRANSFER) transferAccountId else null
        if (transactionType == TransactionType.TRANSFER && (destination == null || destination == selectedAccount?.id)) return
        val date = LocalDate.now().minusDays(daysAgo.toLong())
        val timestamp = date.atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        onAddExpense(cents, category, quickTitle.trim(), timestamp, transactionType, destination)
        calculator = CalculatorState()
        quickTitle = ""
        titleEditorVisible = false
    }

    Column(modifier.fillMaxSize()) {
        BalanceHeader(selectedAccount, accounts, expenses, onAccountSelected, searchQuery, { searchQuery = it }, searchVisible) {
            searchVisible = it
            if (!it) searchQuery = ""
        }
        ExpenseHistory(accountExpenses, categories, Modifier.weight(1f), searchVisible, onExpenseClick = { selectedExpense = it })
        AnimatedVisibility(calculatorVisible) {
            EntryPanel(
                calculator = calculator,
                categories = visibleCategories,
                accounts = accounts,
                expenses = expenses,
                selectedAccount = selectedAccount,
                transactionType = transactionType,
                transferAccountId = transferAccountId,
                onTransactionTypeChange = { transactionTypeName = it.name; selectedCategoryId = null },
                onTransferAccountSelected = { transferAccountId = it.id },
                selectedCategory = selectedCategory,
                quickTitle = quickTitle,
                onQuickTitleChange = { quickTitle = it },
                titleEditorVisible = titleEditorVisible,
                onTitleEditorVisibleChange = { titleEditorVisible = it },
                onCategorySelected = { selectedCategoryId = it.id },
                onDigit = { calculator = calculator.pressDigit(it) },
                onDecimal = { calculator = calculator.pressDecimal() },
                onOperation = { calculator = calculator.pressOperation(it) },
                onEquals = { calculator = calculator.pressEquals() },
                onClear = { calculator = CalculatorState() },
                onBackspace = { calculator = calculator.pressBackspace() },
                onCollapse = { calculatorVisible = false },
                onAdd = { save(0) },
                onChooseDay = { chooseExpenseDay = true },
            )
        }
        if (!calculatorVisible) {
            Surface(Modifier.fillMaxWidth().clickable { calculatorVisible = true }, color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.KeyboardArrowUp, null)
                    Text("Открыть калькулятор", Modifier.padding(start = 6.dp), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
    if (chooseExpenseDay) {
        AlertDialog(
            onDismissRequest = { chooseExpenseDay = false },
            title = { Text("Когда была операция?") },
            text = { Column {
                listOf(0 to "Сегодня", 1 to "Вчера", 2 to "Позавчера").forEach { (days, title) ->
                    Text(title, Modifier.fillMaxWidth().clickable { chooseExpenseDay = false; save(days) }.padding(vertical = 14.dp), fontWeight = FontWeight.Medium)
                }
            } },
            confirmButton = {},
            dismissButton = { TextButton({ chooseExpenseDay = false }) { Text("Отмена") } },
        )
    }
    selectedExpense?.let { expense ->
        ExpenseDetailDialog(
            expense = expense,
            category = categories.firstOrNull { it.id == expense.categoryId },
            onDismiss = { selectedExpense = null },
            onSave = { updated -> onUpdateExpense(updated); selectedExpense = null },
            onPickPhoto = { photoLauncher.launch(arrayOf("image/*")) },
            onTakePhoto = {
                val file = createExpensePhotoTarget(context)
                pendingCameraFile = file
                cameraLauncher.launch(FileProvider.getUriForFile(context, "${context.packageName}.files", file))
            },
            onRemovePhoto = {
                runCatching { File(expense.photoPath).delete() }
                val updated = expense.copy(photoPath = "")
                onUpdateExpense(updated)
                selectedExpense = updated
            },
            onDelete = { onDeleteExpense(expense); selectedExpense = null },
        )
    }
}

@Composable
private fun BalanceHeader(
    account: Account?,
    accounts: List<Account>,
    expenses: List<Expense>,
    onSelect: (Account) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchVisible: Boolean,
    onSearchVisibleChange: (Boolean) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val balance = account?.currentBalance(expenses) ?: 0
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Box(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("ArkMoney", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.CenterStart))
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatMoney(balance), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Box {
                    val accountShape = RoundedCornerShape(14.dp)
                    Text(account?.name ?: "Счёт", Modifier.clip(accountShape).background(MaterialTheme.colorScheme.secondaryContainer).clickable { menuOpen = true }.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                    DropdownMenu(menuOpen, { menuOpen = false }) { accounts.forEach { item -> DropdownMenuItem({ Column { Text(item.name); Text(formatMoney(item.currentBalance(expenses)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }, onClick = { onSelect(item); menuOpen = false }) } }
                }
            }
            IconButton({ onSearchVisibleChange(!searchVisible) }, Modifier.align(Alignment.CenterEnd)) {
                Icon(if (searchVisible) Icons.Default.Close else Icons.Default.Search, if (searchVisible) "Закрыть поиск" else "Поиск")
            }
        }
        AnimatedVisibility(searchVisible) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                placeholder = { Text("Название или сумма") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseHistory(expenses: List<Expense>, categories: List<Category>, modifier: Modifier, showDateAction: Boolean, onExpenseClick: (Expense) -> Unit) {
    val groups = expenses.groupBy { it.localDate() }.entries.toList()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showPicker by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        if (showDateAction) TextButton({ showPicker = true }, Modifier.align(Alignment.End).padding(horizontal = 8.dp)) { Text("Перейти к дате") }
        if (expenses.isEmpty()) Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Пока нет операций", style = MaterialTheme.typography.titleMedium)
            Text("Выберите тип и введите сумму", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } } else LazyColumn(Modifier.fillMaxWidth().weight(1f), state = listState) {
            groups.forEach { (date, dayExpenses) ->
                item(key = "day-$date") { DayHeader(date, dayExpenses.sumOf { if (it.transactionType == TransactionType.INCOME) -it.amountCents else if (it.transactionType == TransactionType.EXPENSE) it.amountCents else 0L }) }
                items(dayExpenses.size, key = { dayExpenses[it].id }) { index ->
                    val expense = dayExpenses[index]
                    ExpenseRow(expense, categories.firstOrNull { it.id == expense.categoryId }, onExpenseClick)
                    if (index != dayExpenses.lastIndex) HorizontalDivider(Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f))
                }
            }
        }
    }
    if (showPicker) {
        val picker = androidx.compose.material3.rememberDatePickerState()
        DatePickerDialog({ showPicker = false }, confirmButton = { TextButton({
            val selected = picker.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate() }
            val index = groups.indexOfFirst { selected != null && !it.key.isAfter(selected) }
            if (index >= 0) scope.launch { listState.animateScrollToItem(groups.take(index).sumOf { 1 + it.value.size }) }
            showPicker = false
        }) { Text("Перейти") } }, dismissButton = { TextButton({ showPicker = false }) { Text("Отмена") } }) { DatePicker(picker) }
    }
}

@Composable private fun DayHeader(date: LocalDate, total: Long) {
    val today = LocalDate.now()
    val title = when (date) { today -> "Сегодня"; today.minusDays(1) -> "Вчера"; else -> date.format(DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru"))) }
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow).padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatMoney(total), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun ExpenseRow(expense: Expense, category: Category?, onClick: (Expense) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick(expense) }.padding(horizontal = 20.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape), contentAlignment = Alignment.Center) { Text(category?.emoji ?: "•••", fontSize = 17.sp) }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(expense.title.ifBlank { if (expense.transactionType == TransactionType.TRANSFER) "Перевод" else category?.name ?: expense.category })
            Text(Instant.ofEpochMilli(expense.createdAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (expense.comment.isNotBlank()) Text(expense.comment, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
        val prefix = when (expense.transactionType) { TransactionType.EXPENSE -> "−"; TransactionType.INCOME -> "+"; TransactionType.TRANSFER -> "→ " }
        Text(prefix + formatMoney(expense.amountCents), fontWeight = FontWeight.SemiBold, color = if (expense.transactionType == TransactionType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseDetailDialog(
    expense: Expense,
    category: Category?,
    onDismiss: () -> Unit,
    onSave: (Expense) -> Unit,
    onPickPhoto: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onDelete: () -> Unit,
) {
    var title by remember(expense.id) { mutableStateOf(expense.title) }
    var description by remember(expense.id) { mutableStateOf(expense.comment) }
    var amount by remember(expense.id) { mutableStateOf("%.2f".format(Locale.US, expense.amountCents / 100.0)) }
    var createdAt by remember(expense.id) { mutableStateOf(expense.createdAt) }
    var editDate by remember { mutableStateOf(false) }
    var editTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val currentDateTime = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault())
    val parsedAmount = parseMoneyCents(amount)
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().heightIn(max = 720.dp).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title.ifBlank { if (expense.transactionType == TransactionType.TRANSFER) "Перевод" else category?.name ?: expense.category }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        Text(category?.let { "${it.emoji}  ${it.name}" } ?: "Перевод между счетами", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton({ confirmDelete = true }) { Icon(Icons.Default.Delete, "Удалить операцию", tint = MaterialTheme.colorScheme.error) }
                }
                OutlinedTextField(amount, { value -> amount = value.filter { it.isDigit() || it == ',' || it == '.' } }, Modifier.fillMaxWidth(), label = { Text("Сумма") }, suffix = { Text("₽") }, singleLine = true, isError = parsedAmount == null || parsedAmount <= 0)
                OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Название операции") }, placeholder = { Text(if (expense.transactionType == TransactionType.TRANSFER) "Перевод" else category?.name ?: expense.category) }, singleLine = true)
                OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("Описание") }, minLines = 1, maxLines = 3)
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                        TextButton({ editDate = true }, Modifier.weight(1f)) { Text(currentDateTime.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("ru")))) }
                        TextButton({ editTime = true }, Modifier.weight(1f)) { Text(currentDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))) }
                    }
                }
                Text("Фотография", style = MaterialTheme.typography.titleSmall)
                if (expense.photoPath.isNotBlank()) {
                    AndroidView(
                        factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
                        update = { it.setImageURI(Uri.fromFile(File(expense.photoPath))) },
                        modifier = Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainer),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onTakePhoto, Modifier.weight(1f)) { Text("Камера") }
                    FilledTonalButton(onPickPhoto, Modifier.weight(1f)) { Text("Галерея") }
                    if (expense.photoPath.isNotBlank()) IconButton(onRemovePhoto) { Icon(Icons.Default.Delete, "Удалить фотографию", tint = MaterialTheme.colorScheme.error) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onDismiss) { Text("Отмена") }
                    Button({ parsedAmount?.let { onSave(expense.copy(amountCents = it, title = title.trim(), comment = description.trim(), createdAt = createdAt)) } }, enabled = parsedAmount != null && parsedAmount > 0) { Text("Сохранить") }
                }
            }
        }
    }
    if (editDate) {
        val picker = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = currentDateTime.toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog({ editDate = false }, confirmButton = { TextButton({
            picker.selectedDateMillis?.let { millis -> val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate(); createdAt = date.atTime(currentDateTime.toLocalTime()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }
            editDate = false
        }) { Text("Готово") } }, dismissButton = { TextButton({ editDate = false }) { Text("Отмена") } }) { DatePicker(picker) }
    }
    if (editTime) {
        val picker = rememberTimePickerState(currentDateTime.hour, currentDateTime.minute, true)
        AlertDialog({ editTime = false }, title = { Text("Время операции") }, text = { TimePicker(picker) }, confirmButton = { TextButton({ createdAt = currentDateTime.toLocalDate().atTime(picker.hour, picker.minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(); editTime = false }) { Text("Готово") } }, dismissButton = { TextButton({ editTime = false }) { Text("Отмена") } })
    }
    if (confirmDelete) AlertDialog({ confirmDelete = false }, title = { Text("Удалить операцию?") }, text = { Text("Операция и прикреплённая фотография будут удалены без возможности восстановления.") }, confirmButton = { TextButton(onDelete) { Text("Удалить", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton({ confirmDelete = false }) { Text("Отмена") } })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryPanel(
    calculator: CalculatorState,
    categories: List<Category>,
    accounts: List<Account>,
    expenses: List<Expense>,
    selectedAccount: Account?,
    transactionType: TransactionType,
    transferAccountId: Long?,
    onTransactionTypeChange: (TransactionType) -> Unit,
    onTransferAccountSelected: (Account) -> Unit,
    selectedCategory: Category?,
    quickTitle: String,
    onQuickTitleChange: (String) -> Unit,
    titleEditorVisible: Boolean,
    onTitleEditorVisibleChange: (Boolean) -> Unit,
    onCategorySelected: (Category) -> Unit,
    onDigit: (Char) -> Unit,
    onDecimal: () -> Unit,
    onOperation: (Char) -> Unit,
    onEquals: () -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onCollapse: () -> Unit,
    onAdd: () -> Unit,
    onChooseDay: () -> Unit,
) {
    val titleFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val editorScope = rememberCoroutineScope()
    fun closeTitleEditor() {
        keyboardController?.hide()
        editorScope.launch { delay(180); onTitleEditorVisibleChange(false) }
    }
    fun collapseCalculator() {
        keyboardController?.hide()
        editorScope.launch {
            delay(if (titleEditorVisible) 240 else 80)
            onTitleEditorVisibleChange(false)
            onCollapse()
        }
    }
    LaunchedEffect(titleEditorVisible) {
        if (titleEditorVisible) {
            titleFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), elevation = CardDefaults.cardElevation(8.dp)) {
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(TransactionType.EXPENSE to "Расход", TransactionType.INCOME to "Доход", TransactionType.TRANSFER to "Перевод").forEach { (type, label) ->
                    val shape = RoundedCornerShape(14.dp)
                    Text(label, Modifier.weight(1f).clip(shape).background(if (type == transactionType) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh).clickable { onTransactionTypeChange(type) }.padding(vertical = 8.dp), textAlign = TextAlign.Center, fontWeight = if (type == transactionType) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (transactionType == TransactionType.TRANSFER) {
                    var transferMenu by remember { mutableStateOf(false) }
                    Box(Modifier.weight(1f)) {
                        val target = accounts.firstOrNull { it.id == transferAccountId }
                        Text(target?.let { "На счёт: ${it.name}" } ?: "Выбрать счёт назначения", Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.secondaryContainer).clickable { transferMenu = true }.padding(12.dp))
                        DropdownMenu(transferMenu, { transferMenu = false }) {
                            accounts.filter { it.id != selectedAccount?.id }.forEach { account ->
                                DropdownMenuItem(
                                    text = { Column { Text(account.name); Text(formatMoney(account.currentBalance(expenses)), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) } },
                                    onClick = { onTransferAccountSelected(account); transferMenu = false },
                                )
                            }
                        }
                    }
                } else Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { categories.forEach { CategoryChip(it, it.id == selectedCategory?.id) { onCategorySelected(it) } } }
                IconButton(::collapseCalculator) { Icon(Icons.Default.KeyboardArrowDown, "Скрыть калькулятор") }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 3.dp), horizontalAlignment = Alignment.End) {
                Text(calculator.expression.replace('.', ','), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, maxLines = 1)
                Row(verticalAlignment = Alignment.Bottom) { Text(calculator.display.replace('.', ','), fontSize = 32.sp, fontWeight = FontWeight.Medium, maxLines = 1); Text(" ₽", fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp)) }
            }
            if (titleEditorVisible) {
                OutlinedTextField(
                    value = quickTitle,
                    onValueChange = onQuickTitleChange,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp).focusRequester(titleFocusRequester),
                    label = { Text("Название операции") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { closeTitleEditor() }),
                    trailingIcon = { IconButton({ closeTitleEditor() }) { Icon(Icons.Default.Close, "Готово") } },
                )
            } else {
                TextButton({ onTitleEditorVisibleChange(true) }, Modifier.padding(horizontal = 10.dp)) {
                    Text(if (quickTitle.isBlank()) "+ Название операции" else quickTitle, maxLines = 1)
                }
            }
            if (!titleEditorVisible) {
                val rows = listOf(listOf("C", "⌫", "÷", "×"), listOf("7", "8", "9", "−"), listOf("4", "5", "6", "+"), listOf("1", "2", "3", "="))
                rows.forEach { row -> Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp)) { row.forEach { key -> CalculatorKey(key, Modifier.weight(1f)) {
                when (key) { "C" -> onClear(); "⌫" -> onBackspace(); "÷", "×", "+" -> onOperation(key.first()); "−" -> onOperation('-'); "=" -> onEquals(); else -> onDigit(key.first()) }
            } } } }
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
                    CalculatorKey("0", Modifier.weight(2f)) { onDigit('0') }
                    CalculatorKey(",", Modifier.weight(1f)) { onDecimal() }
                    val addShape = RoundedCornerShape(18.dp)
                    Box(Modifier.weight(1f).height(52.dp).padding(3.dp).clip(addShape).background(MaterialTheme.colorScheme.primary).combinedClickable(enabled = calculator.amountCents() != null, onClick = onAdd, onLongClick = onChooseDay), contentAlignment = Alignment.Center) {
                        Text("⏎", color = MaterialTheme.colorScheme.onPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable private fun CategoryChip(category: Category, selected: Boolean, onClick: () -> Unit) { val shape = RoundedCornerShape(16.dp); Column(Modifier.clip(shape).background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(category.emoji, fontSize = 17.sp); Text(category.name, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) } }
@Composable private fun CalculatorKey(label: String, modifier: Modifier, onClick: () -> Unit) { val shape = RoundedCornerShape(16.dp); Box(modifier.height(44.dp).padding(3.dp).clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable(onClick = onClick), contentAlignment = Alignment.Center) { Text(label, fontSize = 20.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center) } }
