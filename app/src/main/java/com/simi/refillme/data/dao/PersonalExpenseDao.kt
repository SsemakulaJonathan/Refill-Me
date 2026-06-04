package com.simi.refillme.data.dao

import androidx.room.*
import com.simi.refillme.data.entity.PersonalExpense
import com.simi.refillme.data.entity.PersonalExpenseCategory
import com.simi.refillme.data.entity.PersonalExpenseItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalExpenseCategoryDao {
    @Query("SELECT * FROM personal_expense_categories ORDER BY isDefault DESC, name ASC")
    fun getAllCategories(): Flow<List<PersonalExpenseCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: PersonalExpenseCategory): Long

    @Delete
    suspend fun deleteCategory(category: PersonalExpenseCategory)

    @Query("SELECT COUNT(*) FROM personal_expense_categories")
    suspend fun getCategoryCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<PersonalExpenseCategory>)
}

@Dao
interface PersonalExpenseDao {
    @Query("SELECT * FROM personal_expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<PersonalExpense>>

    @Query("SELECT * FROM personal_expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): PersonalExpense?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: PersonalExpense): Long

    @Update
    suspend fun updateExpense(expense: PersonalExpense)

    @Delete
    suspend fun deleteExpense(expense: PersonalExpense)
}

@Dao
interface PersonalExpenseItemDao {
    @Query("SELECT * FROM personal_expense_items WHERE expenseId = :expenseId ORDER BY id ASC")
    suspend fun getItemsByExpenseId(expenseId: Long): List<PersonalExpenseItem>

    @Query("SELECT * FROM personal_expense_items ORDER BY expenseId DESC, id ASC")
    fun getAllItems(): Flow<List<PersonalExpenseItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<PersonalExpenseItem>)

    @Query("DELETE FROM personal_expense_items WHERE expenseId = :expenseId")
    suspend fun deleteItemsByExpenseId(expenseId: Long)
}
