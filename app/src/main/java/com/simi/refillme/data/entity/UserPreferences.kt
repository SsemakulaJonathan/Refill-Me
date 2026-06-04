package com.simi.refillme.data.entity

/**
 * Per-vehicle Auto Trip Logging configuration.
 * Each vehicle can have its own Bluetooth device and WiFi SSID trigger.
 */
data class VehicleAutoTripConfig(
    val vehicleId: Long,
    val autoTripEnabled: Boolean = false,
    val bluetoothDevice: String? = null,      // Device MAC address
    val bluetoothDeviceName: String? = null,
    val wifiSsid: String? = null
)

data class UserPreferences(
    val currencySymbol: String = "UGX",
    val currencyCode: String = "UGX",
    val decimalPlaces: Int = 0,
    val priceFormat: String = "%,.0f", // Format for displaying prices
    val useMetricSystem: Boolean = true, // For future: miles vs km
    // Legacy single-vehicle Auto Trip settings (kept for backward compat / migration)
    val autoTripEnabled: Boolean = false,
    val selectedBluetoothDevice: String? = null, // Device address
    val selectedBluetoothDeviceName: String? = null,
    val selectedWifiSsid: String? = null, // WiFi SSID
    val selectedVehicleId: Long? = null,
    // Multi-vehicle Auto Trip configs (replaces single-vehicle settings)
    val vehicleAutoTripConfigs: List<VehicleAutoTripConfig> = emptyList(),
    // Global active vehicle for viewing across all screens (Dashboard, Stats, etc.)
    val activeViewingVehicleId: Long? = null
)

object Currency {
    val currencies = listOf(
        CurrencyInfo("UGX", "UGX", 0, "%,.0f"), // Ugandan Shilling
        CurrencyInfo("USD", "$", 2, "%,.2f"),    // US Dollar
        CurrencyInfo("EUR", "€", 2, "%,.2f"),    // Euro
        CurrencyInfo("GBP", "£", 2, "%,.2f"),    // British Pound
        CurrencyInfo("KES", "KSh", 2, "%,.2f"),  // Kenyan Shilling
        CurrencyInfo("TZS", "TSh", 0, "%,.0f"),  // Tanzanian Shilling
        CurrencyInfo("ZAR", "R", 2, "%,.2f"),    // South African Rand
        CurrencyInfo("NGN", "₦", 2, "%,.2f"),    // Nigerian Naira
        CurrencyInfo("GHS", "GH₵", 2, "%,.2f"),  // Ghanaian Cedi
        CurrencyInfo("INR", "₹", 2, "%,.2f"),    // Indian Rupee
        CurrencyInfo("AED", "د.إ", 2, "%,.2f"),  // UAE Dirham
        CurrencyInfo("SAR", "ر.س", 2, "%,.2f"),  // Saudi Riyal
    )
}

data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val decimalPlaces: Int,
    val format: String
)
