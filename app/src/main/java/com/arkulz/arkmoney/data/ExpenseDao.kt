package com.arkulz.arkmoney.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<Expense>>

    @Insert
    suspend fun insert(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun expenseById(id: Long): Expense?

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: Long)

    @Query("SELECT * FROM expenses WHERE accountId = :accountId OR transferAccountId = :accountId")
    suspend fun expensesForAccount(accountId: Long): List<Expense>

    @Query("DELETE FROM expenses WHERE accountId = :accountId OR transferAccountId = :accountId")
    suspend fun deleteExpensesForAccount(accountId: Long)

    @Query("SELECT COUNT(*) FROM expenses WHERE categoryId = :categoryId")
    suspend fun countExpensesForCategory(categoryId: Long): Int

    @Query("SELECT * FROM expenses WHERE categoryId = :categoryId")
    suspend fun expensesForCategory(categoryId: Long): List<Expense>

    @Query("DELETE FROM expenses WHERE categoryId = :categoryId")
    suspend fun deleteExpensesForCategory(categoryId: Long)

    @Insert
    suspend fun insertAll(expenses: List<Expense>)

    @Insert
    suspend fun insertCategories(categories: List<Category>)

    @Insert
    suspend fun insertAccounts(accounts: List<Account>)

    @Query("DELETE FROM expenses") suspend fun deleteAllExpenses()
    @Query("DELETE FROM categories") suspend fun deleteAllCategories()
    @Query("DELETE FROM accounts") suspend fun deleteAllAccounts()

    @Query("DELETE FROM expenses WHERE isDemo = 1")
    suspend fun deleteDemoExpenses()

    @Query("DELETE FROM accounts WHERE isDemo = 1")
    suspend fun deleteDemoAccounts()

    @Query("SELECT * FROM categories ORDER BY sortOrder, id")
    fun observeCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY sortOrder, id")
    suspend fun categoriesNow(): List<Category>

    @Insert suspend fun insertCategory(category: Category): Long
    @Update suspend fun updateCategory(category: Category)
    @Update suspend fun updateCategories(categories: List<Category>)
    @Query("UPDATE expenses SET categoryId = :replacementId, category = :replacementName WHERE categoryId = :categoryId")
    suspend fun reassignCategory(categoryId: Long, replacementId: Long, replacementName: String)
    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategory(categoryId: Long): Int

    @Transaction
    suspend fun reassignAndDeleteCategory(category: Category, replacement: Category) {
        require(category.id != replacement.id && category.type == replacement.type)
        reassignCategory(category.id, replacement.id, replacement.name)
        check(deleteCategory(category.id) == 1) { "Category ${category.id} was not deleted" }
        val normalized = categoriesNow().filter { it.type == category.type }
            .sortedWith(compareBy<Category> { it.sortOrder }.thenBy { it.id })
            .mapIndexed { index, item -> item.copy(sortOrder = index) }
        updateCategories(normalized)
    }

    @Transaction
    suspend fun deleteCategoryAndExpenses(category: Category): List<String> {
        val photoPaths = expensesForCategory(category.id).mapNotNull { it.photoPath.takeIf(String::isNotBlank) }
        deleteExpensesForCategory(category.id)
        check(deleteCategory(category.id) == 1) { "Category ${category.id} was not deleted" }
        return photoPaths
    }

    @Query("SELECT * FROM accounts ORDER BY sortOrder, id")
    fun observeAccounts(): Flow<List<Account>>

    @Insert suspend fun insertAccount(account: Account): Long
    @Update suspend fun updateAccount(account: Account)
    @Query("DELETE FROM accounts WHERE id = :accountId")
    suspend fun deleteAccount(accountId: Long)
}
