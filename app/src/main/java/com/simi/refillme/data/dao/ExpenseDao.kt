package com.simi.refillme.data.dao

import androidx.room.*
import com.simi.refillme.data.entity.Expense
import com.simi.refillme.data.entity.ExpenseTask
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getExpensesByVehicle(vehicleId: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    suspend fun getExpenseById(expenseId: Long): Expense?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE vehicleId = :vehicleId")
    suspend fun deleteExpensesByVehicle(vehicleId: Long)

    @Query("SELECT COUNT(*) FROM expenses WHERE vehicleId = :vehicleId")
    suspend fun getExpenseCountByVehicle(vehicleId: Long): Int

    @Query("SELECT SUM(totalCost) FROM expenses WHERE vehicleId = :vehicleId")
    suspend fun getTotalExpenseCostByVehicle(vehicleId: Long): Double?
}

@Dao
interface ExpenseTaskDao {
    @Query("SELECT * FROM expense_tasks WHERE expenseId = :expenseId ORDER BY id ASC")
    fun getExpenseTasksByExpenseId(expenseId: Long): Flow<List<ExpenseTask>>

    @Query("SELECT * FROM expense_tasks WHERE expenseId = :expenseId ORDER BY id ASC")
    suspend fun getExpenseTasksByExpenseIdSync(expenseId: Long): List<ExpenseTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseTask(expenseTask: ExpenseTask): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseTasks(expenseTasks: List<ExpenseTask>)

    @Update
    suspend fun updateExpenseTask(expenseTask: ExpenseTask)

    @Delete
    suspend fun deleteExpenseTask(expenseTask: ExpenseTask)

    @Query("DELETE FROM expense_tasks WHERE expenseId = :expenseId")
    suspend fun deleteExpenseTasksByExpenseId(expenseId: Long)
}
