package com.example.spendwise.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val date: Long,
    val category: String,
    val notes: String = "",
    val isIncome: Boolean = false
)
