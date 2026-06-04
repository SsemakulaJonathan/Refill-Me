package com.simi.refillme.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simi.refillme.data.api.ExpenseApiService
import com.simi.refillme.data.api.toExpense
import com.simi.refillme.data.api.toExpenseTask
import com.simi.refillme.data.auth.AuthManager
import com.simi.refillme.data.entity.Expense
import com.simi.refillme.data.entity.ExpenseTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ExpenseViewModel(
    private val expenseApiService: ExpenseApiService,
    private val authManager: AuthManager
) : ViewModel() {

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadExpenses(vehicleId: Long? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val token = authManager.getToken()
                if (token != null) {
                    val result = expenseApiService.getExpenses(token, vehicleId)
                    result.onSuccess { expenseDtos ->
                        _expenses.value = expenseDtos.map { it.toExpense() }
                    }.onFailure { e ->
                        _error.value = e.message
                        Log.e("ExpenseViewModel", "Failed to load expenses", e)
                    }
                } else {
                    _error.value = "Not authenticated"
                }
            } catch (e: Exception) {
                _error.value = e.message
                Log.e("ExpenseViewModel", "Error loading expenses", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addExpense(
        vehicleId: Long,
        date: Long,
        odometerReading: Int,
        vendor: String,
        expenseTasks: List<Pair<String, Double>>,
        totalCost: Double,
        notes: String,
        receiptUri: Uri?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val token = authManager.getToken()
                val user = authManager.currentUser.value

                if (token != null && user != null) {
                    // Save receipt image path if provided
                    val receiptPath = receiptUri?.toString()

                    // Create expense record
                    val expense = Expense(
                        vehicleId = vehicleId,
                        date = date,
                        odometerReading = odometerReading,
                        vendor = vendor,
                        totalCost = totalCost,
                        notes = notes.ifBlank { null },
                        receiptImagePath = receiptPath
                    )

                    // Convert user ID from String to Int
                    val userId = user.id.toIntOrNull() ?: 0

                    // Call backend API
                    val result = expenseApiService.createExpense(token, userId, expense, expenseTasks)
                    result.onSuccess { expenseDto ->
                        Log.d("ExpenseViewModel", "Expense created successfully: $expenseDto")
                        loadExpenses(vehicleId) // Reload expenses
                        onSuccess()
                    }.onFailure { e ->
                        _error.value = e.message
                        onError(e.message ?: "Unknown error occurred")
                        Log.e("ExpenseViewModel", "Failed to create expense", e)
                    }
                } else {
                    val errorMsg = "Not authenticated"
                    _error.value = errorMsg
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                _error.value = e.message
                onError(e.message ?: "Unknown error occurred")
                Log.e("ExpenseViewModel", "Error creating expense", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateExpense(
        expenseId: Long,
        vehicleId: Long,
        date: Long,
        odometerReading: Int,
        vendor: String,
        expenseTasks: List<Pair<String, Double>>,
        totalCost: Double,
        notes: String,
        receiptUri: Uri?,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val token = authManager.getToken()
                val user = authManager.currentUser.value

                if (token != null && user != null) {
                    val receiptPath = receiptUri?.toString()

                    val expense = Expense(
                        id = expenseId,
                        vehicleId = vehicleId,
                        date = date,
                        odometerReading = odometerReading,
                        vendor = vendor,
                        totalCost = totalCost,
                        notes = notes.ifBlank { null },
                        receiptImagePath = receiptPath
                    )

                    // Convert user ID from String to Int
                    val userId = user.id.toIntOrNull() ?: 0

                    val result = expenseApiService.updateExpense(token, userId, expenseId, expense, expenseTasks)
                    result.onSuccess { expenseDto ->
                        Log.d("ExpenseViewModel", "Expense updated successfully: $expenseDto")
                        loadExpenses(vehicleId)
                        onSuccess()
                    }.onFailure { e ->
                        _error.value = e.message
                        onError(e.message ?: "Unknown error occurred")
                        Log.e("ExpenseViewModel", "Failed to update expense", e)
                    }
                } else {
                    val errorMsg = "Not authenticated"
                    _error.value = errorMsg
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                _error.value = e.message
                onError(e.message ?: "Unknown error occurred")
                Log.e("ExpenseViewModel", "Error updating expense", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteExpense(expenseId: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val token = authManager.getToken()
                if (token != null) {
                    val result = expenseApiService.deleteExpense(token, expenseId)
                    result.onSuccess { response ->
                        Log.d("ExpenseViewModel", "Expense deleted successfully")
                        loadExpenses() // Reload expenses
                        onComplete()
                    }.onFailure { e ->
                        _error.value = e.message
                        Log.e("ExpenseViewModel", "Failed to delete expense", e)
                    }
                } else {
                    _error.value = "Not authenticated"
                }
            } catch (e: Exception) {
                _error.value = e.message
                Log.e("ExpenseViewModel", "Error deleting expense", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
