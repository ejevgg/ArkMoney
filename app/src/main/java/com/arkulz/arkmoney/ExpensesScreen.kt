package com.arkulz.arkmoney

import android.net.Uri
import android.content.Context
import android.graphics.Matrix
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arkulz.arkmoney.data.Account
import com.arkulz.arkmoney.data.Category
import com.arkulz.arkmoney.data.Expense
import com.arkulz.arkmoney.data.TransactionType
import com.arkulz.arkmoney.data.transactionType
import com.arkulz.arkmoney.data.storeExpensePhoto
import com.arkulz.arkmoney.data.createExpensePhotoTarget
import com.arkulz.arkmoney.data.compressExpensePhoto
import com.arkulz.arkmoney.data.saveExpensePhotoToGallery
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
    onAddExpense: (Long, Category?, String, Long, TransactionType, Long?, (Long) -> Unit) -> Unit,
    onUndoExpense: (Long) -> Unit,
    onUpdateExpense: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    onMoveCategory: (Category, Int) -> Unit,
    hapticsEnabled: Boolean,
    showCategoryEmoji: Boolean,
    dailyLimitCents: Long?,
) {
    val context = LocalContext.current
    val operationAddedText = tr("Операция добавлена", "Operation added")
    val undoText = tr("Отменить", "Undo")
    val photoAddError = tr("Не удалось добавить фотографию", "Could not add photo")
    val photoProcessError = tr("Не удалось обработать фотографию", "Could not process photo")
    val scope = rememberCoroutineScope()
    val historyState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var calculator by remember { mutableStateOf(CalculatorState()) }
    var calculatorVisible by rememberSaveable { mutableStateOf(true) }
    val calculatorVisibility = remember { MutableTransitionState(true) }
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
    val haptic = LocalHapticFeedback.current
    var displayedCategories by remember { mutableStateOf(categories) }
    LaunchedEffect(categories) { displayedCategories = categories }
    LaunchedEffect(calculatorVisible) { calculatorVisibility.targetState = calculatorVisible }
    BackHandler(calculatorVisible) { calculatorVisible = false }
    val visibleCategories = displayedCategories.filter { it.type == transactionType.name }
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
                .onFailure { android.widget.Toast.makeText(context, photoAddError, android.widget.Toast.LENGTH_SHORT).show() }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val expense = selectedExpense
        val file = pendingCameraFile
        pendingCameraFile = null
        if (saved && expense != null && file != null) scope.launch {
            runCatching { withContext(Dispatchers.IO) { compressExpensePhoto(file) } }
                .onSuccess {
                    if (expense.photoPath.isNotBlank() && expense.photoPath != file.absolutePath) runCatching { File(expense.photoPath).delete() }
                    val updated = expense.copy(photoPath = file.absolutePath); onUpdateExpense(updated); selectedExpense = updated
                }
                .onFailure { file.delete(); Toast.makeText(context, photoProcessError, Toast.LENGTH_SHORT).show() }
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
        onAddExpense(cents, category, quickTitle.trim(), timestamp, transactionType, destination) { createdId ->
            scope.launch {
                historyState.animateScrollToItem(0)
                val result = snackbarHostState.showSnackbar(operationAddedText, undoText, withDismissAction = true)
                if (result == SnackbarResult.ActionPerformed) onUndoExpense(createdId)
            }
        }
        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        calculator = CalculatorState()
        quickTitle = ""
        titleEditorVisible = false
    }

    Box(modifier.fillMaxSize()) {
      Column(Modifier.fillMaxSize()) {
        BalanceHeader(selectedAccount, accounts, expenses, onAccountSelected, searchQuery, { searchQuery = it }, searchVisible) {
            searchVisible = it
            if (!it) searchQuery = ""
        }
        ExpenseHistory(accountExpenses, categories, Modifier.weight(1f), historyState, searchVisible, dailyLimitCents, onUserScroll = { calculatorVisible = false }, onExpenseClick = { selectedExpense = it })
        AnimatedVisibility(visibleState = calculatorVisibility) {
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
                onMoveCategory = { category, direction ->
                    val moved = reorderedCategoryType(displayedCategories, category.id, direction)
                    displayedCategories = displayedCategories.filter { it.type != category.type } + moved
                    onMoveCategory(category, direction)
                },
                hapticsEnabled = hapticsEnabled,
                showCategoryEmoji = showCategoryEmoji,
                onDigit = { calculator = calculator.pressDigit(it) },
                onDecimal = { calculator = calculator.pressDecimal() },
                onOperation = { calculator = calculator.pressOperation(it) },
                onClear = { calculator = CalculatorState() },
                onBackspace = { calculator = calculator.pressBackspace() },
                onCollapse = { calculatorVisible = false },
                onAdd = { save(0) },
                onChooseDay = { chooseExpenseDay = true },
            )
        }
        if (calculatorVisibility.isIdle && !calculatorVisibility.currentState && !calculatorVisibility.targetState) {
            Surface(Modifier.fillMaxWidth().pointerInput(Unit) {
                var drag = 0f
                detectVerticalDragGestures(onVerticalDrag = { change, amount -> change.consume(); drag += amount }, onDragEnd = { if (drag < -24f) calculatorVisible = true })
            }.clickable { calculatorVisible = true }, color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.KeyboardArrowUp, null)
                    Text(tr("Открыть калькулятор", "Open calculator"), Modifier.padding(start = 6.dp), fontWeight = FontWeight.Medium)
                }
            }
        }
      }
      SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(bottom = if (calculatorVisible) 12.dp else 52.dp))
    }
    if (chooseExpenseDay) {
        AlertDialog(
            onDismissRequest = { chooseExpenseDay = false },
            title = { Text(tr("Когда была операция?", "When was the operation?")) },
            text = { Column {
                listOf(0 to tr("Сегодня", "Today"), 1 to tr("Вчера", "Yesterday"), 2 to tr("Позавчера", "Two days ago")).forEach { (days, title) ->
                    Text(title, Modifier.fillMaxWidth().clickable { chooseExpenseDay = false; save(days) }.padding(vertical = 14.dp), fontWeight = FontWeight.Medium)
                }
            } },
            confirmButton = {},
            dismissButton = { TextButton({ chooseExpenseDay = false }) { Text(tr("Отмена", "Cancel")) } },
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

@OptIn(ExperimentalMaterial3Api::class)
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
    val searchFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(searchVisible) {
        if (searchVisible) { delay(120); searchFocusRequester.requestFocus(); keyboard?.show() }
        else keyboard?.hide()
    }
    val balance = account?.currentBalance(expenses) ?: 0
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Box(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("ArkMoney", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.CenterStart))
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatMoney(balance), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                val accountShape = RoundedCornerShape(14.dp)
                Text("${account?.name ?: tr("Счёт", "Account")}  ▾", Modifier.clip(accountShape).background(MaterialTheme.colorScheme.secondaryContainer).clickable { menuOpen = true }.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
            }
            IconButton({ if (searchVisible) keyboard?.hide(); onSearchVisibleChange(!searchVisible) }, Modifier.align(Alignment.CenterEnd)) {
                Icon(if (searchVisible) Icons.Default.Close else Icons.Default.Search, if (searchVisible) tr("Закрыть поиск", "Close search") else tr("Поиск", "Search"))
            }
        }
        AnimatedVisibility(searchVisible) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).focusRequester(searchFocusRequester),
                placeholder = { Text(tr("Название или сумма", "Name or amount")) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
            )
        }
    }
    if (menuOpen) ModalBottomSheet(onDismissRequest = { menuOpen = false }) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Text(tr("Счета", "Accounts"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
            accounts.forEach { item ->
                val selected = item.id == account?.id
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(18.dp)).clickable { onSelect(item); menuOpen = false },
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape))
                        Text(item.name, Modifier.padding(start = 12.dp).weight(1f), fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                        Text(formatMoney(item.currentBalance(expenses)), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tr("Всего на счетах", "Total across accounts"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatMoney(accounts.sumOf { it.currentBalance(expenses) }), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseHistory(expenses: List<Expense>, categories: List<Category>, modifier: Modifier, listState: androidx.compose.foundation.lazy.LazyListState, showDateAction: Boolean, dailyLimitCents: Long?, onUserScroll: () -> Unit, onExpenseClick: (Expense) -> Unit) {
    val groups = expenses.groupBy { it.localDate() }.entries.toList()
    val scope = rememberCoroutineScope()
    var showPicker by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        if (showDateAction) TextButton({ showPicker = true }, Modifier.align(Alignment.End).padding(horizontal = 8.dp)) { Text(tr("Перейти к дате", "Go to date")) }
        if (expenses.isEmpty()) Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(tr("Пока нет операций", "No operations yet"), style = MaterialTheme.typography.titleMedium)
            Text(tr("Выберите тип и введите сумму", "Choose a type and enter an amount"), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } } else LazyColumn(Modifier.fillMaxWidth().weight(1f).pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.changes.any { it.pressed && it.positionChange().y != 0f }) onUserScroll()
                }
            }
        }, state = listState) {
            groups.forEach { (date, dayExpenses) ->
                item(key = "day-$date") { DayHeader(date, dayExpenses.filter { it.transactionType == TransactionType.EXPENSE }.sumOf { it.amountCents }, dailyLimitCents) }
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
        }) { Text(tr("Перейти", "Go")) } }, dismissButton = { TextButton({ showPicker = false }) { Text(tr("Отмена", "Cancel")) } }) { DatePicker(picker) }
    }
}

