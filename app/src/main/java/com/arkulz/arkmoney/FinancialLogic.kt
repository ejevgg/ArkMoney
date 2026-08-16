package com.arkulz.arkmoney

import com.arkulz.arkmoney.data.Account
import com.arkulz.arkmoney.data.Expense

fun Account.currentBalance(expenses: List<Expense>): Long =
    openingBalanceCents - expenses.asSequence().filter { it.accountId == id }.sumOf { it.amountCents }

fun nextVersionTap(current: Int): Pair<Int, Boolean> =
    if (current >= 9) 0 to true else (current + 1) to false
