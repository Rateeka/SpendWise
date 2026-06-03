package com.example.spendwise.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert
    suspend fun insertExpense(expense: Expense)

    @Query("SELECT SUM(amount) FROM expenses WHERE isIncome = 0")
    fun getTotalExpenses(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE isIncome = 1")
    fun getTotalIncome(): Flow<Double?>
}
