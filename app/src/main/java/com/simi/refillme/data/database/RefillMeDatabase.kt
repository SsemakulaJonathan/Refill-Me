package com.simi.refillme.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.simi.refillme.data.dao.ExpenseDao
import com.simi.refillme.data.dao.ExpenseTaskDao
import com.simi.refillme.data.dao.PersonalExpenseCategoryDao
import com.simi.refillme.data.dao.PersonalExpenseDao
import com.simi.refillme.data.dao.PersonalExpenseItemDao
import com.simi.refillme.data.dao.RefillDao
import com.simi.refillme.data.dao.ServiceDao
import com.simi.refillme.data.dao.ServiceItemDao
import com.simi.refillme.data.dao.TripDao
import com.simi.refillme.data.dao.VehicleDao
import com.simi.refillme.data.dao.VehicleSpecificationDao
import com.simi.refillme.data.entity.Expense
import com.simi.refillme.data.entity.ExpenseTask
import com.simi.refillme.data.entity.PersonalExpense
import com.simi.refillme.data.entity.PersonalExpenseCategory
import com.simi.refillme.data.entity.PersonalExpenseItem
import com.simi.refillme.data.entity.Refill
import com.simi.refillme.data.entity.Service
import com.simi.refillme.data.entity.ServiceItem
import com.simi.refillme.data.entity.Trip
import com.simi.refillme.data.entity.Vehicle
import com.simi.refillme.data.entity.VehicleSpecification

@Database(
    entities = [
        Vehicle::class, Refill::class, VehicleSpecification::class,
        Trip::class, Service::class, ServiceItem::class,
        Expense::class, ExpenseTask::class,
        PersonalExpenseCategory::class, PersonalExpense::class, PersonalExpenseItem::class
    ],
    version = 7,
    exportSchema = false
)
abstract class RefillMeDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun refillDao(): RefillDao
    abstract fun vehicleSpecificationDao(): VehicleSpecificationDao
    abstract fun tripDao(): TripDao
    abstract fun serviceDao(): ServiceDao
    abstract fun serviceItemDao(): ServiceItemDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun expenseTaskDao(): ExpenseTaskDao
    abstract fun personalExpenseCategoryDao(): PersonalExpenseCategoryDao
    abstract fun personalExpenseDao(): PersonalExpenseDao
    abstract fun personalExpenseItemDao(): PersonalExpenseItemDao

    companion object {
        @Volatile
        private var INSTANCE: RefillMeDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the trips table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS trips (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        vehicleId INTEGER NOT NULL,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER,
                        startLatitude REAL,
                        startLongitude REAL,
                        startAddress TEXT,
                        endLatitude REAL,
                        endLongitude REAL,
                        endAddress TEXT,
                        distanceKm REAL NOT NULL DEFAULT 0.0,
                        averageSpeedKmh REAL NOT NULL DEFAULT 0.0,
                        maxSpeedKmh REAL NOT NULL DEFAULT 0.0,
                        durationMinutes INTEGER NOT NULL DEFAULT 0,
                        tripType TEXT NOT NULL DEFAULT 'PERSONAL',
                        notes TEXT,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        FOREIGN KEY(vehicleId) REFERENCES vehicles(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                // Create index on vehicleId
                database.execSQL("CREATE INDEX IF NOT EXISTS index_trips_vehicleId ON trips(vehicleId)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the services table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS services (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        vehicleId INTEGER NOT NULL,
                        date INTEGER NOT NULL,
                        odometerReading INTEGER NOT NULL,
                        serviceCenter TEXT NOT NULL,
                        totalCost REAL NOT NULL,
                        notes TEXT,
                        receiptImagePath TEXT,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(vehicleId) REFERENCES vehicles(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                // Create index on vehicleId
                database.execSQL("CREATE INDEX IF NOT EXISTS index_services_vehicleId ON services(vehicleId)")

                // Create the service_items table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS service_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        serviceId INTEGER NOT NULL,
                        serviceName TEXT NOT NULL,
                        cost REAL NOT NULL,
                        FOREIGN KEY(serviceId) REFERENCES services(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                // Create index on serviceId
                database.execSQL("CREATE INDEX IF NOT EXISTS index_service_items_serviceId ON service_items(serviceId)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the expenses table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS expenses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        vehicleId INTEGER NOT NULL,
                        date INTEGER NOT NULL,
                        odometerReading INTEGER NOT NULL,
                        vendor TEXT NOT NULL,
                        totalCost REAL NOT NULL,
                        notes TEXT,
                        receiptImagePath TEXT,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(vehicleId) REFERENCES vehicles(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                // Create index on vehicleId
                database.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_vehicleId ON expenses(vehicleId)")

                // Create the expense_tasks table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS expense_tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        expenseId INTEGER NOT NULL,
                        taskName TEXT NOT NULL,
                        cost REAL NOT NULL,
                        FOREIGN KEY(expenseId) REFERENCES expenses(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                // Create index on expenseId
                database.execSQL("CREATE INDEX IF NOT EXISTS index_expense_tasks_expenseId ON expense_tasks(expenseId)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `personal_expense_categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `colorHex` TEXT NOT NULL,
                        `isDefault` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `personal_expenses` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` INTEGER NOT NULL,
                        `vendor` TEXT NOT NULL,
                        `categoryId` INTEGER,
                        `totalCost` REAL NOT NULL,
                        `notes` TEXT,
                        `receiptImagePath` TEXT,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `personal_expense_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `expenseId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `categoryId` INTEGER,
                        `cost` REAL NOT NULL,
                        FOREIGN KEY(`expenseId`) REFERENCES `personal_expenses`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_personal_expense_items_expenseId` ON `personal_expense_items`(`expenseId`)"
                )
            }
        }

        fun getDatabase(context: Context): RefillMeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RefillMeDatabase::class.java,
                    "refillme_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
