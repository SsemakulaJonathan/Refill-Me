package com.simi.refillme.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.simi.refillme.MainActivity
import com.simi.refillme.R
import com.simi.refillme.RefillMeApplication
import com.simi.refillme.data.entity.Trip
import com.simi.refillme.data.repository.TripRepository
import com.simi.refillme.utils.LocationHelper
import kotlinx.coroutines.*

class TripTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var tripRepository: TripRepository
    private lateinit var locationHelper: LocationHelper

    private var currentTrip: Trip? = null
    private var currentVehicleId: Long? = null
    private var locations = mutableListOf<Location>()
    private var lastLocation: Location? = null
    private var maxSpeed: Double = 0.0
    private var updateJob: Job? = null
    
    // Bluetooth monitoring
    private var bluetoothDevice: BluetoothDevice? = null
    private var disconnectTime: Long? = null
    private var bluetoothMonitorJob: Job? = null
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
                    if (device?.address == bluetoothDevice?.address) {
                        Log.d(TAG, "Bluetooth device reconnected: ${device?.name ?: "Unknown"}")
                        disconnectTime = null
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    if (device?.address == bluetoothDevice?.address) {
                        Log.d(TAG, "Bluetooth device disconnected: ${device?.name ?: "Unknown"}")
                        disconnectTime = System.currentTimeMillis()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TripTrackingService created")

        val app = application as RefillMeApplication
        tripRepository = app.tripRepository
        locationHelper = LocationHelper(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    handleNewLocation(location)
                }
            }
        }

        createNotificationChannel()
        
        // Register Bluetooth receiver
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        registerReceiver(bluetoothReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")

        when (intent?.action) {
            ACTION_START_TRIP -> {
                val vehicleId = intent.getLongExtra(EXTRA_VEHICLE_ID, -1L)
                val deviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS)
                if (vehicleId != -1L) {
                    startTrip(vehicleId, deviceAddress)
                }
            }
            ACTION_STOP_TRIP -> {
                stopTrip()
            }
        }

        return START_STICKY
    }

    private fun startTrip(vehicleId: Long, deviceAddress: String?) {
        Log.d(TAG, "Starting trip for vehicle: $vehicleId with device: $deviceAddress")
        currentVehicleId = vehicleId
        
        // Set up Bluetooth monitoring if device address provided
        if (deviceAddress != null) {
            try {
                val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val bluetoothAdapter = bluetoothManager.adapter
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress)
                    startBluetoothMonitoring()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up Bluetooth monitoring", e)
            }
        }

        // Start foreground service immediately to avoid crash
        val notification = createNotification("Trip starting...", "Initializing trip tracking...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        Log.d(TAG, "Foreground service started with notification")

        serviceScope.launch {
            try {
                // Try to get last known location immediately
                val initialLocation: Location? = if (ActivityCompat.checkSelfPermission(
                        this@TripTrackingService,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    try {
                        suspendCancellableCoroutine<Location?> { continuation ->
                            fusedLocationClient.lastLocation
                                .addOnSuccessListener { location ->
                                    if (location != null) {
                                        Log.d(TAG, "Got last known location: ${location.latitude}, ${location.longitude}")
                                    }
                                    continuation.resume(location, null)
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error getting last known location", e)
                                    continuation.resume(null, null)
                                }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception getting last known location", e)
                        null
                    }
                } else {
                    null
                }

                // Check if there's already an active trip
                val activeTrip = tripRepository.getActiveTrip()
                if (activeTrip != null) {
                    Log.d(TAG, "Active trip already exists, resuming")
                    currentTrip = activeTrip
                } else {
                    // Reverse geocode start address if location available
                    val startAddress = initialLocation?.let { location ->
                        try {
                            locationHelper.getAddressFromLocation(location)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error getting start address", e)
                            null
                        }
                    }

                    // Create a new trip with initial location if available
                    val trip = Trip(
                        vehicleId = vehicleId,
                        startTime = System.currentTimeMillis(),
                        startLatitude = initialLocation?.latitude,
                        startLongitude = initialLocation?.longitude,
                        startAddress = startAddress,
                        isActive = true
                    )
                    val tripId = tripRepository.insertTrip(trip)
                    currentTrip = trip.copy(id = tripId)
                    Log.d(TAG, "Created new trip with ID: $tripId, location: ${initialLocation != null}, address: $startAddress")
                }

                // Update notification
                val updatedNotification = createNotification("Trip in progress", "Tracking your journey...")
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.notify(NOTIFICATION_ID, updatedNotification)
                Log.d(TAG, "Notification updated")

                // Start location updates
                startLocationUpdates()

                // Start periodic trip updates
                startPeriodicTripUpdates()

            } catch (e: Exception) {
                Log.e(TAG, "Error starting trip", e)
                stopSelf()
            }
        }
    }

    private fun startPeriodicTripUpdates() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (isActive) {
                delay(10000) // Update every 10 seconds
                updateCurrentTripStats()
            }
        }
        Log.d(TAG, "Started periodic trip updates")
    }
    
    private fun startBluetoothMonitoring() {
        bluetoothMonitorJob?.cancel()
        bluetoothMonitorJob = serviceScope.launch {
            while (isActive) {
                delay(10000) // Check every 10 seconds
                disconnectTime?.let { disconnectTime ->
                    val timeSinceDisconnect = System.currentTimeMillis() - disconnectTime
                    val disconnectThreshold = 120000L // 2 minutes in milliseconds
                    if (timeSinceDisconnect >= disconnectThreshold) {
                        Log.d(TAG, "Bluetooth device disconnected for 2 minutes, stopping trip")
                        stopTrip()
                        return@launch
                    }
                }
            }
        }
        Log.d(TAG, "Started Bluetooth monitoring")
    }

    private suspend fun updateCurrentTripStats() {
        currentTrip?.let { trip ->
            if (!trip.isActive) return
            
            val distance = calculateTotalDistance()
            val durationMillis = System.currentTimeMillis() - trip.startTime
            val durationMinutes = durationMillis / 1000 / 60
            val avgSpeed = if (durationMinutes > 0) {
                (distance / durationMinutes) * 60 // km/h
            } else {
                0.0
            }

            val updatedTrip = trip.copy(
                distanceKm = distance,
                averageSpeedKmh = avgSpeed,
                maxSpeedKmh = maxSpeed,
                durationMinutes = durationMinutes
            )
            
            try {
                tripRepository.updateTrip(updatedTrip)
                currentTrip = updatedTrip
                Log.d(TAG, "Periodic update - Distance: ${distance}km, Duration: ${durationMinutes}min, Avg: ${avgSpeed}km/h")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating trip stats", e)
            }
        }
    }

    private fun stopTrip() {
        Log.d(TAG, "Stopping trip")

        // Cancel periodic updates
        updateJob?.cancel()
        bluetoothMonitorJob?.cancel()
        bluetoothDevice = null
        disconnectTime = null

        serviceScope.launch {
            try {
                // If currentTrip is null, try to fetch active trip from repository
                var trip = currentTrip
                if (trip == null) {
                    Log.d(TAG, "currentTrip is null, fetching active trip from repository")
                    trip = tripRepository.getActiveTrip()
                    if (trip == null) {
                        Log.e(TAG, "No active trip found in repository")
                        return@launch
                    }
                    currentTrip = trip
                    Log.d(TAG, "Loaded active trip from repository: id=${trip.id}")
                }

                if (!trip.isActive) {
                    Log.d(TAG, "Trip already stopped")
                    return@launch
                }

                val endTime = System.currentTimeMillis()
                val duration = (endTime - trip.startTime) / 1000 / 60 // minutes

                // Calculate distance
                val distance = calculateTotalDistance()

                // Calculate average speed
                val avgSpeed = if (duration > 0) {
                    (distance / duration) * 60 // km/h
                } else {
                    0.0
                }

                // Get end address from last location
                val endAddress = lastLocation?.let { location ->
                    try {
                        locationHelper.getAddressFromLocation(location)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting end address", e)
                        null
                    }
                }

                // Update trip
                val updatedTrip = trip.copy(
                    endTime = endTime,
                    endLatitude = lastLocation?.latitude,
                    endLongitude = lastLocation?.longitude,
                    endAddress = endAddress,
                    distanceKm = distance,
                    averageSpeedKmh = avgSpeed,
                    maxSpeedKmh = maxSpeed,
                    durationMinutes = duration,
                    isActive = false
                )

                tripRepository.updateTrip(updatedTrip)
                Log.d(TAG, "Trip completed: ${distance}km in ${duration}min from '${trip.startAddress}' to '$endAddress'")

                // Reset state
                currentTrip = null
                locations.clear()
                lastLocation = null
                maxSpeed = 0.0
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping trip", e)
            } finally {
                stopLocationUpdates()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Location permission not granted")
            stopSelf()
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATE_INTERVAL
        ).apply {
            setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL)
            setWaitForAccurateLocation(false)
        }.build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        Log.d(TAG, "Location updates started")
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d(TAG, "Location updates stopped")
    }

    private fun handleNewLocation(location: Location) {
        Log.d(TAG, "New location: ${location.latitude}, ${location.longitude}, speed: ${location.speed}")

        // Update start location if this is the first location
        if (currentTrip?.startLatitude == null) {
            serviceScope.launch {
                currentTrip?.let { trip ->
                    // Get address from coordinates
                    val startAddress = try {
                        locationHelper.getAddressFromLocation(location)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting start address", e)
                        null
                    }

                    val updatedTrip = trip.copy(
                        startLatitude = location.latitude,
                        startLongitude = location.longitude,
                        startAddress = startAddress
                    )

                    // Retry logic for API update
                    var retries = 0
                    while (retries < 3) {
                        try {
                            tripRepository.updateTrip(updatedTrip)
                            currentTrip = updatedTrip
                            Log.d(TAG, "Updated start location and address: $startAddress")
                            break
                        } catch (e: Exception) {
                            retries++
                            if (retries < 3) {
                                delay(1000L * retries) // Backoff: 1s, 2s
                                Log.w(TAG, "Retry $retries: Failed to update start location", e)
                            } else {
                                Log.e(TAG, "Failed to update start location after 3 retries", e)
                            }
                        }
                    }
                }
            }
        }

        // Track speed (m/s to km/h)
        val speedKmh = (location.speed * 3.6)
        if (speedKmh > maxSpeed) {
            maxSpeed = speedKmh
        }

        // Store location for distance calculation
        locations.add(location)
        lastLocation = location

        // Calculate current stats
        val distance = calculateTotalDistance()
        val durationMillis = System.currentTimeMillis() - (currentTrip?.startTime ?: 0)
        val durationMinutes = durationMillis / 1000 / 60
        val avgSpeed = if (durationMinutes > 0) {
            (distance / durationMinutes) * 60 // km/h
        } else {
            0.0
        }

        // Update trip in database periodically (every 5 locations or ~25 seconds)
        if (locations.size % 5 == 0) {
            serviceScope.launch {
                currentTrip?.let { trip ->
                    val updatedTrip = trip.copy(
                        distanceKm = distance,
                        averageSpeedKmh = avgSpeed,
                        maxSpeedKmh = maxSpeed,
                        durationMinutes = durationMinutes
                    )
                    tripRepository.updateTrip(updatedTrip)
                    currentTrip = updatedTrip
                    Log.d(TAG, "Updated trip in database: ${distance}km, ${avgSpeed}km/h avg, ${maxSpeed}km/h max")
                }
            }
        }

        // Update notification
        val notification = createNotification(
            "Trip in progress",
            "Distance: ${"%.2f".format(distance)} km | Speed: ${"%.0f".format(speedKmh)} km/h"
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun calculateTotalDistance(): Double {
        var distance = 0.0
        for (i in 1 until locations.size) {
            distance += locations[i - 1].distanceTo(locations[i]) / 1000.0 // meters to km
        }
        return distance
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Trip Tracking",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for active trip tracking"
                setShowBadge(true)
                enableVibration(false)
                enableLights(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    private fun createNotification(title: String, text: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopTripIntent = Intent(this, TripTrackingService::class.java).apply {
            action = ACTION_STOP_TRIP
        }
        val stopTripPendingIntent = PendingIntent.getService(
            this,
            1,
            stopTripIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Trip",
                stopTripPendingIntent
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "TripTrackingService destroyed")
        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering Bluetooth receiver", e)
        }
        serviceScope.cancel()
        stopLocationUpdates()
    }

    companion object {
        private const val TAG = "TripTrackingService"
        private const val CHANNEL_ID = "trip_tracking_channel"
        private const val NOTIFICATION_ID = 1001
        private const val LOCATION_UPDATE_INTERVAL = 5000L // 5 seconds
        private const val LOCATION_FASTEST_INTERVAL = 2000L // 2 seconds

        const val ACTION_START_TRIP = "com.simi.refillme.ACTION_START_TRIP"
        const val ACTION_STOP_TRIP = "com.simi.refillme.ACTION_STOP_TRIP"
        const val EXTRA_VEHICLE_ID = "vehicle_id"
        const val EXTRA_DEVICE_ADDRESS = "device_address"
    }
}
