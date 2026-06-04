package com.simi.refillme.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.simi.refillme.data.api.PersonalExpenseApiService
import com.simi.refillme.data.api.toCategory
import com.simi.refillme.data.api.toExpense
import com.simi.refillme.data.api.toItem
import com.simi.refillme.data.auth.AuthManager
import com.simi.refillme.data.entity.PersonalExpense
import com.simi.refillme.data.entity.PersonalExpenseCategory
import com.simi.refillme.data.entity.PersonalExpenseItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PersonalExpenseViewModel(
    private val apiService: PersonalExpenseApiService,
    private val authManager: AuthManager
) : ViewModel() {

    private val TAG = "PersonalExpenseVM"

    private val _categories = MutableStateFlow<List<PersonalExpenseCategory>>(emptyList())
    val categories: StateFlow<List<PersonalExpenseCategory>> = _categories

    private val _expenses = MutableStateFlow<List<PersonalExpense>>(emptyList())
    val expenses: StateFlow<List<PersonalExpense>> = _expenses

    private val _allItems = MutableStateFlow<List<PersonalExpenseItem>>(emptyList())
    val allItems: StateFlow<List<PersonalExpenseItem>> = _allItems

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        viewModelScope.launch { loadData() }
    }

    /** Fetches categories and expenses from the cloud; also seeds defaults server-side. */
    fun loadData() {
        viewModelScope.launch {
            val token = authManager.getToken() ?: return@launch
            _isLoading.value = true
            try {
                apiService.getCategories(token).onSuccess { dtos ->
                    _categories.value = dtos.map { it.toCategory() }
                }.onFailure { e -> Log.e(TAG, "getCategories failed", e) }

                apiService.getExpenses(token).onSuccess { dtos ->
                    _expenses.value = dtos.map { it.toExpense() }
                    _allItems.value = dtos.flatMap { dto ->
                        (dto.items ?: emptyList()).map { it.toItem() }
                    }
                }.onFailure { e -> Log.e(TAG, "getExpenses failed", e) }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addCategory(name: String, colorHex: String) {
        viewModelScope.launch {
            val token = authManager.getToken() ?: return@launch
            apiService.createCategory(token, name, colorHex)
                .onSuccess { loadData() }
                .onFailure { e -> Log.e(TAG, "addCategory failed", e) }
        }
    }

    fun deleteCategory(category: PersonalExpenseCategory) {
        viewModelScope.launch {
            val token = authManager.getToken() ?: return@launch
            apiService.deleteCategory(token, category.id)
                .onSuccess { loadData() }
        }
    }

    fun saveExpense(
        expense: PersonalExpense,
        items: List<PersonalExpenseItem>,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val token = authManager.getToken() ?: run { _isLoading.value = false; return@launch }
            try {
                val result = if (expense.id == 0L) {
                    apiService.createExpense(token, expense, items)
                } else {
                    apiService.updateExpense(token, expense, items)
                }
                result.onSuccess {
                    loadData()
                    onSuccess()
                }.onFailure { e -> Log.e(TAG, "saveExpense failed", e) }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteExpense(expense: PersonalExpense) {
        viewModelScope.launch {
            val token = authManager.getToken() ?: return@launch
            apiService.deleteExpense(token, expense.id)
                .onSuccess { loadData() }
                .onFailure { e -> Log.e(TAG, "deleteExpense failed", e) }
        }
    }

    companion object {
        fun factory(
            apiService: PersonalExpenseApiService,
            authManager: AuthManager
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PersonalExpenseViewModel(apiService, authManager) as T
            }
    }
}
