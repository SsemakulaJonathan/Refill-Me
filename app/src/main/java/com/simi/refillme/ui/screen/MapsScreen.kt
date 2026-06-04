package com.simi.refillme.ui.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.simi.refillme.data.entity.Trip
import com.simi.refillme.data.entity.Refill
import com.simi.refillme.ui.theme.*
import android.util.Log
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import android.graphics.drawable.BitmapDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

// Helper function to fetch route from OSRM API
private suspend fun fetchRoutePoints(startLat: Double, startLng: Double, endLat: Double, endLng: Double): List<GeoPoint> {
    return withContext(Dispatchers.IO) {
        try {
            // Use OSRM public API for routing
            val url = "https://router.project-osrm.org/route/v1/driving/$startLng,$startLat;$endLng,$endLat?overview=full&geometries=geojson"

            Log.d("MapsScreen", "Fetching route: $startLat,$startLng -> $endLat,$endLng")

            val response = URL(url).readText()
            val json = JSONObject(response)

            if (json.getString("code") == "Ok") {
                val routes = json.getJSONArray("routes")
                if (routes.length() > 0) {
                    val geometry = routes.getJSONObject(0).getJSONObject("geometry")
                    val coordinates = geometry.getJSONArray("coordinates")

                    val points = mutableListOf<GeoPoint>()
                    for (i in 0 until coordinates.length()) {
                        val coord = coordinates.getJSONArray(i)
                        val lng = coord.getDouble(0)
                        val lat = coord.getDouble(1)
                        points.add(GeoPoint(lat, lng))
                    }

                    Log.d("MapsScreen", "Route fetched: ${points.size} waypoints")
                    return@withContext points
                }
            }

            Log.w("MapsScreen", "Failed to fetch route, falling back to straight line")
            // Fallback to straight line
            listOf(GeoPoint(startLat, startLng), GeoPoint(endLat, endLng))
        } catch (e: Exception) {
            Log.e("MapsScreen", "Error fetching route: ${e.message}", e)
            // Fallback to straight line on error
            listOf(GeoPoint(startLat, startLng), GeoPoint(endLat, endLng))
        }
    }
}

