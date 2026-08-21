package com.arkulz.arkmoney

import android.os.Bundle
import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.room.withTransaction
import com.arkulz.arkmoney.data.Account
import com.arkulz.arkmoney.data.BackupArchive
import com.arkulz.arkmoney.data.ArkMoneyBackup
import com.arkulz.arkmoney.data.ArkMoneyDatabase
import com.arkulz.arkmoney.data.Category
import com.arkulz.arkmoney.data.DemoDataGenerator
import com.arkulz.arkmoney.data.Expense
import com.arkulz.arkmoney.data.TransactionType
import com.arkulz.arkmoney.data.transactionType
import com.arkulz.arkmoney.data.createExpensePhotoTarget
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ArkMoneyApp() }
    }
}

private enum class AppPage { EXPENSES, ANALYTICS, SETTINGS }

@Composable
private fun ArkMoneyApp() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("arkmoney_settings", Context.MODE_PRIVATE) }
    var themeMode by remember {
        mutableStateOf(ThemeMode.from(preferences.getString("theme", ThemeMode.SYSTEM.name)))
    }
    var hapticsEnabled by remember { mutableStateOf(preferences.getBoolean("haptics", true)) }
    var dailyLimitEnabled by remember { mutableStateOf(preferences.getBoolean("daily_limit_enabled", false)) }
    var dailyLimitCents by remember { mutableStateOf(preferences.getLong("daily_limit_cents", 0L)) }
    var showCategoryEmoji by remember { mutableStateOf(preferences.getBoolean("show_category_emoji", true)) }
    var appLanguage by remember { mutableStateOf(AppLanguage.from(preferences.getString("language", AppLanguage.SYSTEM.name))) }
    CompositionLocalProvider(LocalAppLanguage provides appLanguage) { ArkMoneyTheme(themeMode) {
        val database = remember { ArkMoneyDatabase.getInstance(context) }
        val dao = remember(database) { database.expenseDao() }
        val expenses by dao.observeAll().collectAsState(initial = emptyList())
        val categories by dao.observeCategories().collectAsState(initial = emptyList())
        val accounts by dao.observeAccounts().collectAsState(initial = emptyList())
        val scope = rememberCoroutineScope()
        val transferTitle = tr("Перевод", "Transfer")
        val demoAccountName = tr("Демо-счёт", "Demo account")
        val categoryDeletedText = tr("Категория удалена", "Category deleted")
        val categoryDeleteErrorText = tr("Не удалось удалить категорию", "Could not delete category")
        val categoryOperationsDeleteErrorText = tr("Не удалось удалить категорию и операции", "Could not delete category and operations")
        val backupRestoredText = tr("Резервная копия восстановлена", "Backup restored")
        val backupRestoreErrorText = tr("Не удалось восстановить данные", "Could not restore data")
        val categoryOrderMutex = remember { Mutex() }
        var page by rememberSaveable { mutableStateOf(AppPage.EXPENSES) }
        var selectedAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
        val selectedAccount = accounts.firstOrNull { it.id == selectedAccountId }
            ?: accounts.firstOrNull().also { selectedAccountId = it?.id }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(page == AppPage.EXPENSES, { page = AppPage.EXPENSES }, { Icon(Icons.AutoMirrored.Filled.List, null) }, label = { Text(tr("Финансы", "Finances")) })
                    NavigationBarItem(page == AppPage.ANALYTICS, { page = AppPage.ANALYTICS }, { Icon(Icons.Default.Star, null) }, label = { Text(tr("Аналитика", "Analytics")) })
                    NavigationBarItem(page == AppPage.SETTINGS, { page = AppPage.SETTINGS }, { Icon(Icons.Default.Settings, null) }, label = { Text(tr("Настройки", "Settings")) })
                }
            },
        ) { padding ->
            when (page) {
                AppPage.EXPENSES -> ExpensesScreen(
                    modifier = Modifier.padding(padding),
                    expenses = expenses,
                    categories = categories,
                    accounts = accounts,
                    selectedAccount = selectedAccount,
                    onAccountSelected = { selectedAccountId = it.id },
                    onAddExpense = { cents, category, title, createdAt, type, transferAccountId, completed ->
                        val account = selectedAccount ?: return@ExpensesScreen
                        scope.launch { completed(dao.insert(Expense(amountCents = cents, category = category?.name.orEmpty(), categoryId = category?.id ?: 0, accountId = account.id, title = title.ifBlank { if (type == TransactionType.TRANSFER) transferTitle else "" }, createdAt = createdAt, type = type.name, transferAccountId = transferAccountId))) }
                    },
                    onUndoExpense = { id -> scope.launch { dao.deleteExpense(id) } },
                    onUpdateExpense = { expense -> scope.launch { dao.updateExpense(expense) } },
                    onDeleteExpense = { expense -> scope.launch { database.withTransaction { dao.deleteExpense(expense.id) }; if (expense.photoPath.isNotBlank()) File(expense.photoPath).delete() } },
                    onMoveCategory = { category, direction -> scope.launch {
                        categoryOrderMutex.withLock { dao.updateCategories(reorderedCategoryType(dao.categoriesNow(), category.id, direction)) }
                    } },
                    hapticsEnabled = hapticsEnabled,
                    showCategoryEmoji = showCategoryEmoji,
                    dailyLimitCents = dailyLimitCents.takeIf { dailyLimitEnabled },
                )
                AppPage.ANALYTICS -> AnalyticsScreen(Modifier.padding(padding), expenses, categories)
                AppPage.SETTINGS -> SettingsScreen(
                    modifier = Modifier.padding(padding),
                    expenses = expenses,
                    categories = categories,
                    accounts = accounts,
                    themeMode = themeMode,
                    appLanguage = appLanguage,
                    onAppLanguageChange = { appLanguage = it; preferences.edit { putString("language", it.name) } },
                    onThemeModeChange = {
                        themeMode = it
                        preferences.edit { putString("theme", it.name) }
                    },
                    onAddCategory = { name, emoji, type -> scope.launch { dao.insertCategory(Category(name = name, emoji = emoji, sortOrder = categories.count { it.type == type.name }, type = type.name)) } },
                    onUpdateCategory = { scope.launch { dao.updateCategory(it) } },
                    onDeleteCategory = { category, replacement, completed ->
                        scope.launch {
                            categoryOrderMutex.withLock {
                                runCatching { dao.reassignAndDeleteCategory(category, replacement) }
                                    .onSuccess {
                                        completed(true)
                                        Toast.makeText(context, categoryDeletedText, Toast.LENGTH_SHORT).show()
                                    }
                                    .onFailure {
                                        completed(false)
                                        Toast.makeText(context, "$categoryDeleteErrorText: ${it.message.orEmpty()}", Toast.LENGTH_LONG).show()
                                    }
                            }
                        }
                    },
                    onDeleteCategoryWithExpenses = { category, completed ->
                        scope.launch {
                            categoryOrderMutex.withLock {
                                runCatching { dao.deleteCategoryAndExpenses(category) }
                                    .onSuccess { paths -> paths.forEach { File(it).delete() }; completed(true) }
                                    .onFailure { completed(false); Toast.makeText(context, categoryOperationsDeleteErrorText, Toast.LENGTH_LONG).show() }
                            }
                        }
                    },
                    onAddAccount = { name, balance -> scope.launch { dao.insertAccount(Account(name = name, openingBalanceCents = balance, sortOrder = accounts.size)) } },
                    onUpdateAccount = { scope.launch { dao.updateAccount(it) } },
                    onDeleteAccount = { account ->
                        val replacement = accounts.firstOrNull { it.id != account.id } ?: return@SettingsScreen
                        if (selectedAccountId == account.id) selectedAccountId = replacement.id
                        scope.launch {
                            val attached = dao.expensesForAccount(account.id)
                            database.withTransaction { dao.deleteExpensesForAccount(account.id); dao.deleteAccount(account.id) }
                            attached.map { it.photoPath }.filter { it.isNotBlank() }.forEach { File(it).delete() }
                        }
                    },
                    onGenerateDemoData = {
                        scope.launch { database.withTransaction {
                            dao.deleteDemoExpenses()
                            dao.deleteDemoAccounts()
                            val accountId = dao.insertAccount(Account(name = demoAccountName, sortOrder = accounts.size, isDemo = true))
                            val demoExpenses = DemoDataGenerator.expensesForYear(categories, accountId)
                            val account = Account(
                                id = accountId,
                                name = demoAccountName,
                                openingBalanceCents = demoExpenses.sumOf { if (it.transactionType == TransactionType.EXPENSE) it.amountCents else -it.amountCents } + 250_000_00L,
                                sortOrder = accounts.size,
                                isDemo = true,
                            )
                            dao.updateAccount(account)
                            dao.insertAll(demoExpenses)
                            selectedAccountId = accountId
                        } }
                    },
                    onDeleteDemoData = {
                        scope.launch { database.withTransaction { dao.deleteDemoExpenses(); dao.deleteDemoAccounts() } }
                    },
                    hapticsEnabled = hapticsEnabled,
                    onHapticsEnabledChange = { hapticsEnabled = it; preferences.edit { putBoolean("haptics", it) } },
                    showCategoryEmoji = showCategoryEmoji,
                    onShowCategoryEmojiChange = { showCategoryEmoji = it; preferences.edit { putBoolean("show_category_emoji", it) } },
                    dailyLimitEnabled = dailyLimitEnabled,
                    dailyLimitCents = dailyLimitCents,
                    onDailyLimitChange = { enabled, cents ->
                        dailyLimitEnabled = enabled; dailyLimitCents = cents
                        preferences.edit { putBoolean("daily_limit_enabled", enabled); putLong("daily_limit_cents", cents) }
                    },
                    onCategoryOrderChanged = { ordered -> scope.launch {
                        categoryOrderMutex.withLock { dao.updateCategories(ordered) }
                    } },
                    onRestoreBackup = { archive -> scope.launch {
                        val createdPaths = mutableListOf<String>()
                        runCatching {
                            val oldPhotos = expenses.mapNotNull { it.photoPath.takeIf(String::isNotBlank) }
                            val restored = withContext(Dispatchers.IO) {
                                File(context.filesDir, "last_before_restore.arkmoney").outputStream().use { ArkMoneyBackup.write(it, accounts, categories, expenses) }
                                archive.expenses.map { operation ->
                                    val path = archive.photos[operation.id]?.let { bytes -> createExpensePhotoTarget(context).apply { writeBytes(bytes); com.arkulz.arkmoney.data.compressExpensePhoto(this); createdPaths += absolutePath }.absolutePath }.orEmpty()
                                    operation.copy(photoPath = path)
                                }
                            }
                            database.withTransaction {
                                dao.deleteAllExpenses(); dao.deleteAllCategories(); dao.deleteAllAccounts()
                                dao.insertAccounts(archive.accounts); dao.insertCategories(archive.categories); dao.insertAll(restored)
                            }
                            oldPhotos.forEach { File(it).delete() }
                            selectedAccountId = archive.accounts.firstOrNull()?.id
                        }.onSuccess { Toast.makeText(context, backupRestoredText, Toast.LENGTH_SHORT).show() }
                            .onFailure { createdPaths.forEach { File(it).delete() }; Toast.makeText(context, backupRestoreErrorText, Toast.LENGTH_LONG).show() }
                    } },
                )
            }
        }
    } }
}
