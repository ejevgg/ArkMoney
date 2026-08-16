package com.arkulz.arkmoney

import com.arkulz.arkmoney.data.Account
import com.arkulz.arkmoney.data.Expense
import com.arkulz.arkmoney.data.TransactionType
import com.arkulz.arkmoney.data.transactionType

fun Account.currentBalance(expenses: List<Expense>): Long = openingBalanceCents + expenses.sumOf { transaction ->
    when (transaction.transactionType) {
        TransactionType.EXPENSE -> if (transaction.accountId == id) -transaction.amountCents else 0L
        TransactionType.INCOME -> if (transaction.accountId == id) transaction.amountCents else 0L
        TransactionType.TRANSFER -> when (id) {
            transaction.accountId -> -transaction.amountCents
            transaction.transferAccountId -> transaction.amountCents
            else -> 0L
        }
    }
}

fun nextVersionTap(current: Int): Pair<Int, Boolean> =
    if (current >= 9) 0 to true else (current + 1) to false
