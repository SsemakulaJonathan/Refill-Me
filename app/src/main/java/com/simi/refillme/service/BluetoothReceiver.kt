package com.simi.refillme.service

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.simi.refillme.data.preferences.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BluetoothReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    handleBluetoothConnected(context, it)
                }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    handleBluetoothDisconnected(context, it)
                }
            }
        }
    }

    private fun handleBluetoothConnected(context: Context, device: BluetoothDevice) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preferencesManager = PreferencesManager.getInstance(context)
                val preferences = preferencesManager.userPreferencesFlow.first()

                // Check all per-vehicle configs for a matching BT device
                val configs = preferences.vehicleAutoTripConfigs.ifEmpty {
                    // Legacy fallback: single-vehicle prefs
                    if (preferences.selectedVehicleId != null && preferences.selectedBluetoothDevice != null) {
                        listOf(
                            com.simi.refillme.data.entity.VehicleAutoTripConfig(
                                vehicleId = preferences.selectedVehicleId,
                                autoTripEnabled = preferences.autoTripEnabled,
                                bluetoothDevice = preferences.selectedBluetoothDevice
                            )
                        )
                    } else emptyList()
                }
                val matchingConfig = configs.find { it.autoTripEnabled && it.bluetoothDevice == device.address }

                if (matchingConfig != null) {
                    Log.d(TAG, "Connected to car Bluetooth: ${device.name} → vehicleId=${matchingConfig.vehicleId}")
                    val serviceIntent = Intent(context, TripTrackingService::class.java).apply {
                        action = TripTrackingService.ACTION_START_TRIP
                        putExtra(TripTrackingService.EXTRA_VEHICLE_ID, matchingConfig.vehicleId)
                    }
                    try {
                        context.startForegroundService(serviceIntent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting trip tracking service", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling Bluetooth connection", e)
            }
        }
    }

    private fun handleBluetoothDisconnected(context: Context, device: BluetoothDevice) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preferencesManager = PreferencesManager.getInstance(context)
                val preferences = preferencesManager.userPreferencesFlow.first()

                // Check if this device is configured for any vehicle
                val configs = preferences.vehicleAutoTripConfigs.ifEmpty {
                    if (preferences.selectedBluetoothDevice != null) {
                        listOf(com.simi.refillme.data.entity.VehicleAutoTripConfig(
                            vehicleId = preferences.selectedVehicleId ?: 0L,
                            bluetoothDevice = preferences.selectedBluetoothDevice
                        ))
                    } else emptyList()
                }
                val isConfigured = configs.any { it.bluetoothDevice == device.address }

                if (isConfigured) {
                    Log.d(TAG, "Disconnected from car Bluetooth: ${device.name}")
                    val serviceIntent = Intent(context, TripTrackingService::class.java).apply {
                        action = TripTrackingService.ACTION_STOP_TRIP
                    }
                    try {
                        context.startService(serviceIntent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error stopping trip tracking service", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling Bluetooth disconnection", e)
            }
        }
    }

    companion object {
        private const val TAG = "BluetoothReceiver"
    }
}
