package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.MockData
import java.util.Locale

data class CityDetails(
    val name: String,
    val description: String,
    val emoji: String,
    val imageUrl: String
)

private val cityDetailsList = listOf(
    CityDetails("Kathmandu", "Capital & temple valley", "🏛️", "https://images.unsplash.com/photo-1544735716-392fe2489ffa?q=80&w=400"),
    CityDetails("Pokhara", "Lakes, mountains & trekking", "🏔️", "https://images.unsplash.com/photo-1571501679680-de32f135a4a0?q=80&w=400"),
    CityDetails("Lalitpur", "City of ancient fine arts", "🎨", "https://images.unsplash.com/photo-1623091426285-4427555e7a46?q=80&w=400"),
    CityDetails("Bhaktapur", "Historic pottery & squares", "🏺", "https://images.unsplash.com/photo-1582510003544-4d00b7f74220?q=80&w=400"),
    CityDetails("Chitwan", "Jungle safaris & wildlife", "🐅", "https://images.unsplash.com/photo-1581888227599-779811939961?q=80&w=400"),
    CityDetails("Biratnagar", "Vibrant industrial gateway", "🏭", "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?q=80&w=400"),
    CityDetails("Butwal", "Birthplace of Buddha gate", "☸️", "https://images.unsplash.com/photo-1609137144814-6d9b047a0641?q=80&w=400"),
    CityDetails("Dharan", "Scenic hills & eastern culture", "⛰️", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=400"),
    CityDetails("Nepalgunj", "Western plains & trade hub", "🕌", "https://images.unsplash.com/photo-1533105079780-92b9be482077?q=80&w=400")
)

private fun getClosestCity(lat: Double, lon: Double): String {
    val cityCoordinates = mapOf(
        "Kathmandu" to Pair(27.7172, 85.3240),
        "Pokhara" to Pair(28.2096, 83.9856),
        "Lalitpur" to Pair(27.6744, 85.3240),
        "Bhaktapur" to Pair(27.6710, 85.4298),
        "Chitwan" to Pair(27.5291, 84.3542),
        "Biratnagar" to Pair(26.4525, 87.2718),
        "Butwal" to Pair(27.6866, 83.4323),
        "Dharan" to Pair(26.8124, 87.2835),
        "Nepalgunj" to Pair(28.0500, 81.6167)
    )
    
    var closestCity = "Kathmandu"
    var minDistance = Double.MAX_VALUE
    
    for ((cityName, coords) in cityCoordinates) {
        val dLat = lat - coords.first
        val dLon = lon - coords.second
        val distSq = dLat * dLat + dLon * dLon
        if (distSq < minDistance) {
            minDistance = distSq
            closestCity = cityName
        }
    }
    return closestCity
}

@Composable
fun OnboardingScreen(
    onCitySelected: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedCity by remember { mutableStateOf("") }
    var isDetecting by remember { mutableStateOf(false) }
    var detectionMethod by remember { mutableStateOf<String?>(null) }
    
    val scrollState = rememberScrollState()

    // Geolocation routine
    fun performGeolocation() {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            Toast.makeText(context, "Location services not available", Toast.LENGTH_SHORT).show()
            isDetecting = false
            return
        }

        val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!hasGps && !hasNetwork) {
            Toast.makeText(context, "Please enable location services in system settings.", Toast.LENGTH_LONG).show()
            isDetecting = false
            return
        }

        try {
            val lastGpsLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNetLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val bestLocation = lastGpsLoc ?: lastNetLoc

            if (bestLocation != null) {
                // Try reverse geocoding
                var cityName: String? = null
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(bestLocation.latitude, bestLocation.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        cityName = address.locality ?: address.subAdminArea ?: address.adminArea
                    }
                } catch (e: Exception) {
                    // Ignore and fallback to coordinates mapping
                }

                val finalCity = if (cityName != null && MockData.cities.contains(cityName)) {
                    cityName
                } else {
                    getClosestCity(bestLocation.latitude, bestLocation.longitude)
                }

                selectedCity = finalCity
                detectionMethod = "Auto-detected via GPS"
                isDetecting = false
                Toast.makeText(context, "Location detected: $finalCity", Toast.LENGTH_SHORT).show()
            } else {
                // Register a temporary listener to get current location
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        val finalCity = getClosestCity(location.latitude, location.longitude)
                        selectedCity = finalCity
                        detectionMethod = "Auto-detected via GPS"
                        isDetecting = false
                        Toast.makeText(context, "Location detected: $finalCity", Toast.LENGTH_SHORT).show()
                        locationManager.removeUpdates(this)
                    }
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                val provider = if (hasNetwork) LocationManager.NETWORK_PROVIDER else LocationManager.GPS_PROVIDER
                locationManager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())

                // Timeout safety
                Handler(Looper.getMainLooper()).postDelayed({
                    if (isDetecting) {
                        isDetecting = false
                        locationManager.removeUpdates(listener)
                        // fallback to Kathmandu default
                        selectedCity = "Kathmandu"
                        detectionMethod = "Default Location"
                        Toast.makeText(context, "Signal weak. Defaulted to Kathmandu.", Toast.LENGTH_SHORT).show()
                    }
                }, 4000)
            }
        } catch (e: SecurityException) {
            isDetecting = false
            Toast.makeText(context, "Location permission missing", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            isDetecting = false
            Toast.makeText(context, "Unable to get location. Select manually.", Toast.LENGTH_SHORT).show()
        }
    }

    // Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            performGeolocation()
        } else {
            isDetecting = false
            Toast.makeText(context, "Permission denied. Please select a city manually.", Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Hero Background Image Header
        Image(
            painter = rememberAsyncImagePainter("https://images.unsplash.com/photo-1544735716-392fe2489ffa?q=80&w=600"),
            contentDescription = "Beautiful Kathmandu",
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.38f),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay for smooth transition to background color
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.39f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.fillMaxHeight(0.24f).height(140.dp))

            // Welcome Text Banner Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(bottom = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Welcome to Connect",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Discover vibrant events, meetups, and local activities. Let's customize your profile with your location to get started.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        ),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }

            Text(
                text = "Choose Your City",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Select your default home city manually or automatically via GPS to browse nearby events.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Geolocation Button (Sleek Capsule pill)
            Button(
                onClick = {
                    isDetecting = true
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        performGeolocation()
                    } else {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("detect_location_button")
                    .padding(bottom = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(28.dp),
                enabled = !isDetecting
            ) {
                if (isDetecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("GPS Geolocating...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Detect Location",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Auto-Detect Location (GPS)", fontWeight = FontWeight.Bold)
                }
            }

            // Auto-Detected Highlight Banner
            if (detectionMethod != null && selectedCity.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Auto-Detected City",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Set to $selectedCity",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Grid Layout of Predefined Cities
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    cityDetailsList.chunked(2).forEach { rowList ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowList.forEach { cityDetails ->
                                val isSelected = selectedCity == cityDetails.name
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 6.dp)
                                        .clickable {
                                            selectedCity = cityDetails.name
                                            detectionMethod = null // Clear GPS banner if manually changed
                                        }
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = cityDetails.emoji,
                                                fontSize = 28.sp
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = cityDetails.name,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = cityDetails.description,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            if (rowList.size == 1) {
                                Box(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Confirm Selection Button
            AnimatedVisibility(
                visible = selectedCity.isNotEmpty(),
                enter = fadeIn() + slideInVertically(animationSpec = spring()) { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                Button(
                    onClick = { if (selectedCity.isNotEmpty()) onCitySelected(selectedCity) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                        .height(56.dp)
                        .testTag("explore_onboarding_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = "Continue with $selectedCity",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
