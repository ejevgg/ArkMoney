package com.arkulz.arkmoney.data

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountCents: Long,
    val category: String,
    val categoryId: Long = 1,
    val accountId: Long = 1,
    @ColumnInfo(defaultValue = "0") val isDemo: Boolean = false,
    @ColumnInfo(defaultValue = "''") val comment: String = "",
    @ColumnInfo(defaultValue = "''") val title: String = "",
    @ColumnInfo(defaultValue = "''") val photoPath: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
