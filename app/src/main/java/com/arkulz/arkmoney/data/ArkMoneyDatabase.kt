package com.arkulz.arkmoney.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Expense::class, Category::class, Account::class], version = 6, exportSchema = true)
abstract class ArkMoneyDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile private var instance: ArkMoneyDatabase? = null

        fun getInstance(context: Context): ArkMoneyDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ArkMoneyDatabase::class.java,
                    "arkmoney.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            seedDefaults(db)
                        }
                    })
                    .build().also { instance = it }
            }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, emoji TEXT NOT NULL, sortOrder INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS accounts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, openingBalanceCents INTEGER NOT NULL, sortOrder INTEGER NOT NULL)")
                seedDefaults(db)
                db.execSQL("ALTER TABLE expenses ADD COLUMN categoryId INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE expenses ADD COLUMN accountId INTEGER NOT NULL DEFAULT 1")
                defaultCategories.forEachIndexed { index, seed ->
                    db.execSQL("UPDATE expenses SET categoryId = ? WHERE category = ?", arrayOf<Any>(index + 1, seed.first))
                }
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN isDemo INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE accounts ADD COLUMN isDemo INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN comment TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN title TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE expenses ADD COLUMN photoPath TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN type TEXT NOT NULL DEFAULT 'EXPENSE'")
                db.execSQL("ALTER TABLE expenses ADD COLUMN transferAccountId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE categories ADD COLUMN type TEXT NOT NULL DEFAULT 'EXPENSE'")
                incomeCategories.forEach { seed ->
                    db.execSQL(
                        "INSERT INTO categories (name, emoji, sortOrder, type) VALUES (?, ?, ?, 'INCOME')",
                        arrayOf<Any>(seed.first, seed.second, seed.third),
                    )
                }
            }
        }

        private val defaultCategories = listOf(
            Triple("Другое", "•••", 11), Triple("Продукты", "🛒", 0),
            Triple("Транспорт", "🚕", 1), Triple("Кафе и доставка", "☕", 2),
            Triple("Покупки", "🛍️", 3), Triple("Дом", "🏠", 4),
            Triple("Здоровье", "💊", 5), Triple("Развлечения", "🎬", 6),
            Triple("Связь и подписки", "📱", 7), Triple("Счета и услуги", "🧾", 8),
            Triple("Подарки", "🎁", 9), Triple("Путешествия", "✈️", 10),
        )
        private val incomeCategories = listOf(
            Triple("Зарплата", "💼", 0), Triple("Дополнительный доход", "💰", 1),
            Triple("Возврат", "↩️", 2), Triple("Подарок", "🎁", 3),
            Triple("Инвестиции", "📈", 4), Triple("Другое", "•••", 5),
        )

        private fun seedDefaults(db: SupportSQLiteDatabase) {
            defaultCategories.forEachIndexed { index, seed ->
                db.execSQL(
                    "INSERT INTO categories (id, name, emoji, sortOrder, type) VALUES (?, ?, ?, ?, 'EXPENSE')",
                    arrayOf<Any>(index + 1, seed.first, seed.second, seed.third),
                )
            }
            incomeCategories.forEach { seed ->
                db.execSQL(
                    "INSERT INTO categories (name, emoji, sortOrder, type) VALUES (?, ?, ?, 'INCOME')",
                    arrayOf<Any>(seed.first, seed.second, seed.third),
                )
            }
            db.execSQL("INSERT INTO accounts (id, name, openingBalanceCents, sortOrder) VALUES (1, 'Основной', 0, 0)")
        }
    }
}
