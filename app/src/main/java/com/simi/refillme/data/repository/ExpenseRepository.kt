package com.simi.refillme.data.repository

import com.simi.refillme.data.dao.ExpenseDao
import com.simi.refillme.data.dao.ExpenseTaskDao
import com.simi.refillme.data.entity.Expense
import com.simi.refillme.data.entity.ExpenseTask
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val expenseTaskDao: ExpenseTaskDao
) {

    fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()

    fun getExpensesByVehicle(vehicleId: Long): Flow<List<Expense>> = expenseDao.getExpensesByVehicle(vehicleId)

    suspend fun getExpenseById(expenseId: Long): Expense? = expenseDao.getExpenseById(expenseId)

    suspend fun insertExpense(expense: Expense): Long = expenseDao.insertExpense(expense)

    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)

    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    suspend fun getExpenseCountByVehicle(vehicleId: Long): Int = expenseDao.getExpenseCountByVehicle(vehicleId)

    suspend fun getTotalExpenseCostByVehicle(vehicleId: Long): Double = expenseDao.getTotalExpenseCostByVehicle(vehicleId) ?: 0.0

    // Expense Tasks
    fun getExpenseTasksByExpenseId(expenseId: Long): Flow<List<ExpenseTask>> = expenseTaskDao.getExpenseTasksByExpenseId(expenseId)

    suspend fun getExpenseTasksByExpenseIdSync(expenseId: Long): List<ExpenseTask> = expenseTaskDao.getExpenseTasksByExpenseIdSync(expenseId)

    suspend fun insertExpenseTask(expenseTask: ExpenseTask): Long = expenseTaskDao.insertExpenseTask(expenseTask)

    suspend fun insertExpenseTasks(expenseTasks: List<ExpenseTask>) = expenseTaskDao.insertExpenseTasks(expenseTasks)

    suspend fun updateExpenseTask(expenseTask: ExpenseTask) = expenseTaskDao.updateExpenseTask(expenseTask)

    suspend fun deleteExpenseTask(expenseTask: ExpenseTask) = expenseTaskDao.deleteExpenseTask(expenseTask)

    suspend fun deleteExpenseTasksByExpenseId(expenseId: Long) = expenseTaskDao.deleteExpenseTasksByExpenseId(expenseId)
}
