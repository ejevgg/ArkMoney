package com.arkulz.arkmoney

import android.os.Bundle
import android.content.Context
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.room.withTransaction
import com.arkulz.arkmoney.data.Account
import com.arkulz.arkmoney.data.ArkMoneyDatabase
import com.arkulz.arkmoney.data.Category
import com.arkulz.arkmoney.data.DemoDataGenerator
import com.arkulz.arkmoney.data.Expense
import com.arkulz.arkmoney.data.ImportedWorkbook
import kotlinx.coroutines.launch

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
    ArkMoneyTheme(themeMode) {
        val database = remember { ArkMoneyDatabase.getInstance(context) }
        val dao = remember(database) { database.expenseDao() }
        val expenses by dao.observeAll().collectAsState(initial = emptyList())
        val categories by dao.observeCategories().collectAsState(initial = emptyList())
        val accounts by dao.observeAccounts().collectAsState(initial = emptyList())
        val scope = rememberCoroutineScope()
        var page by rememberSaveable { mutableStateOf(AppPage.EXPENSES) }
        var selectedAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
        val selectedAccount = accounts.firstOrNull { it.id == selectedAccountId }
            ?: accounts.firstOrNull().also { selectedAccountId = it?.id }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(page == AppPage.EXPENSES, { page = AppPage.EXPENSES }, { Icon(Icons.AutoMirrored.Filled.List, null) }, label = { Text("Расходы") })
                    NavigationBarItem(page == AppPage.ANALYTICS, { page = AppPage.ANALYTICS }, { Icon(Icons.Default.Star, null) }, label = { Text("Аналитика") })
                    NavigationBarItem(page == AppPage.SETTINGS, { page = AppPage.SETTINGS }, { Icon(Icons.Default.Settings, null) }, label = { Text("Настройки") })
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
                    onAddExpense = { cents, category, title, createdAt ->
                        val account = selectedAccount ?: return@ExpensesScreen
                        scope.launch { dao.insert(Expense(amountCents = cents, category = category.name, categoryId = category.id, accountId = account.id, title = title, createdAt = createdAt)) }
                    },
                    onUpdateExpense = { expense -> scope.launch { dao.updateExpense(expense) } },
                )
                AppPage.ANALYTICS -> AnalyticsScreen(Modifier.padding(padding), expenses, categories)
                AppPage.SETTINGS -> SettingsScreen(
                    modifier = Modifier.padding(padding),
                    expenses = expenses,
                    categories = categories,
                    accounts = accounts,
                    themeMode = themeMode,
                    onThemeModeChange = {
                        themeMode = it
                        preferences.edit { putString("theme", it.name) }
                    },
                    onAddCategory = { name, emoji -> scope.launch { dao.insertCategory(Category(name = name, emoji = emoji, sortOrder = categories.size)) } },
                    onUpdateCategory = { scope.launch { dao.updateCategory(it) } },
                    onDeleteCategory = { category ->
                        val replacement = categories.firstOrNull { it.id != category.id } ?: return@SettingsScreen
                        scope.launch { database.withTransaction { dao.reassignCategory(category.id, replacement.id); dao.deleteCategory(category.id) } }
                    },
                    onAddAccount = { name, balance -> scope.launch { dao.insertAccount(Account(name = name, openingBalanceCents = balance, sortOrder = accounts.size)) } },
                    onUpdateAccount = { scope.launch { dao.updateAccount(it) } },
                    onDeleteAccount = { account ->
                        val replacement = accounts.firstOrNull { it.id != account.id } ?: return@SettingsScreen
                        if (selectedAccountId == account.id) selectedAccountId = replacement.id
                        scope.launch { database.withTransaction { dao.reassignAccount(account.id, replacement.id); dao.deleteAccount(account.id) } }
                    },
                    onGenerateDemoData = {
                        scope.launch { database.withTransaction {
                            dao.deleteDemoExpenses()
                            dao.deleteDemoAccounts()
                            val accountId = dao.insertAccount(Account(name = "Демо-счёт", sortOrder = accounts.size, isDemo = true))
                            val demoExpenses = DemoDataGenerator.expensesForYear(categories, accountId)
                            val account = Account(
                                id = accountId,
                                name = "Демо-счёт",
                                openingBalanceCents = demoExpenses.sumOf { it.amountCents } + 250_000_00L,
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
                    onImportWorkbook = { workbook ->
                        scope.launch { database.withTransaction { importWorkbook(workbook, dao, categories, accounts) } }
                    },
                )
            }
        }
    }
}

private suspend fun importWorkbook(
    workbook: ImportedWorkbook,
    dao: com.arkulz.arkmoney.data.ExpenseDao,
    currentCategories: List<Category>,
    currentAccounts: List<Account>,
) {
    val categoryIds = currentCategories.associate { it.name.lowercase() to it.id }.toMutableMap()
    val accountIds = currentAccounts.associate { it.name.lowercase() to it.id }.toMutableMap()
    workbook.accounts.forEachIndexed { index, imported ->
        val key = imported.name.lowercase()
        val existing = currentAccounts.firstOrNull { it.name.equals(imported.name, ignoreCase = true) }
        if (existing != null) {
            dao.updateAccount(existing.copy(openingBalanceCents = imported.openingBalanceCents))
        } else {
            accountIds[key] = dao.insertAccount(Account(name = imported.name, openingBalanceCents = imported.openingBalanceCents, sortOrder = currentAccounts.size + index))
        }
    }
    val importedExpenses = workbook.expenses.map { imported ->
        val categoryKey = imported.category.lowercase()
        val categoryId = categoryIds[categoryKey] ?: dao.insertCategory(Category(name = imported.category, emoji = "📁", sortOrder = categoryIds.size)).also { categoryIds[categoryKey] = it }
        val accountKey = imported.account.lowercase()
        val accountId = accountIds[accountKey] ?: dao.insertAccount(Account(name = imported.account, sortOrder = accountIds.size)).also { accountIds[accountKey] = it }
        Expense(amountCents = imported.amountCents, category = imported.category, categoryId = categoryId, accountId = accountId, title = imported.title, comment = imported.comment, createdAt = imported.createdAt)
    }
    dao.insertAll(importedExpenses)
}
