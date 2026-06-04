package com.simi.refillme.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.simi.refillme.MainActivity
import com.simi.refillme.R
import com.simi.refillme.data.entity.VehicleAutoTripConfig
import com.simi.refillme.RefillMeApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BluetoothMonitoringService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // All enabled per-vehicle configs (BT device address → config)
    private var enabledConfigs: List<VehicleAutoTripConfig> = emptyList()
    // Legacy single-device support (used only when enabledConfigs is empty)
    private var configuredWifiSsid: String? = null
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    companion object {
        private const val TAG = "BluetoothMonitorService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "bluetooth_monitor_channel"

        fun start(context: Context) {
            val intent = Intent(context, BluetoothMonitoringService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BluetoothMonitoringService::class.java)
            context.stopService(intent)
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    Log.d(TAG, "Bluetooth device connected: ${device?.address}")

                    // Find a matching enabled config for the connected device
                    val matchingConfig = enabledConfigs.find { it.bluetoothDevice == device?.address }
                    if (matchingConfig != null) {
                        Log.d(TAG, "Configured device connected (vehicleId=${matchingConfig.vehicleId}) - starting trip")
                        startTripTracking(matchingConfig.vehicleId, matchingConfig.bluetoothDevice)
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    Log.d(TAG, "Bluetooth device disconnected: ${device?.address}")
                    // TripTrackingService will handle the 2-minute disconnect logic
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BluetoothMonitoringService created")
        createNotificationChannel()

        // Load configuration from preferences
        loadConfiguration()

        // Register Bluetooth receiver
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        registerReceiver(bluetoothReceiver, filter)

        // Register WiFi monitoring
        registerWifiMonitoring()

        // Start foreground service
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called")
        loadConfiguration()
        return START_STICKY // Restart service if killed
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "BluetoothMonitoringService destroyed")
        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering Bluetooth receiver", e)
        }
        try {
            networkCallback?.let {
                connectivityManager?.unregisterNetworkCallback(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering network callback", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun loadConfiguration() {
        serviceScope.launch {
            val app = application as RefillMeApplication
            val preferences = app.preferencesManager.userPreferencesFlow.first()

            // Use per-vehicle configs (with fallback to legacy single-vehicle prefs)
            val configs = preferences.vehicleAutoTripConfigs.ifEmpty {
                // Legacy migration: if old single-vehicle prefs exist, treat as one config
                if (preferences.selectedVehicleId != null && preferences.selectedBluetoothDevice != null) {
                    listOf(
                        VehicleAutoTripConfig(
                            vehicleId = preferences.selectedVehicleId,
                            autoTripEnabled = preferences.autoTripEnabled,
                            bluetoothDevice = preferences.selectedBluetoothDevice,
                            bluetoothDeviceName = preferences.selectedBluetoothDeviceName,
                            wifiSsid = preferences.selectedWifiSsid
                        )
                    )
                } else emptyList()
            }
            enabledConfigs = configs.filter { it.autoTripEnabled }

            // For WiFi monitoring, collect unique SSIDs from enabled configs
            configuredWifiSsid = enabledConfigs.mapNotNull { it.wifiSsid }.firstOrNull()

            Log.d(TAG, "Configuration loaded - ${enabledConfigs.size} enabled configs")
            enabledConfigs.forEach { c ->
                Log.d(TAG, "  Vehicle ${c.vehicleId}: BT=${c.bluetoothDevice}, WiFi=${c.wifiSsid}")
            }

            // If no configs are enabled, stop the service
            if (enabledConfigs.isEmpty()) {
                Log.d(TAG, "No enabled auto trip configs - stopping service")
                stopSelf()
            }
        }
    }

    private fun registerWifiMonitoring() {
        if (configuredWifiSsid == null) {
            Log.d(TAG, "No WiFi SSID configured, skipping WiFi monitoring")
            return
        }

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                checkWifiConnection()
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                checkWifiConnection()
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.d(TAG, "WiFi network lost")
            }
        }

        try {
            connectivityManager?.registerNetworkCallback(networkRequest, networkCallback!!)
            Log.d(TAG, "WiFi monitoring registered")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering WiFi monitoring", e)
        }
    }

    private fun checkWifiConnection() {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo: WifiInfo? = wifiManager.connectionInfo

        if (wifiInfo != null) {
            val ssid = wifiInfo.ssid.trim('"') // Remove quotes from SSID
            Log.d(TAG, "Connected to WiFi: $ssid")

            // Find a config that matches this WiFi SSID
            val matchingConfig = enabledConfigs.find { it.wifiSsid == ssid }
            if (matchingConfig != null) {
                Log.d(TAG, "Configured WiFi network connected (vehicleId=${matchingConfig.vehicleId}) - starting trip")
                startTripTracking(matchingConfig.vehicleId, matchingConfig.bluetoothDevice)
            }
        }
    }

    private fun startTripTracking(vehicleId: Long, deviceAddress: String?) {
        val intent = Intent(this, TripTrackingService::class.java).apply {
            action = TripTrackingService.ACTION_START_TRIP
            putExtra(TripTrackingService.EXTRA_VEHICLE_ID, vehicleId)
            putExtra(TripTrackingService.EXTRA_DEVICE_ADDRESS, deviceAddress)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Auto Trip Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors Bluetooth connections for automatic trip tracking"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Auto Trip Logging Active")
            .setContentText("Monitoring for car connection (Bluetooth/WiFi)")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}
