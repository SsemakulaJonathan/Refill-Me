package com.simi.refillme.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.simi.refillme.data.entity.UserPreferences
import com.simi.refillme.data.entity.VehicleAutoTripConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    
    private object PreferencesKeys {
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val DECIMAL_PLACES = intPreferencesKey("decimal_places")
        val PRICE_FORMAT = stringPreferencesKey("price_format")
        // Legacy single-vehicle Auto Trip Logging keys (kept for migration)
        val AUTO_TRIP_ENABLED = booleanPreferencesKey("auto_trip_enabled")
        val SELECTED_BLUETOOTH_DEVICE = stringPreferencesKey("selected_bluetooth_device")
        val SELECTED_BLUETOOTH_DEVICE_NAME = stringPreferencesKey("selected_bluetooth_device_name")
        val SELECTED_WIFI_SSID = stringPreferencesKey("selected_wifi_ssid")
        val SELECTED_VEHICLE_ID = longPreferencesKey("selected_vehicle_id")
        // Multi-vehicle configs (JSON string)
        val VEHICLE_AUTO_TRIP_CONFIGS = stringPreferencesKey("vehicle_auto_trip_configs")
        // Global active vehicle for viewing across screens
        val ACTIVE_VIEWING_VEHICLE_ID = longPreferencesKey("active_viewing_vehicle_id")
    }

    // ── JSON helpers ──────────────────────────────────────────────────────────

    private fun serializeVehicleConfigs(configs: List<VehicleAutoTripConfig>): String {
        val array = org.json.JSONArray()
        configs.forEach { cfg ->
            val obj = org.json.JSONObject()
            obj.put("vehicleId", cfg.vehicleId)
            obj.put("autoTripEnabled", cfg.autoTripEnabled)
            cfg.bluetoothDevice?.let { obj.put("bluetoothDevice", it) }
            cfg.bluetoothDeviceName?.let { obj.put("bluetoothDeviceName", it) }
            cfg.wifiSsid?.let { obj.put("wifiSsid", it) }
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseVehicleConfigs(json: String): List<VehicleAutoTripConfig> {
        return try {
            val array = org.json.JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                VehicleAutoTripConfig(
                    vehicleId = obj.getLong("vehicleId"),
                    autoTripEnabled = obj.optBoolean("autoTripEnabled", false),
                    bluetoothDevice = obj.optString("bluetoothDevice").takeIf { it.isNotEmpty() },
                    bluetoothDeviceName = obj.optString("bluetoothDeviceName").takeIf { it.isNotEmpty() },
                    wifiSsid = obj.optString("wifiSsid").takeIf { it.isNotEmpty() }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        // Load multi-vehicle configs; if none saved yet, migrate from legacy single-vehicle prefs
        val configsJson = preferences[PreferencesKeys.VEHICLE_AUTO_TRIP_CONFIGS]
        val vehicleAutoTripConfigs = when {
            !configsJson.isNullOrEmpty() -> parseVehicleConfigs(configsJson)
            else -> {
                // Migrate legacy single-vehicle prefs to the new format
                val vehicleId = preferences[PreferencesKeys.SELECTED_VEHICLE_ID]
                if (vehicleId != null) {
                    listOf(
                        VehicleAutoTripConfig(
                            vehicleId = vehicleId,
                            autoTripEnabled = preferences[PreferencesKeys.AUTO_TRIP_ENABLED] ?: false,
                            bluetoothDevice = preferences[PreferencesKeys.SELECTED_BLUETOOTH_DEVICE],
                            bluetoothDeviceName = preferences[PreferencesKeys.SELECTED_BLUETOOTH_DEVICE_NAME],
                            wifiSsid = preferences[PreferencesKeys.SELECTED_WIFI_SSID]
                        )
                    )
                } else emptyList()
            }
        }
        UserPreferences(
            currencySymbol = preferences[PreferencesKeys.CURRENCY_SYMBOL] ?: "UGX",
            currencyCode = preferences[PreferencesKeys.CURRENCY_CODE] ?: "UGX",
            decimalPlaces = preferences[PreferencesKeys.DECIMAL_PLACES] ?: 0,
            priceFormat = preferences[PreferencesKeys.PRICE_FORMAT] ?: "%,.0f",
            autoTripEnabled = preferences[PreferencesKeys.AUTO_TRIP_ENABLED] ?: false,
            selectedBluetoothDevice = preferences[PreferencesKeys.SELECTED_BLUETOOTH_DEVICE],
            selectedBluetoothDeviceName = preferences[PreferencesKeys.SELECTED_BLUETOOTH_DEVICE_NAME],
            selectedWifiSsid = preferences[PreferencesKeys.SELECTED_WIFI_SSID],
            selectedVehicleId = preferences[PreferencesKeys.SELECTED_VEHICLE_ID],
            vehicleAutoTripConfigs = vehicleAutoTripConfigs,
            activeViewingVehicleId = preferences[PreferencesKeys.ACTIVE_VIEWING_VEHICLE_ID]
        )
    }
    
    suspend fun updateCurrency(symbol: String, code: String, decimalPlaces: Int, format: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENCY_SYMBOL] = symbol
            preferences[PreferencesKeys.CURRENCY_CODE] = code
            preferences[PreferencesKeys.DECIMAL_PLACES] = decimalPlaces
            preferences[PreferencesKeys.PRICE_FORMAT] = format
        }
    }

    suspend fun updateAutoTripEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_TRIP_ENABLED] = enabled
        }
    }

    suspend fun updateBluetoothDevice(deviceAddress: String?, deviceName: String?) {
        context.dataStore.edit { preferences ->
            if (deviceAddress != null && deviceName != null) {
                preferences[PreferencesKeys.SELECTED_BLUETOOTH_DEVICE] = deviceAddress
                preferences[PreferencesKeys.SELECTED_BLUETOOTH_DEVICE_NAME] = deviceName
            } else {
                preferences.remove(PreferencesKeys.SELECTED_BLUETOOTH_DEVICE)
                preferences.remove(PreferencesKeys.SELECTED_BLUETOOTH_DEVICE_NAME)
            }
        }
    }

    suspend fun updateWifiSsid(ssid: String?) {
        context.dataStore.edit { preferences ->
            if (ssid != null) {
                preferences[PreferencesKeys.SELECTED_WIFI_SSID] = ssid
            } else {
                preferences.remove(PreferencesKeys.SELECTED_WIFI_SSID)
            }
        }
    }

    suspend fun updateSelectedVehicle(vehicleId: Long?) {
        context.dataStore.edit { preferences ->
            if (vehicleId != null) {
                preferences[PreferencesKeys.SELECTED_VEHICLE_ID] = vehicleId
            } else {
                preferences.remove(PreferencesKeys.SELECTED_VEHICLE_ID)
            }
        }
    }

    /** Save the full list of per-vehicle auto trip configurations. */
    suspend fun updateVehicleAutoTripConfigs(configs: List<VehicleAutoTripConfig>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VEHICLE_AUTO_TRIP_CONFIGS] = serializeVehicleConfigs(configs)
        }
    }

    /** Update or insert a single vehicle's auto trip config (upsert by vehicleId). */
    suspend fun upsertVehicleAutoTripConfig(config: VehicleAutoTripConfig, currentConfigs: List<VehicleAutoTripConfig>) {
        val updated = currentConfigs.toMutableList()
        val idx = updated.indexOfFirst { it.vehicleId == config.vehicleId }
        if (idx >= 0) updated[idx] = config else updated.add(config)
        updateVehicleAutoTripConfigs(updated)
    }

    /** Remove a vehicle's auto trip config by vehicleId. */
    suspend fun removeVehicleAutoTripConfig(vehicleId: Long, currentConfigs: List<VehicleAutoTripConfig>) {
        updateVehicleAutoTripConfigs(currentConfigs.filter { it.vehicleId != vehicleId })
    }

    /** Set the globally active vehicle for viewing across all screens. */
    suspend fun updateActiveViewingVehicle(vehicleId: Long?) {
        context.dataStore.edit { preferences ->
            if (vehicleId != null) {
                preferences[PreferencesKeys.ACTIVE_VIEWING_VEHICLE_ID] = vehicleId
            } else {
                preferences.remove(PreferencesKeys.ACTIVE_VIEWING_VEHICLE_ID)
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: PreferencesManager? = null
        
        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context).also { INSTANCE = it }
            }
        }
    }
}