@Composable private fun DayHeader(date: LocalDate, total: Long, dailyLimitCents: Long?) {
    val today = LocalDate.now()
    val language = LocalAppLanguage.current
    val title = when (date) { today -> tr("Сегодня", "Today"); today.minusDays(1) -> tr("Вчера", "Yesterday"); else -> date.format(DateTimeFormatter.ofPattern("d MMMM", language.locale())) }
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow).padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(if (date == today && dailyLimitCents != null && dailyLimitCents > 0) "${formatMoney(total)} / ${formatMoney(dailyLimitCents)}" else formatMoney(total), color = if (date == today && dailyLimitCents != null && total > dailyLimitCents) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun ExpenseRow(expense: Expense, category: Category?, onClick: (Expense) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick(expense) }.padding(horizontal = 20.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape), contentAlignment = Alignment.Center) { Text(category?.emoji ?: "•••", fontSize = 17.sp) }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(expense.title.ifBlank { if (expense.transactionType == TransactionType.TRANSFER) tr("Перевод", "Transfer") else category?.name ?: expense.category })
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
    var showPhoto by remember { mutableStateOf(false) }
    val language = LocalAppLanguage.current
    val photoSavedText = tr("Фотография сохранена", "Photo saved")
    val photoSaveErrorText = tr("Не удалось сохранить фотографию", "Could not save photo")
    val currentDateTime = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault())
    val parsedAmount = parseMoneyCents(amount)
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().heightIn(max = 720.dp).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title.ifBlank { if (expense.transactionType == TransactionType.TRANSFER) tr("Перевод", "Transfer") else category?.name ?: expense.category }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        Text(category?.let { "${it.emoji}  ${it.name}" } ?: tr("Перевод между счетами", "Transfer between accounts"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton({ confirmDelete = true }) { Icon(Icons.Default.Delete, tr("Удалить операцию", "Delete operation"), tint = MaterialTheme.colorScheme.error) }
                }
                OutlinedTextField(amount, { value -> amount = value.filter { it.isDigit() || it == ',' || it == '.' } }, Modifier.fillMaxWidth(), label = { Text(tr("Сумма", "Amount")) }, suffix = { Text("₽") }, singleLine = true, isError = parsedAmount == null || parsedAmount <= 0)
                OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text(tr("Название операции", "Operation name")) }, placeholder = { Text(if (expense.transactionType == TransactionType.TRANSFER) tr("Перевод", "Transfer") else category?.name ?: expense.category) }, singleLine = true)
                OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text(tr("Описание", "Description")) }, minLines = 1, maxLines = 3)
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                        TextButton({ editDate = true }, Modifier.weight(1f)) { Text(currentDateTime.format(DateTimeFormatter.ofPattern("d MMM yyyy", language.locale()))) }
                        TextButton({ editTime = true }, Modifier.weight(1f)) { Text(currentDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))) }
                    }
                }
                Text(tr("Фотография", "Photo"), style = MaterialTheme.typography.titleSmall)
                if (expense.photoPath.isNotBlank()) {
                    AndroidView(
                        factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
                        update = { it.setImageURI(Uri.fromFile(File(expense.photoPath))) },
                        modifier = Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainer).clickable { showPhoto = true },
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onTakePhoto, Modifier.weight(1f)) { Text(tr("Камера", "Camera")) }
                    FilledTonalButton(onPickPhoto, Modifier.weight(1f)) { Text(tr("Галерея", "Gallery")) }
                    if (expense.photoPath.isNotBlank()) IconButton(onRemovePhoto) { Icon(Icons.Default.Delete, tr("Удалить фотографию", "Remove photo"), tint = MaterialTheme.colorScheme.error) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onDismiss) { Text(tr("Отмена", "Cancel")) }
                    Button({ parsedAmount?.let { onSave(expense.copy(amountCents = it, title = title.trim(), comment = description.trim(), createdAt = createdAt)) } }, enabled = parsedAmount != null && parsedAmount > 0) { Text(tr("Сохранить", "Save")) }
                }
            }
        }
    }
    if (editDate) {
        val picker = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = currentDateTime.toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog({ editDate = false }, confirmButton = { TextButton({
            picker.selectedDateMillis?.let { millis -> val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate(); createdAt = date.atTime(currentDateTime.toLocalTime()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }
            editDate = false
        }) { Text(tr("Готово", "Done")) } }, dismissButton = { TextButton({ editDate = false }) { Text(tr("Отмена", "Cancel")) } }) { DatePicker(picker) }
    }
    if (editTime) {
        val picker = rememberTimePickerState(currentDateTime.hour, currentDateTime.minute, true)
        AlertDialog({ editTime = false }, title = { Text(tr("Время операции", "Operation time")) }, text = { TimePicker(picker) }, confirmButton = { TextButton({ createdAt = currentDateTime.toLocalDate().atTime(picker.hour, picker.minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(); editTime = false }) { Text(tr("Готово", "Done")) } }, dismissButton = { TextButton({ editTime = false }) { Text(tr("Отмена", "Cancel")) } })
    }
    if (confirmDelete) AlertDialog({ confirmDelete = false }, title = { Text(tr("Удалить операцию?", "Delete operation?")) }, text = { Text(tr("Операция и прикреплённая фотография будут удалены без возможности восстановления.", "The operation and attached photo will be permanently deleted.")) }, confirmButton = { TextButton(onDelete) { Text(tr("Удалить", "Delete"), color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton({ confirmDelete = false }) { Text(tr("Отмена", "Cancel")) } })
    if (showPhoto && expense.photoPath.isNotBlank()) {
        val context = LocalContext.current
        Dialog(onDismissRequest = { showPhoto = false }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            Surface(Modifier.fillMaxSize(), color = Color.Black, shape = RoundedCornerShape(0.dp)) {
                Box(Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ZoomablePhotoView(it) },
                        update = { it.show(File(expense.photoPath)) },
                        modifier = Modifier.fillMaxSize(),
                    )
                    Row(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp)) {
                        TextButton({
                            val saved = saveExpensePhotoToGallery(context, File(expense.photoPath))
                            Toast.makeText(context, if (saved) photoSavedText else photoSaveErrorText, Toast.LENGTH_SHORT).show()
                        }) { Text(tr("Сохранить", "Save"), color = Color.White) }
                        TextButton({ showPhoto = false }) { Text(tr("Закрыть", "Close"), color = Color.White) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
    onMoveCategory: (Category, Int) -> Unit,
    hapticsEnabled: Boolean,
    showCategoryEmoji: Boolean,
    onDigit: (Char) -> Unit,
    onDecimal: () -> Unit,
    onOperation: (Char) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onCollapse: () -> Unit,
    onAdd: () -> Unit,
    onChooseDay: () -> Unit,
) {
    val titleFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val editorScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var transferAccountSheetOpen by remember { mutableStateOf(false) }
    fun feedback() { if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
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
            var typeMenuOpen by remember { mutableStateOf(false) }
            Box(
                Modifier.fillMaxWidth().height(20.dp).pointerInput(titleEditorVisible) {
                    var dragDistance = 0f
                    detectVerticalDragGestures(
                        onDragStart = { dragDistance = 0f },
                        onVerticalDrag = { change, amount -> change.consume(); if (amount > 0f) dragDistance += amount },
                        onDragEnd = { if (dragDistance > 28f) collapseCalculator() },
                    )
                },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .35f)))
            }
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 6.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    val typeLabel = when (transactionType) { TransactionType.EXPENSE -> tr("Расход", "Expense"); TransactionType.INCOME -> tr("Доход", "Income"); TransactionType.TRANSFER -> tr("Перевод", "Transfer") }
                    Text("$typeLabel  ▾", Modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer).clickable { typeMenuOpen = true }.padding(horizontal = 14.dp, vertical = 9.dp), fontWeight = FontWeight.SemiBold)
                    DropdownMenu(typeMenuOpen, { typeMenuOpen = false }) {
                        listOf(TransactionType.EXPENSE to tr("Расход", "Expense"), TransactionType.INCOME to tr("Доход", "Income"), TransactionType.TRANSFER to tr("Перевод", "Transfer")).forEach { (type, label) ->
                            DropdownMenuItem(text = { Text(label, fontWeight = if (type == transactionType) FontWeight.SemiBold else FontWeight.Normal) }, onClick = { onTransactionTypeChange(type); typeMenuOpen = false })
                        }
                    }
                }
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                IconButton(::collapseCalculator) { Icon(Icons.Default.KeyboardArrowDown, tr("Скрыть калькулятор", "Hide calculator")) }
            }
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (transactionType == TransactionType.TRANSFER) {
                    Box(Modifier.weight(1f)) {
                        val target = accounts.firstOrNull { it.id == transferAccountId }
                        Text(target?.let { tr("На счёт: ${it.name}", "To account: ${it.name}") } ?: tr("Выбрать счёт назначения", "Choose destination account"), Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.secondaryContainer).clickable { transferAccountSheetOpen = true }.padding(12.dp))
                    }
                } else Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { categories.forEach { category -> CategoryChip(category, category.id == selectedCategory?.id, showCategoryEmoji, { onCategorySelected(category); feedback() }, { direction -> onMoveCategory(category, direction); feedback() }) } }
            }
            if (titleEditorVisible) {
                OutlinedTextField(
                    value = quickTitle,
                    onValueChange = onQuickTitleChange,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp).focusRequester(titleFocusRequester),
                    label = { Text(tr("Название операции", "Operation name")) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { closeTitleEditor() }),
                    trailingIcon = { IconButton({ closeTitleEditor() }) { Icon(Icons.Default.Close, tr("Готово", "Done")) } },
                )
            } else {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { onTitleEditorVisibleChange(true) },
                        modifier = Modifier.width(124.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    ) {
                        Text(
                            if (quickTitle.isBlank()) tr("+ Название", "+ Name") else quickTitle,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start,
                        )
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(calculator.expression.replace('.', ','), modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.End) { Text(calculator.display.replace('.', ','), modifier = Modifier.weight(1f, fill = false), fontSize = 32.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(" ₽", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp)) }
                    }
                }
            }
            if (!titleEditorVisible) {
                val rows = listOf(listOf("C", "⌫", "÷", "×"), listOf("7", "8", "9", "−"), listOf("4", "5", "6", "+"))
                rows.forEach { row -> Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp)) { row.forEach { key ->
                    CalculatorKey(key, Modifier.weight(1f)) {
                        feedback(); when (key) { "C" -> onClear(); "⌫" -> onBackspace(); "÷", "×", "+" -> onOperation(key.first()); "−" -> onOperation('-'); else -> onDigit(key.first()) }
                    }
                } } }
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
                    Column(Modifier.weight(3f)) {
                        Row(Modifier.fillMaxWidth()) {
                            listOf("1", "2", "3").forEach { key -> CalculatorKey(key, Modifier.weight(1f)) { feedback(); onDigit(key.first()) } }
                        }
                        Row(Modifier.fillMaxWidth()) {
                            CalculatorKey("0", Modifier.weight(2f)) { feedback(); onDigit('0') }
                            CalculatorKey(",", Modifier.weight(1f), fontSize = 17) { feedback(); onDecimal() }
                        }
                    }
                    val shape = RoundedCornerShape(16.dp)
                    Box(
                        Modifier.weight(1f).height(104.dp).padding(3.dp).clip(shape)
                            .background(MaterialTheme.colorScheme.primary)
                            .combinedClickable(enabled = calculator.amountCents() != null, onClick = { feedback(); onAdd() }, onLongClick = { feedback(); onChooseDay() }),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowForward, tr("Добавить операцию", "Add operation"), tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(27.dp)) }
                }
            }
        }
    }
    if (transferAccountSheetOpen) ModalBottomSheet(onDismissRequest = { transferAccountSheetOpen = false }) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Text(tr("Счёт назначения", "Destination account"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(tr("Выберите счёт, на который переводите деньги", "Choose the account receiving the transfer"), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
            accounts.filter { it.id != selectedAccount?.id }.forEach { account ->
                val selected = account.id == transferAccountId
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(18.dp)).clickable {
                        onTransferAccountSelected(account)
                        transferAccountSheetOpen = false
                    },
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape))
                        Text(account.name, Modifier.padding(start = 12.dp).weight(1f), fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatMoney(account.currentBalance(expenses)), fontWeight = FontWeight.SemiBold)
                            Text(tr("Доступно", "Available"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun CategoryChip(category: Category, selected: Boolean, showEmoji: Boolean, onClick: () -> Unit, onMove: (Int) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.clip(shape).background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .pointerInput(category.id) {
                var accumulated = 0f
                detectDragGesturesAfterLongPress(
                    onDragStart = { accumulated = 0f },
                    onDrag = { change, amount ->
                        change.consume(); accumulated += amount.x
                        if (kotlin.math.abs(accumulated) > 44f) { onMove(if (accumulated > 0) 1 else -1); accumulated = 0f }
                    },
                )
            }
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { if (showEmoji) Text(category.emoji, fontSize = 17.sp); Text(category.name, fontSize = if (showEmoji) 11.sp else 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
}
@Composable private fun CalculatorKey(label: String, modifier: Modifier, fontSize: Int = 20, onClick: () -> Unit) { val shape = RoundedCornerShape(16.dp); Box(modifier.height(52.dp).padding(3.dp).clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable(onClick = onClick), contentAlignment = Alignment.Center) { Text(label, fontSize = fontSize.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center) } }

private class ZoomablePhotoView(context: Context) : ImageView(context) {
    private val transform = Matrix()
    private var baseScale = 1f
    private var currentScale = 1f
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val next = (currentScale * detector.scaleFactor).coerceIn(baseScale, baseScale * 5f)
            val factor = next / currentScale
            transform.postScale(factor, factor, detector.focusX, detector.focusY)
            currentScale = next; imageMatrix = transform; return true
        }
    })
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean { if (currentScale > baseScale) { transform.postTranslate(-distanceX, -distanceY); imageMatrix = transform }; return true }
        override fun onDoubleTap(e: MotionEvent): Boolean { if (currentScale > baseScale * 1.1f) resetImage() else { transform.postScale(2f, 2f, e.x, e.y); currentScale *= 2f; imageMatrix = transform }; return true }
    })
    init { scaleType = ScaleType.MATRIX }
    fun show(file: File) { setImageURI(Uri.fromFile(file)); post(::resetImage) }
    private fun resetImage() {
        val image = drawable ?: return
        val sx = width.toFloat() / image.intrinsicWidth.coerceAtLeast(1); val sy = height.toFloat() / image.intrinsicHeight.coerceAtLeast(1)
        baseScale = minOf(sx, sy); currentScale = baseScale; transform.reset(); transform.postScale(baseScale, baseScale)
        transform.postTranslate((width - image.intrinsicWidth * baseScale) / 2f, (height - image.intrinsicHeight * baseScale) / 2f); imageMatrix = transform
    }
    override fun onTouchEvent(event: MotionEvent): Boolean { parent?.requestDisallowInterceptTouchEvent(true); scaleDetector.onTouchEvent(event); gestureDetector.onTouchEvent(event); if (event.action == MotionEvent.ACTION_UP) performClick(); return true }
    override fun performClick(): Boolean { super.performClick(); return true }
}
