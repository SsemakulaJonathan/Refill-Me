package com.simi.refillme.data.api

import android.util.Log
import com.simi.refillme.data.entity.Expense
import com.simi.refillme.data.entity.ExpenseTask
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

@Serializable
data class ExpenseResponse(
    val success: Boolean = true,
    val message: String = "",
    val expense: ExpenseDto? = null,
    val expenses: List<ExpenseDto> = emptyList()
)

@Serializable
data class ExpenseDto(
    val id: Int,
    val vehicle_id: Int,
    val user_id: Int,
    val date: String,
    val odometer_reading: Int,
    val vendor: String,
    val total_cost: Double,
    val notes: String?,
    val receipt_image_path: String?,
    val expense_tasks: List<ExpenseTaskDto>?,
    val created_at: String
)

@Serializable
data class ExpenseTaskDto(
    val id: Int,
    val task_name: String,
    val cost: Double
)

fun ExpenseDto.toExpense(): Expense {
    return Expense(
        id = this.id.toLong(),
        vehicleId = this.vehicle_id.toLong(),
        date = java.time.Instant.parse(this.date).toEpochMilli(),
        odometerReading = this.odometer_reading,
        vendor = this.vendor,
        totalCost = this.total_cost,
        notes = this.notes,
        receiptImagePath = this.receipt_image_path,
        createdAt = java.time.Instant.parse(this.created_at).toEpochMilli()
    )
}

fun ExpenseTaskDto.toExpenseTask(expenseId: Long): ExpenseTask {
    return ExpenseTask(
        id = this.id.toLong(),
        expenseId = expenseId,
        taskName = this.task_name,
        cost = this.cost
    )
}

@Serializable
data class CreateExpenseRequest(
    val vehicle_id: Long,
    val user_id: Int,
    val date: String,
    val odometer_reading: Int,
    val vendor: String,
    val total_cost: Double,
    val notes: String?,
    val receipt_image_path: String?,
    val expense_tasks: List<ExpenseTaskRequest>
)

@Serializable
data class ExpenseTaskRequest(
    val task_name: String,
    val cost: Double
)

class ExpenseApiService {

    private val baseUrl = "https://refill-me.vercel.app/api"

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.BODY
        }
    }

    companion object {
        @Volatile
        private var instance: ExpenseApiService? = null

        fun getInstance(): ExpenseApiService {
            return instance ?: synchronized(this) {
                instance ?: ExpenseApiService().also { instance = it }
            }
        }
    }

    suspend fun getExpenses(token: String, vehicleId: Long? = null): Result<List<ExpenseDto>> {
        return try {
            val url = if (vehicleId != null) {
                "$baseUrl/expenses?vehicle_id=$vehicleId"
            } else {
                "$baseUrl/expenses"
            }

            val response = client.get(url) {
                header("Authorization", "Bearer $token")
            }
            Result.success(response.body<List<ExpenseDto>>())
        } catch (e: Exception) {
            Log.e("ExpenseApiService", "Failed to get expenses", e)
            Result.failure(e)
        }
    }

    suspend fun createExpense(
        token: String,
        userId: Int,
        expense: Expense,
        expenseTasks: List<Pair<String, Double>>
    ): Result<ExpenseDto> {
        return try {
            // Format date as ISO-8601 timestamp (PostgreSQL compatible)
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            dateFormatter.timeZone = TimeZone.getTimeZone("UTC")
            val formattedDate = dateFormatter.format(Date(expense.date))

            val request = CreateExpenseRequest(
                vehicle_id = expense.vehicleId,
                user_id = userId,
                date = formattedDate,
                odometer_reading = expense.odometerReading,
                vendor = expense.vendor,
                total_cost = expense.totalCost,
                notes = expense.notes,
                receipt_image_path = expense.receiptImagePath,
                expense_tasks = expenseTasks.map { (name, cost) ->
                    ExpenseTaskRequest(task_name = name, cost = cost)
                }
            )

            Log.d("ExpenseApiService", "Creating expense: $request")

            val response = client.post("$baseUrl/expenses") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            val result = response.body<ExpenseDto>()
            Log.d("ExpenseApiService", "Create expense response: $result")
            Result.success(result)
        } catch (e: Exception) {
            Log.e("ExpenseApiService", "Failed to create expense", e)
            Result.failure(e)
        }
    }

    suspend fun updateExpense(
        token: String,
        userId: Int,
        id: Long,
        expense: Expense,
        expenseTasks: List<Pair<String, Double>>
    ): Result<ExpenseDto> {
        return try {
            // Format date as ISO-8601 timestamp (PostgreSQL compatible)
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            dateFormatter.timeZone = TimeZone.getTimeZone("UTC")
            val formattedDate = dateFormatter.format(Date(expense.date))

            val request = CreateExpenseRequest(
                vehicle_id = expense.vehicleId,
                user_id = userId,
                date = formattedDate,
                odometer_reading = expense.odometerReading,
                vendor = expense.vendor,
                total_cost = expense.totalCost,
                notes = expense.notes,
                receipt_image_path = expense.receiptImagePath,
                expense_tasks = expenseTasks.map { (name, cost) ->
                    ExpenseTaskRequest(task_name = name, cost = cost)
                }
            )

            val response = client.put("$baseUrl/expenses?id=$id") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Result.success(response.body<ExpenseDto>())
        } catch (e: Exception) {
            Log.e("ExpenseApiService", "Failed to update expense", e)
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(token: String, id: Long): Result<ExpenseResponse> {
        return try {
            val response = client.delete("$baseUrl/expenses?id=$id") {
                header("Authorization", "Bearer $token")
            }
            Result.success(response.body<ExpenseResponse>())
        } catch (e: Exception) {
            Log.e("ExpenseApiService", "Failed to delete expense", e)
            Result.failure(e)
        }
    }
}