// Helper function to create colored marker icons
private fun createMarkerIcon(context: Context, color: Int, isStart: Boolean, isActive: Boolean = false): BitmapDrawable {
    val size = if (isActive) 72 else 48 // Larger for active trips
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint().apply {
        this.color = color
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    val centerX = size / 2f
    val centerY = size / 2f
    val mainRadius = size / 3f

    // For active trip, draw pulsing outer ring
    if (isActive) {
        paint.color = AndroidColor.argb(80, 255, 152, 0) // Semi-transparent orange
        paint.style = Paint.Style.FILL
        canvas.drawCircle(centerX, centerY, mainRadius * 1.5f, paint)

        paint.color = AndroidColor.argb(120, 255, 152, 0)
        canvas.drawCircle(centerX, centerY, mainRadius * 1.25f, paint)
    }

    // Draw main circle marker
    paint.color = color
    paint.style = Paint.Style.FILL
    canvas.drawCircle(centerX, centerY, mainRadius, paint)

    // Draw white border
    paint.color = AndroidColor.WHITE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = if (isActive) 4f else 3f
    canvas.drawCircle(centerX, centerY, mainRadius, paint)

    // Draw inner symbol
    paint.color = AndroidColor.WHITE
    paint.style = Paint.Style.FILL
    paint.textSize = if (isActive) size / 2.2f else size / 2.5f
    paint.textAlign = Paint.Align.CENTER

    if (isActive && isStart) {
        // Draw "LIVE" text for active trip
        paint.textSize = size / 5f
        paint.isFakeBoldText = true
        canvas.drawText("LIVE", centerX, centerY + size / 12f, paint)
    } else if (isStart) {
        canvas.drawText("S", centerX, centerY + size / 7f, paint)
    } else {
        canvas.drawText("E", centerX, centerY + size / 7f, paint)
    }

    return BitmapDrawable(context.resources, bitmap)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapsScreen(
    trips: List<Trip>,
    activeTrip: Trip?,
    refills: List<Refill>,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Initialize osmdroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    // Debug logging
    Log.d("MapsScreen", "=== MAPS SCREEN LOADED ===")
    Log.d("MapsScreen", "Total trips received: ${trips.size}")
    Log.d("MapsScreen", "Active trip: ${activeTrip?.id}")

    LaunchedEffect(trips.size) {
        val tripsWithCoords = trips.count { it.startLatitude != null && it.startLongitude != null }
        Toast.makeText(context, "Trips with location: $tripsWithCoords / ${trips.size}", Toast.LENGTH_LONG).show()
    }

    trips.forEachIndexed { index, trip ->
        Log.d("MapsScreen", "Trip $index: id=${trip.id}, startLat=${trip.startLatitude}, startLng=${trip.startLongitude}, endLat=${trip.endLatitude}, endLng=${trip.endLongitude}, distance=${trip.distanceKm}")
    }

    var showTrips by remember { mutableStateOf(true) }

    // State to hold fetched routes and loading status
    var routesCache by remember { mutableStateOf<Map<String, List<GeoPoint>>>(emptyMap()) }
    var routesFetchProgress by remember { mutableStateOf(0) }
    var totalRoutesToFetch by remember { mutableStateOf(0) }

    // Initialize routes with straight lines immediately, then fetch real routes
    LaunchedEffect(trips) {
        if (trips.isNotEmpty()) {
            // Count routes to fetch
            totalRoutesToFetch = trips.count {
                it.startLatitude != null && it.startLongitude != null &&
                it.endLatitude != null && it.endLongitude != null
            }
            routesFetchProgress = 0

            // Initialize with straight lines immediately
            val initialRoutes = mutableMapOf<String, List<GeoPoint>>()
            trips.forEach { trip ->
                if (trip.startLatitude != null && trip.startLongitude != null &&
                    trip.endLatitude != null && trip.endLongitude != null) {
                    val key = "${trip.id}"
                    initialRoutes[key] = listOf(
                        GeoPoint(trip.startLatitude, trip.startLongitude),
                        GeoPoint(trip.endLatitude, trip.endLongitude)
                    )
                }
            }
            routesCache = initialRoutes

            // Now fetch real routes in background and update progressively
            val finalRoutes = mutableMapOf<String, List<GeoPoint>>()
            var fetchedCount = 0

            trips.forEach { trip ->
                if (trip.startLatitude != null && trip.startLongitude != null &&
                    trip.endLatitude != null && trip.endLongitude != null) {

                    val key = "${trip.id}"
                    val routePoints = fetchRoutePoints(
                        trip.startLatitude,
                        trip.startLongitude,
                        trip.endLatitude,
                        trip.endLongitude
                    )
                    finalRoutes[key] = routePoints
                    fetchedCount++
                    routesFetchProgress = fetchedCount

                    // Update cache progressively so routes appear as they're fetched
                    routesCache = routesCache + (key to routePoints)

                    Log.d("MapsScreen", "Fetched route $fetchedCount/$totalRoutesToFetch for trip ${trip.id}: ${routePoints.size} points")
                }
            }

            Log.d("MapsScreen", "All routes fetched: ${finalRoutes.size} routes cached")
        }
    }

    // Get all locations for initial camera position
    val allLocations = remember(trips) {
        val locations = mutableListOf<GeoPoint>()

        Log.d("MapsScreen", "=== Processing ${trips.size} trips for map display ===")

        var tripsWithStartCoords = 0
        var tripsWithEndCoords = 0

        trips.forEachIndexed { index, trip ->
            trip.startLatitude?.let { lat ->
                trip.startLongitude?.let { lng ->
                    Log.d("MapsScreen", "✓ Adding start location for trip $index: $lat, $lng")
                    locations.add(GeoPoint(lat, lng))
                    tripsWithStartCoords++
                }
            }
            trip.endLatitude?.let { lat ->
                trip.endLongitude?.let { lng ->
                    Log.d("MapsScreen", "✓ Adding end location for trip $index: $lat, $lng")
                    locations.add(GeoPoint(lat, lng))
                    tripsWithEndCoords++
                }
            }
        }

        Log.d("MapsScreen", "=== Map Data Summary ===")
        Log.d("MapsScreen", "Total trips: ${trips.size}")
        Log.d("MapsScreen", "Trips with start coordinates: $tripsWithStartCoords")
        Log.d("MapsScreen", "Trips with end coordinates: $tripsWithEndCoords")
        Log.d("MapsScreen", "Total locations to display: ${locations.size}")

        locations
    }

    // Calculate center position - prioritize active trip if available
    val centerPosition = remember(allLocations, activeTrip) {
        // If there's an active trip, center on its start position
        if (activeTrip?.startLatitude != null && activeTrip.startLongitude != null) {
            Log.d("MapsScreen", "Centering on active trip: ${activeTrip.startLatitude}, ${activeTrip.startLongitude}")
            GeoPoint(activeTrip.startLatitude, activeTrip.startLongitude)
        } else if (allLocations.isNotEmpty()) {
            val avgLat = allLocations.map { it.latitude }.average()
            val avgLng = allLocations.map { it.longitude }.average()
            Log.d("MapsScreen", "Center position calculated: $avgLat, $avgLng")
            GeoPoint(avgLat, avgLng)
        } else {
            Log.d("MapsScreen", "Using default center position (Kampala)")
            GeoPoint(0.3476, 32.5825) // Kampala, Uganda default
        }
    }

    // Higher zoom level for active trip
    val initialZoom = remember(activeTrip) {
        if (activeTrip != null) 15.0 else 12.0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Maps") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = OnPrimary,
                    navigationIconContentColor = OnPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Show map or empty state
            if (allLocations.isEmpty()) {
                // Empty state when no location data
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No Trip Data Available",
                        style = MaterialTheme.typography.titleLarge,
                        color = OnSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Start tracking trips with the Auto Trip Logging feature to see your routes on the map.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Trips found: ${trips.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        "Trips with location: ${trips.count { it.startLatitude != null && it.startLongitude != null }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                // Show OpenStreetMap
                Log.d("MapsScreen", "Rendering OpenStreetMap with ${allLocations.size} locations")

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)

                            // Set initial position and zoom
                            controller.setZoom(initialZoom)
                            controller.setCenter(centerPosition)

                            // Create custom marker icons
                            val startIcon = createMarkerIcon(ctx, AndroidColor.rgb(76, 175, 80), isStart = true, isActive = false) // Green
                            val endIcon = createMarkerIcon(ctx, AndroidColor.rgb(244, 67, 54), isStart = false, isActive = false)   // Red
                            val activeStartIcon = createMarkerIcon(ctx, AndroidColor.rgb(255, 152, 0), isStart = true, isActive = true) // Orange LIVE

                            // Add markers and polylines for trips
                            if (showTrips) {
                                trips.forEach { trip ->
                                    val isActiveTrip = activeTrip?.id == trip.id

                                    // Draw polyline FIRST (so it appears under markers)
                                    if (trip.startLatitude != null && trip.startLongitude != null &&
                                        trip.endLatitude != null && trip.endLongitude != null) {

                                        // Get cached route (either straight line or fetched route)
                                        routesCache["${trip.id}"]?.let { routePoints ->
                                            val polyline = Polyline(this).apply {
                                                // Add all route points to follow roads
                                                routePoints.forEach { point ->
                                                    addPoint(point)
                                                }
                                                outlinePaint.apply {
                                                    // Active trip: Orange, Completed trips: Blue
                                                    color = if (isActiveTrip) {
                                                        AndroidColor.rgb(255, 152, 0) // Orange for active
                                                    } else {
                                                        AndroidColor.rgb(33, 150, 243) // Blue for completed
                                                    }
                                                    strokeWidth = if (isActiveTrip) 7f else 5f // Thicker for active
                                                    alpha = if (isActiveTrip) 255 else 200 // More opaque for active
                                                }
                                            }
                                            overlays.add(polyline)
                                        }
                                    }

                                    // Add start marker - use LIVE marker for active trip
                                    trip.startLatitude?.let { startLat ->
                                        trip.startLongitude?.let { startLng ->
                                            val startMarker = Marker(this).apply {
                                                position = GeoPoint(startLat, startLng)
                                                title = if (isActiveTrip) "🔴 ACTIVE TRIP" else "Trip Start"
                                                snippet = if (isActiveTrip) {
                                                    "In Progress - ${"%.2f".format(trip.distanceKm)} km"
                                                } else {
                                                    "Distance: ${"%.2f".format(trip.distanceKm)} km"
                                                }
                                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                                icon = if (isActiveTrip) activeStartIcon else startIcon
                                            }
                                            overlays.add(startMarker)
                                        }
                                    }

                                    // Add end marker (Red with "E")
                                    trip.endLatitude?.let { endLat ->
                                        trip.endLongitude?.let { endLng ->
                                            val endMarker = Marker(this).apply {
                                                position = GeoPoint(endLat, endLng)
                                                title = "Trip End"
                                                snippet = "Duration: ${trip.durationMinutes} min"
                                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                                icon = endIcon
                                            }
                                            overlays.add(endMarker)
                                        }
                                    }
                                }
                            }

                            invalidate()
                        }
                    },
                    update = { mapView ->
                        // Update map when trips change
                        mapView.overlays.clear()

                        // Create custom marker icons
                        val startIcon = createMarkerIcon(mapView.context, AndroidColor.rgb(76, 175, 80), isStart = true, isActive = false)
                        val endIcon = createMarkerIcon(mapView.context, AndroidColor.rgb(244, 67, 54), isStart = false, isActive = false)
                        val activeStartIcon = createMarkerIcon(mapView.context, AndroidColor.rgb(255, 152, 0), isStart = true, isActive = true)

                        if (showTrips) {
                            trips.forEach { trip ->
                                val isActiveTrip = activeTrip?.id == trip.id

                                // Draw polyline FIRST (so it appears under markers)
                                if (trip.startLatitude != null && trip.startLongitude != null &&
                                    trip.endLatitude != null && trip.endLongitude != null) {

                                    // Get cached route (either straight line or fetched route)
                                    routesCache["${trip.id}"]?.let { routePoints ->
                                        val polyline = Polyline(mapView).apply {
                                            // Add all route points to follow roads
                                            routePoints.forEach { point ->
                                                addPoint(point)
                                            }
                                            outlinePaint.apply {
                                                // Active trip: Orange, Completed trips: Blue
                                                color = if (isActiveTrip) {
                                                    AndroidColor.rgb(255, 152, 0) // Orange for active
                                                } else {
                                                    AndroidColor.rgb(33, 150, 243) // Blue for completed
                                                }
                                                strokeWidth = if (isActiveTrip) 7f else 5f // Thicker for active
                                                alpha = if (isActiveTrip) 255 else 200 // More opaque for active
                                            }
                                        }
                                        mapView.overlays.add(polyline)
                                    }
                                }

                                // Add start marker - use LIVE marker for active trip
                                trip.startLatitude?.let { startLat ->
                                    trip.startLongitude?.let { startLng ->
                                        val startMarker = Marker(mapView).apply {
                                            position = GeoPoint(startLat, startLng)
                                            title = if (isActiveTrip) "🔴 ACTIVE TRIP" else "Trip Start"
                                            snippet = if (isActiveTrip) {
                                                "In Progress - ${"%.2f".format(trip.distanceKm)} km"
                                            } else {
                                                "Distance: ${"%.2f".format(trip.distanceKm)} km"
                                            }
                                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                            icon = if (isActiveTrip) activeStartIcon else startIcon
                                        }
                                        mapView.overlays.add(startMarker)
                                    }
                                }

                                // Add end marker (Red with "E")
                                trip.endLatitude?.let { endLat ->
                                    trip.endLongitude?.let { endLng ->
                                        val endMarker = Marker(mapView).apply {
                                            position = GeoPoint(endLat, endLng)
                                            title = "Trip End"
                                            snippet = "Duration: ${trip.durationMinutes} min"
                                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                            icon = endIcon
                                        }
                                        mapView.overlays.add(endMarker)
                                    }
                                }
                            }
                        }

                        mapView.invalidate()
                    }
                )
            }

            // Loading indicator showing progress
            if (routesFetchProgress > 0 && routesFetchProgress < totalRoutesToFetch) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Primary
                        )
                        Text(
                            text = "Loading routes $routesFetchProgress/$totalRoutesToFetch",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurface
                        )
                    }
                }
            }

            // Legend and controls (show on top of map)
            if (allLocations.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Map Legend",
                            style = MaterialTheme.typography.titleSmall,
                            color = OnSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Use actual marker icon in legend
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
                                        val radius = size.width / 3f
                                        // Green circle
                                        drawCircle(
                                            color = Color(0xFF4CAF50),
                                            radius = radius
                                        )
                                        // White border
                                        drawCircle(
                                            color = Color.White,
                                            radius = radius,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                                        )
                                    }
                                    Text(
                                        text = "S",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontSize = 10.sp
                                    )
                                }
                                Text("Trip Start", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = showTrips,
                                onCheckedChange = { showTrips = it },
                                modifier = Modifier.height(24.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Use actual marker icon in legend
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
                                        val radius = size.width / 3f
                                        // Red circle
                                        drawCircle(
                                            color = Color(0xFFF44336),
                                            radius = radius
                                        )
                                        // White border
                                        drawCircle(
                                            color = Color.White,
                                            radius = radius,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                                        )
                                    }
                                    Text(
                                        text = "E",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontSize = 10.sp
                                    )
                                }
                                Text("Trip End", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        // Completed trip route legend
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(3.dp)
                                    .background(Color(0xFF2196F3))
                            )
                            Text("Completed Route", style = MaterialTheme.typography.bodySmall)
                        }

                        // Active trip route legend
                        if (activeTrip != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(4.dp)
                                        .background(Color(0xFFFF9800)) // Orange
                                )
                                Text("Active Trip", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = "Powered by OpenStreetMap",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
