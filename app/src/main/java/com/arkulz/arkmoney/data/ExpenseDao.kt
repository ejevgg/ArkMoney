package com.arkulz.arkmoney.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<Expense>>

    @Insert
    suspend fun insert(expense: Expense)

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

    @Insert
    suspend fun insertAll(expenses: List<Expense>)

    @Query("DELETE FROM expenses WHERE isDemo = 1")
    suspend fun deleteDemoExpenses()

    @Query("DELETE FROM accounts WHERE isDemo = 1")
    suspend fun deleteDemoAccounts()

    @Query("SELECT * FROM categories ORDER BY sortOrder, id")
    fun observeCategories(): Flow<List<Category>>

    @Insert suspend fun insertCategory(category: Category): Long
    @Update suspend fun updateCategory(category: Category)
    @Query("UPDATE expenses SET categoryId = :replacementId, category = :replacementName WHERE categoryId = :categoryId")
    suspend fun reassignCategory(categoryId: Long, replacementId: Long, replacementName: String)
    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategory(categoryId: Long)

    @Query("SELECT * FROM accounts ORDER BY sortOrder, id")
    fun observeAccounts(): Flow<List<Account>>

    @Insert suspend fun insertAccount(account: Account): Long
    @Update suspend fun updateAccount(account: Account)
    @Query("DELETE FROM accounts WHERE id = :accountId")
    suspend fun deleteAccount(accountId: Long)
}
