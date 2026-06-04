package com.simi.refillme.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

class LocationHelper(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission()) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        
        try {
            val cancellationTokenSource = CancellationTokenSource()
            
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                continuation.resume(location)
            }.addOnFailureListener {
                continuation.resume(null)
            }
            
            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        } catch (e: SecurityException) {
            continuation.resume(null)
        }
    }
    
    suspend fun getAddressFromLocation(location: Location): String? = suspendCancellableCoroutine { continuation ->
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                    val address = addresses.firstOrNull()
                    continuation.resume(formatAddress(address))
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                continuation.resume(formatAddress(addresses?.firstOrNull()))
            }
        } catch (e: Exception) {
            continuation.resume(null)
        }
    }
    
    private fun formatAddress(address: android.location.Address?): String? {
        return address?.let {
            // Try to find gas station or petrol station in the address
            val subLocality = address.subLocality
            val thoroughfare = address.thoroughfare // Street name
            val featureName = address.featureName
            
            // Build a meaningful address for filling station
            when {
                featureName != null && (featureName.contains("Shell", ignoreCase = true) ||
                        featureName.contains("Total", ignoreCase = true) ||
                        featureName.contains("Petrol", ignoreCase = true) ||
                        featureName.contains("Gas", ignoreCase = true)) -> featureName
                
                thoroughfare != null && subLocality != null -> "$thoroughfare, $subLocality"
                thoroughfare != null -> thoroughfare
                subLocality != null -> subLocality
                else -> address.getAddressLine(0)
            }
        }
    }
    
    suspend fun detectFillingStation(): String? {
        val location = getCurrentLocation() ?: return null
        return getAddressFromLocation(location) ?: 
            "Lat: ${String.format("%.6f", location.latitude)}, Lng: ${String.format("%.6f", location.longitude)}"
    }
    
    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
}
