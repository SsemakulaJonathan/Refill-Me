package com.simi.refillme.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.simi.refillme.RefillMeApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device booted - checking auto trip settings")

            val app = context.applicationContext as RefillMeApplication
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

            scope.launch {
                val preferences = app.preferencesManager.userPreferencesFlow.first()

                if (preferences.autoTripEnabled &&
                    (preferences.selectedBluetoothDevice != null || preferences.selectedWifiSsid != null) &&
                    preferences.selectedVehicleId != null) {

                    Log.d(TAG, "Auto trip is enabled - starting connection monitoring service")
                    BluetoothMonitoringService.start(context)
                } else {
                    Log.d(TAG, "Auto trip not configured - skipping")
                }
            }
        }
    }
}
