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
import com.arkulz.arkmoney.data.TransactionType
import com.arkulz.arkmoney.data.transactionType
import java.io.File
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
                    NavigationBarItem(page == AppPage.EXPENSES, { page = AppPage.EXPENSES }, { Icon(Icons.AutoMirrored.Filled.List, null) }, label = { Text("Финансы") })
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
                    onAddExpense = { cents, category, title, createdAt, type, transferAccountId ->
                        val account = selectedAccount ?: return@ExpensesScreen
                        scope.launch { dao.insert(Expense(amountCents = cents, category = category?.name.orEmpty(), categoryId = category?.id ?: 0, accountId = account.id, title = title.ifBlank { if (type == TransactionType.TRANSFER) "Перевод" else "" }, createdAt = createdAt, type = type.name, transferAccountId = transferAccountId)) }
                    },
                    onUpdateExpense = { expense -> scope.launch { dao.updateExpense(expense) } },
                    onDeleteExpense = { expense -> scope.launch { database.withTransaction { dao.deleteExpense(expense.id) }; if (expense.photoPath.isNotBlank()) File(expense.photoPath).delete() } },
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
                    onAddCategory = { name, emoji, type -> scope.launch { dao.insertCategory(Category(name = name, emoji = emoji, sortOrder = categories.count { it.type == type.name }, type = type.name)) } },
                    onUpdateCategory = { scope.launch { dao.updateCategory(it) } },
                    onDeleteCategory = { category, replacement ->
                        scope.launch { database.withTransaction { dao.reassignCategory(category.id, replacement.id, replacement.name); dao.deleteCategory(category.id) } }
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
                            val accountId = dao.insertAccount(Account(name = "Демо-счёт", sortOrder = accounts.size, isDemo = true))
                            val demoExpenses = DemoDataGenerator.expensesForYear(categories, accountId)
                            val account = Account(
                                id = accountId,
                                name = "Демо-счёт",
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
                )
            }
        }
    }
}
