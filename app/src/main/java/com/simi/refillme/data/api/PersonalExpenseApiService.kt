package com.simi.refillme.data.api

import android.util.Log
import com.simi.refillme.data.entity.PersonalExpense
import com.simi.refillme.data.entity.PersonalExpenseCategory
import com.simi.refillme.data.entity.PersonalExpenseItem
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ── DTOs ──────────────────────────────────────────────────────────────────────

@Serializable
data class PersonalExpenseCategoryDto(
    val id: Int,
    val user_id: Int,
    val name: String,
    val color_hex: String,
    val is_default: Boolean,
    val created_at: String
)

@Serializable
data class PersonalExpenseItemDto(
    val id: Int,
    val expense_id: Int,
    val name: String,
    val category_id: Int?,
    val cost: Double
)

@Serializable
data class PersonalExpenseDto(
    val id: Int,
    val user_id: Int,
    val date: String,
    val vendor: String,
    val category_id: Int?,
    val total_cost: Double,
    val notes: String?,
    val receipt_image_path: String?,
    val created_at: String,
    val items: List<PersonalExpenseItemDto>? = null
)

// ── Mappers ───────────────────────────────────────────────────────────────────

fun PersonalExpenseCategoryDto.toCategory() = PersonalExpenseCategory(
    id        = this.id.toLong(),
    name      = this.name,
    colorHex  = this.color_hex,
    isDefault = this.is_default
)

fun PersonalExpenseDto.toExpense() = PersonalExpense(
    id                = this.id.toLong(),
    date              = try { java.time.Instant.parse(this.date).toEpochMilli() } catch (_: Exception) { 0L },
    vendor            = this.vendor,
    categoryId        = this.category_id?.toLong(),
    totalCost         = this.total_cost,
    notes             = this.notes,
    receiptImagePath  = this.receipt_image_path
)

fun PersonalExpenseItemDto.toItem() = PersonalExpenseItem(
    id         = this.id.toLong(),
    expenseId  = this.expense_id.toLong(),
    name       = this.name,
    categoryId = this.category_id?.toLong(),
    cost       = this.cost
)

// ── Request bodies ────────────────────────────────────────────────────────────

@Serializable
data class CreateCategoryRequest(
    val name: String,
    val color_hex: String
)

@Serializable
data class PersonalExpenseItemRequest(
    val name: String,
    val category_id: Long?,
    val cost: Double
)

@Serializable
data class CreatePersonalExpenseRequest(
    val date: String,
    val vendor: String,
    val category_id: Long?,
    val total_cost: Double,
    val notes: String?,
    val receipt_image_path: String?,
    val items: List<PersonalExpenseItemRequest>
)

@Serializable
data class UpdatePersonalExpenseRequest(
    val id: Long,
    val date: String,
    val vendor: String,
    val category_id: Long?,
    val total_cost: Double,
    val notes: String?,
    val receipt_image_path: String?,
    val items: List<PersonalExpenseItemRequest>
)

// ── Service ───────────────────────────────────────────────────────────────────

class PersonalExpenseApiService {

    private val baseUrl = "https://refill-me.vercel.app/api"

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) { level = LogLevel.BODY }
    }

    companion object {
        @Volatile private var instance: PersonalExpenseApiService? = null
        fun getInstance() = instance ?: synchronized(this) {
            instance ?: PersonalExpenseApiService().also { instance = it }
        }
    }

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    // ── Categories ────────────────────────────────────────────────────────────

    suspend fun getCategories(token: String): Result<List<PersonalExpenseCategoryDto>> =
        try {
            Result.success(
                client.get("$baseUrl/personal-expense-categories") {
                    header("Authorization", "Bearer $token")
                }.body()
            )
        } catch (e: Exception) {
            Log.e("PersonalExpenseApi", "getCategories failed", e)
            Result.failure(e)
        }

    suspend fun createCategory(token: String, name: String, colorHex: String): Result<PersonalExpenseCategoryDto> =
        try {
            Result.success(
                client.post("$baseUrl/personal-expense-categories") {
                    header("Authorization", "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(CreateCategoryRequest(name, colorHex))
                }.body()
            )
        } catch (e: Exception) {
            Log.e("PersonalExpenseApi", "createCategory failed", e)
            Result.failure(e)
        }

    suspend fun deleteCategory(token: String, id: Long): Result<Unit> =
        try {
            client.delete("$baseUrl/personal-expense-categories?id=$id") {
                header("Authorization", "Bearer $token")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PersonalExpenseApi", "deleteCategory failed", e)
            Result.failure(e)
        }

    // ── Expenses ──────────────────────────────────────────────────────────────

    suspend fun getExpenses(token: String): Result<List<PersonalExpenseDto>> =
        try {
            Result.success(
                client.get("$baseUrl/personal-expenses") {
                    header("Authorization", "Bearer $token")
                }.body()
            )
        } catch (e: Exception) {
            Log.e("PersonalExpenseApi", "getExpenses failed", e)
            Result.failure(e)
        }

    suspend fun createExpense(
        token: String,
        expense: PersonalExpense,
        items: List<PersonalExpenseItem>
    ): Result<PersonalExpenseDto> =
        try {
            Result.success(
                client.post("$baseUrl/personal-expenses") {
                    header("Authorization", "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(
                        CreatePersonalExpenseRequest(
                            date              = dateFmt.format(Date(expense.date)),
                            vendor            = expense.vendor,
                            category_id       = expense.categoryId,
                            total_cost        = expense.totalCost,
                            notes             = expense.notes,
                            receipt_image_path = expense.receiptImagePath,
                            items             = items.map {
                                PersonalExpenseItemRequest(it.name, it.categoryId, it.cost)
                            }
                        )
                    )
                }.body()
            )
        } catch (e: Exception) {
            Log.e("PersonalExpenseApi", "createExpense failed", e)
            Result.failure(e)
        }

    suspend fun updateExpense(
        token: String,
        expense: PersonalExpense,
        items: List<PersonalExpenseItem>
    ): Result<PersonalExpenseDto> =
        try {
            Result.success(
                client.put("$baseUrl/personal-expenses") {
                    header("Authorization", "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(
                        UpdatePersonalExpenseRequest(
                            id                = expense.id,
                            date              = dateFmt.format(Date(expense.date)),
                            vendor            = expense.vendor,
                            category_id       = expense.categoryId,
                            total_cost        = expense.totalCost,
                            notes             = expense.notes,
                            receipt_image_path = expense.receiptImagePath,
                            items             = items.map {
                                PersonalExpenseItemRequest(it.name, it.categoryId, it.cost)
                            }
                        )
                    )
                }.body()
            )
        } catch (e: Exception) {
            Log.e("PersonalExpenseApi", "updateExpense failed", e)
            Result.failure(e)
        }

    suspend fun deleteExpense(token: String, id: Long): Result<Unit> =
        try {
            client.delete("$baseUrl/personal-expenses?id=$id") {
                header("Authorization", "Bearer $token")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PersonalExpenseApi", "deleteExpense failed", e)
            Result.failure(e)
        }
}
