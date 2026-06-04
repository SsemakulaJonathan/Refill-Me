package com.simi.refillme.data.repository

import com.simi.refillme.data.database.RefillMeDatabase
import com.simi.refillme.data.entity.PersonalExpense
import com.simi.refillme.data.entity.PersonalExpenseCategory
import com.simi.refillme.data.entity.PersonalExpenseItem
import kotlinx.coroutines.flow.Flow

class PersonalExpenseRepository(private val database: RefillMeDatabase) {

    private val categoryDao = database.personalExpenseCategoryDao()
    private val expenseDao   = database.personalExpenseDao()
    private val itemDao      = database.personalExpenseItemDao()

    val categories: Flow<List<PersonalExpenseCategory>> = categoryDao.getAllCategories()
    val expenses:   Flow<List<PersonalExpense>>         = expenseDao.getAllExpenses()
    val allItems:   Flow<List<PersonalExpenseItem>>     = itemDao.getAllItems()

    /** Seeds a set of default categories on first launch. */
    suspend fun seedDefaultCategories() {
        if (categoryDao.getCategoryCount() > 0) return
        val defaults = listOf(
            PersonalExpenseCategory(name = "Groceries",       colorHex = "#4CAF50", isDefault = true),
            PersonalExpenseCategory(name = "Food & Dining",   colorHex = "#FF9800", isDefault = true),
            PersonalExpenseCategory(name = "Housing & Rent",  colorHex = "#2196F3", isDefault = true),
            PersonalExpenseCategory(name = "Utilities",       colorHex = "#9C27B0", isDefault = true),
            PersonalExpenseCategory(name = "Health",          colorHex = "#F44336", isDefault = true),
            PersonalExpenseCategory(name = "Entertainment",   colorHex = "#E91E63", isDefault = true),
            PersonalExpenseCategory(name = "Travel",          colorHex = "#00BCD4", isDefault = true),
            PersonalExpenseCategory(name = "Clothing",        colorHex = "#FF5722", isDefault = true),
            PersonalExpenseCategory(name = "Education",       colorHex = "#607D8B", isDefault = true),
            PersonalExpenseCategory(name = "Other",           colorHex = "#9E9E9E", isDefault = true),
        )
        categoryDao.insertCategories(defaults)
    }

    suspend fun addCategory(name: String, colorHex: String): Long =
        categoryDao.insertCategory(PersonalExpenseCategory(name = name, colorHex = colorHex))

    suspend fun deleteCategory(category: PersonalExpenseCategory) =
        categoryDao.deleteCategory(category)

    /** Inserts a new expense + items, or replaces an existing one. Returns the saved expense id. */
    suspend fun saveExpense(expense: PersonalExpense, items: List<PersonalExpenseItem>): Long {
        return if (expense.id == 0L) {
            val newId = expenseDao.insertExpense(expense)
            itemDao.insertItems(items.map { it.copy(expenseId = newId) })
            newId
        } else {
            expenseDao.updateExpense(expense)
            itemDao.deleteItemsByExpenseId(expense.id)
            itemDao.insertItems(items.map { it.copy(expenseId = expense.id) })
            expense.id
        }
    }

    suspend fun deleteExpense(expense: PersonalExpense) = expenseDao.deleteExpense(expense)

    suspend fun getItemsForExpense(expenseId: Long): List<PersonalExpenseItem> =
        itemDao.getItemsByExpenseId(expenseId)
}
