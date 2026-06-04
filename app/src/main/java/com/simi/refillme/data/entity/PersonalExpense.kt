package com.simi.refillme.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "personal_expense_categories")
data class PersonalExpenseCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#9E9E9E",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "personal_expenses")
data class PersonalExpense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val vendor: String,
    val categoryId: Long? = null,
    val totalCost: Double,
    val notes: String? = null,
    val receiptImagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "personal_expense_items",
    foreignKeys = [
        ForeignKey(
            entity = PersonalExpense::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("expenseId")]
)
data class PersonalExpenseItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expenseId: Long = 0,
    val name: String,
    val categoryId: Long? = null,
    val cost: Double
)
