package com.example.spendwise.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey val monthYear: String, // e.g., "02-2025"
    val amount: Double
)
