package com.simi.refillme.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Dashboard : Screen("dashboard")
    object AddRefill : Screen("add_refill/{vehicleId}?refillId={refillId}") {
        fun createRoute(vehicleId: Long = 0L, refillId: Long? = null) =
            if (refillId != null) "add_refill/$vehicleId?refillId=$refillId" else "add_refill/$vehicleId"
    }
    object AddService : Screen("add_service/{vehicleId}?serviceId={serviceId}") {
        fun createRoute(vehicleId: Long = 0L, serviceId: Long? = null) =
            if (serviceId != null) "add_service/$vehicleId?serviceId=$serviceId" else "add_service/$vehicleId"
    }
    object AddExpense : Screen("add_expense/{vehicleId}?expenseId={expenseId}") {
        fun createRoute(vehicleId: Long = 0L, expenseId: Long? = null) =
            if (expenseId != null) "add_expense/$vehicleId?expenseId=$expenseId" else "add_expense/$vehicleId"
    }
    object Vehicles : Screen("vehicles")
    object AddVehicle : Screen("add_vehicle?vehicleId={vehicleId}") {
        fun createRoute(vehicleId: Long? = null) =
            if (vehicleId != null) "add_vehicle?vehicleId=$vehicleId" else "add_vehicle"
    }
    object History : Screen("history")
    object Statistics : Screen("statistics")
    object Settings : Screen("settings")
    object AutoTripLogging : Screen("auto_trip_logging")
    object Maps : Screen("maps")
    object AllTrips : Screen("all_trips")
    object PersonalExpenses : Screen("personal_expenses")
    object AddPersonalExpense : Screen("add_personal_expense?expenseId={expenseId}") {
        fun createRoute(expenseId: Long? = null) =
            if (expenseId != null) "add_personal_expense?expenseId=$expenseId"
            else "add_personal_expense"
    }
}
