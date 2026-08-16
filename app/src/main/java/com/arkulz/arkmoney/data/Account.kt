package com.arkulz.arkmoney.data

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val openingBalanceCents: Long = 0,
    val sortOrder: Int = 0,
    @ColumnInfo(defaultValue = "0") val isDemo: Boolean = false,
)
