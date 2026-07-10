package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.style.TextAlign
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.ActivityEntity
import com.example.ui.viewmodel.ConnectViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KathmanduMapTab(
    viewModel: ConnectViewModel,
    onNavigateToDetails: (Int) -> Unit
) {
    val context = LocalContext.current
    val allActivities by viewModel.kathmanduActivities.collectAsStateWithLifecycle()
    
    val mapSource by viewModel.mapSource.collectAsStateWithLifecycle()
    val googleMapsApiKey by viewModel.googleMapsApiKey.collectAsStateWithLifecycle()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    
    // Filtering categories aligned with the categories of activities
    val categories = listOf("All", "Outdoor", "Sports", "Food & Cafés", "Drinks & Nightlife", "Meet People")
    
    // Filter activities based on search and category
    val filteredActivities = remember(allActivities, searchQuery, selectedCategory) {
        allActivities.filter { activity ->
            val matchesCategory = selectedCategory == "All" || activity.category == selectedCategory
            val matchesSearch = activity.title.contains(searchQuery, ignoreCase = true) ||
                    activity.location.contains(searchQuery, ignoreCase = true) ||
                    activity.subCategory.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    // Map control state
    var scale by remember { mutableStateOf(1.2f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    
    var selectedActivity by remember { mutableStateOf<ActivityEntity?>(null) }
    
    // Map projection reference (Kathmandu central coordinates)
    val mapCenterLat = 27.7172
    val mapCenterLng = 85.3240
    
    // Projection scale factors (pixels per degree)
    val latScaleFactor = -6500f // negative because Latitude increases upwards, Canvas Y increases downwards
    val lngScaleFactor = 6500f  // positive because Longitude increases rightwards, Canvas X increases rightwards

    val textMeasurer = rememberTextMeasurer()
    val isDark = with(MaterialTheme.colorScheme.background) { red + green + blue } < 1.2f
    
    // Styling colors for map canvas
    val riverColor = if (isDark) Color(0xFF1E2E3D) else Color(0xFFD4E6F1)
    val ringRoadColor = if (isDark) Color(0xFF2C303B) else Color(0xFFE2E8F0)
    val mainRoadColor = if (isDark) Color(0xFF22262F) else Color(0xFFEDF2F7)
    val gridColor = if (isDark) Color(0xFF161A23) else Color(0xFFF1F5F9)
    val labelColor = if (isDark) Color(0xFF8A94A6) else Color(0xFF64748B)

    // Layout
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        
        val centerOfContainerX = widthPx / 2f
        val centerOfContainerY = heightPx / 2f

        // Helper functions for projecting coordinate onto screen space
        fun getProjectedCoordinates(lat: Double, lng: Double): Offset {
            val dLat = (lat - mapCenterLat).toFloat()
            val dLng = (lng - mapCenterLng).toFloat()
            
            val rawX = dLng * lngScaleFactor
            val rawY = dLat * latScaleFactor
            
            val x = centerOfContainerX + (rawX * scale) + offsetX
            val y = centerOfContainerY + (rawY * scale) + offsetY
            return Offset(x, y)
        }

        val isOfflineCanvas = mapSource.startsWith("Vector Canvas")

        if (isOfflineCanvas) {
            // 1. Map Drawing Canvas (Pannable & Zoomable)
            Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.6f, 4.0f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
                .testTag("kathmandu_map_canvas")
        ) {
            // Draw background grid lines
            val gridSize = 120f * scale
            val gridStartOfX = (offsetX % gridSize)
            val gridStartOfY = (offsetY % gridSize)
            
            // Draw vertical grid lines
            var currentGridX = gridStartOfX
            while (currentGridX < size.width) {
                if (currentGridX > 0) {
                    drawLine(
                        color = gridColor,
                        start = Offset(currentGridX, 0f),
                        end = Offset(currentGridX, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                currentGridX += gridSize
            }
            
            // Draw horizontal grid lines
            var currentGridY = gridStartOfY
            while (currentGridY < size.height) {
                if (currentGridY > 0) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, currentGridY),
                        end = Offset(size.width, currentGridY),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                currentGridY += gridSize
            }

            // Draw Bagmati River (horizontal wavy curve around Lat 27.68)
            val bagmatiPoints = listOf(
                getProjectedCoordinates(27.676, 85.250),
                getProjectedCoordinates(27.678, 85.280),
                getProjectedCoordinates(27.681, 85.300),
                getProjectedCoordinates(27.679, 85.315),
                getProjectedCoordinates(27.683, 85.335),
                getProjectedCoordinates(27.680, 85.360),
                getProjectedCoordinates(27.685, 85.400),
                getProjectedCoordinates(27.682, 85.450)
            )
            val riverPath = Path().apply {
                val start = bagmatiPoints.first()
                moveTo(start.x, start.y)
                for (i in 1 until bagmatiPoints.size) {
                    val p = bagmatiPoints[i]
                    lineTo(p.x, p.y)
                }
            }
            drawPath(
                path = riverPath,
                color = riverColor,
                style = Stroke(width = 10.dp.toPx() * scale, cap = StrokeCap.Round)
            )

            // Draw Vishnumati River (runs North to South on West around Lng 85.30)
            val vishnumatiPoints = listOf(
                getProjectedCoordinates(27.760, 85.301),
                getProjectedCoordinates(27.740, 85.302),
                getProjectedCoordinates(27.720, 85.297),
                getProjectedCoordinates(27.700, 85.298),
                getProjectedCoordinates(27.681, 85.300) // joins Bagmati
            )
            val vishnumatiPath = Path().apply {
                val start = vishnumatiPoints.first()
                moveTo(start.x, start.y)
                for (i in 1 until vishnumatiPoints.size) {
                    val p = vishnumatiPoints[i]
                    lineTo(p.x, p.y)
                }
            }
            drawPath(
                path = vishnumatiPath,
                color = riverColor,
                style = Stroke(width = 6.dp.toPx() * scale, cap = StrokeCap.Round)
            )

            // Draw Ring Road Outline (Capsule wrapping the city center)
            val ringRoadPoints = listOf(
                getProjectedCoordinates(27.740, 85.310), // North West
                getProjectedCoordinates(27.743, 85.340), // North East
                getProjectedCoordinates(27.710, 85.360), // East
                getProjectedCoordinates(27.675, 85.345), // South East
                getProjectedCoordinates(27.674, 85.310), // South West
                getProjectedCoordinates(27.685, 85.285), // South West Outer
                getProjectedCoordinates(27.712, 85.282), // West
                getProjectedCoordinates(27.740, 85.310)  // Close loop
            )
            val ringRoadPath = Path().apply {
                val start = ringRoadPoints.first()
                moveTo(start.x, start.y)
                for (i in 1 until ringRoadPoints.size) {
                    val p = ringRoadPoints[i]
                    lineTo(p.x, p.y)
                }
            }
            drawPath(
                path = ringRoadPath,
                color = ringRoadColor,
                style = Stroke(width = 8.dp.toPx() * scale, cap = StrokeCap.Round)
            )

            // Draw Main Roads inside the city (Thamel / Durbar Marg / Baneshwor / Putalisadak)
            // Kanti Path (North to South through center)
            drawLine(
                color = mainRoadColor,
                start = getProjectedCoordinates(27.730, 85.315),
                end = getProjectedCoordinates(27.690, 85.315),
                strokeWidth = 4.dp.toPx() * scale,
                cap = StrokeCap.Round
            )
            // Durbar Marg to Baneshwor / Tinkune
            val baneshworRoad = Path().apply {
                val p1 = getProjectedCoordinates(27.712, 85.318)
                val p2 = getProjectedCoordinates(27.702, 85.326)
                val p3 = getProjectedCoordinates(27.691, 85.343)
                val p4 = getProjectedCoordinates(27.685, 85.352)
                moveTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                lineTo(p3.x, p3.y)
                lineTo(p4.x, p4.y)
            }
            drawPath(
                path = baneshworRoad,
                color = mainRoadColor,
                style = Stroke(width = 4.dp.toPx() * scale, cap = StrokeCap.Round)
            )

            // Draw major landmarks text labels
            val landmarks = listOf(
                Triple("Thamel", 27.7144, 85.3121),
                Triple("Patan", 27.6727, 85.3253),
                Triple("Baluwatar", 27.7241, 85.3332),
                Triple("Boudha", 27.7186, 85.3614),
                Triple("Shivapuri Peak", 27.7788, 85.3621),
                Triple("Kirtipur", 27.6791, 85.2764),
                Triple("Baneshwor", 27.6912, 85.3421),
                Triple("Kalimati", 27.6980, 85.2890),
                Triple("Chabahil", 27.7170, 85.3480)
            )

            landmarks.forEach { (name, lat, lng) ->
                val offset = getProjectedCoordinates(lat, lng)
                val textLayout = textMeasurer.measure(
                    text = name,
                    style = TextStyle(
                        color = labelColor.copy(alpha = 0.75f),
                        fontSize = (11f * scale).coerceIn(10f, 16f).sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                // Offset text to center horizontally above the location
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(offset.x - textLayout.size.width / 2f, offset.y - textLayout.size.height / 2f)
                )
            }
        }

        // 2. Render Interactive Activity Pins Over Map
        filteredActivities.forEach { activity ->
            val pinOffset = getProjectedCoordinates(activity.latitude, activity.longitude)
            val isSelected = selectedActivity?.id == activity.id
            
            // Map category to a gorgeous color
            val pinColor = when (activity.category) {
                "Food & Cafés" -> Color(0xFFFF9800)
                "Drinks & Nightlife" -> Color(0xFFE91E63)
                "Events" -> Color(0xFF9C27B0)
                "Sports" -> Color(0xFF4CAF50)
                "Outdoor" -> Color(0xFF03A9F4)
                else -> MaterialTheme.colorScheme.primary
            }

            val categoryIcon = when (activity.category) {
                "Food & Cafés" -> Icons.Default.Restaurant
                "Drinks & Nightlife" -> Icons.Default.LocalBar
                "Events" -> Icons.Default.Event
                "Sports" -> Icons.Default.SportsSoccer
                "Outdoor" -> Icons.Default.Terrain
                else -> Icons.Default.LocationOn
            }

            // Box wrapping the pin
            Box(
                modifier = Modifier
                    .offset(
                        x = with(LocalDensity.current) { (pinOffset.x - 22.dp.toPx()).toDp() },
                        y = with(LocalDensity.current) { (pinOffset.y - 44.dp.toPx()).toDp() }
                    )
                    .size(44.dp)
                    .clickable {
                        selectedActivity = activity
                        // Center the map view on the clicked activity with a smooth spring transition
                        offsetX = centerOfContainerX - ( (activity.longitude - mapCenterLng).toFloat() * lngScaleFactor * scale )
                        offsetY = centerOfContainerY - ( (activity.latitude - mapCenterLat).toFloat() * latScaleFactor * scale )
                    }
                    .testTag("map_pin_${activity.id}"),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Pin background and pulse if selected
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(pinColor.copy(alpha = 0.25f), CircleShape)
                            .border(1.5.dp, pinColor, CircleShape)
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .shadow(elevation = if (isSelected) 8.dp else 4.dp, shape = RoundedCornerShape(12.dp))
                            .background(if (isSelected) pinColor else MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                            .border(1.5.dp, pinColor, RoundedCornerShape(12.dp))
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = activity.category,
                            tint = if (isSelected) Color.White else pinColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    // Pin stem
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(6.dp)
                            .background(pinColor)
                    )
                }
            }
        }
        } else {
            // Real Map WebView rendering
            InteractiveWebViewMap(
                mapSource = mapSource,
                googleMapsApiKey = googleMapsApiKey,
                activities = filteredActivities,
                selectedActivity = selectedActivity,
                onActivitySelected = { selectedActivity = it },
                modifier = Modifier.fillMaxSize().testTag("interactive_webview_map")
            )
        }

        // Self-Serve Key quick config overlay if Google Maps is chosen but empty key
        if (mapSource == "Google Maps (Self-Serve)" && googleMapsApiKey.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Google Maps API Key Required",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "To protect your privacy, Connect supports Self-Serve API configuration. Enter your own Google Maps API Key to load accurate satellite and street layouts, or switch to OpenStreetMap.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        var apiKeyInput by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            label = { Text("Enter Google Maps API Key") },
                            placeholder = { Text("AIzaSy...") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("map_api_key_quick_input")
                        )

                        Button(
                            onClick = {
                                if (apiKeyInput.isNotBlank()) {
                                    viewModel.updateGoogleMapsApiKey(apiKeyInput)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Save Key & Load Map", fontWeight = FontWeight.Bold)
                        }

                        TextButton(
                            onClick = {
                                viewModel.updateMapSource("OpenStreetMap (Real)")
                            }
                        ) {
                            Text("Use OpenStreetMap (No Key Required)")
                        }
                    }
                }
            }
        }

        // 3. Float Top Search & Filters Panel
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp)),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search Kathmandu activities...", fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("map_search_field"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent
                        )
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                }
            }

            // Category Filters horizontal scroll
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isCatSelected = selectedCategory == cat
                    val categoryColor = when (cat) {
                        "Food & Cafés" -> Color(0xFFFF9800)
                        "Drinks & Nightlife" -> Color(0xFFE91E63)
                        "Events" -> Color(0xFF9C27B0)
                        "Sports" -> Color(0xFF4CAF50)
                        "Outdoor" -> Color(0xFF03A9F4)
                        else -> MaterialTheme.colorScheme.primary
                    }
                    
                    Box(
                        modifier = Modifier
                            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp))
                            .background(
                                color = if (isCatSelected) categoryColor else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("map_category_chip_$cat")
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isCatSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 4. Floating Compass, Zoom and Recenter Controls (Bottom-Right)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = if (selectedActivity != null) 190.dp else 24.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Recenter
            FloatingActionButton(
                onClick = {
                    scale = 1.2f
                    offsetX = 0f
                    offsetY = 0f
                    Toast.makeText(context, "Map centered on Kathmandu", Toast.LENGTH_SHORT).show()
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("map_recenter_btn"),
                shape = CircleShape
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Center Map", modifier = Modifier.size(20.dp))
            }

            // Zoom In
            FloatingActionButton(
                onClick = { scale = (scale * 1.2f).coerceIn(0.6f, 4.0f) },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("map_zoom_in_btn"),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
            }

            // Zoom Out
            FloatingActionButton(
                onClick = { scale = (scale / 1.2f).coerceIn(0.6f, 4.0f) },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("map_zoom_out_btn"),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
            }
        }

        // 5. Activity Detail Card Slide-Up (Bottom Overlay)
        AnimatedVisibility(
            visible = selectedActivity != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            selectedActivity?.let { activity ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, shape = RoundedCornerShape(20.dp))
                        .testTag("map_detail_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Category Badge
                            val badgeColor = when (activity.category) {
                                "Food & Cafés" -> Color(0xFFFF9800)
                                "Drinks & Nightlife" -> Color(0xFFE91E63)
                                "Events" -> Color(0xFF9C27B0)
                                "Sports" -> Color(0xFF4CAF50)
                                "Outdoor" -> Color(0xFF03A9F4)
                                else -> MaterialTheme.colorScheme.primary
                            }
                            Box(
                                modifier = Modifier
                                    .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = activity.subCategory,
                                    color = badgeColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            // Close detail card button
                            IconButton(
                                onClick = { selectedActivity = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close details",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.Top
                        ) {
                            // Cover thumbnail
                            Image(
                                painter = rememberAsyncImagePainter(activity.coverImageUrl),
                                contentDescription = activity.title,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activity.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = activity.location,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Event,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = activity.date,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = activity.time,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Actions Row inside detail card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // View details button
                            Button(
                                onClick = {
                                    onNavigateToDetails(activity.id)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("map_view_details_btn")
                            ) {
                                Text("View Details", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            // Save Toggle Action
                            IconButton(
                                onClick = {
                                    viewModel.toggleSaveActivity(activity.id)
                                    // Local feedback toast
                                    val savedMsg = if (activity.isSaved) "Removed from Saved" else "Saved to Bookmarks!"
                                    Toast.makeText(context, savedMsg, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .testTag("map_save_btn")
                            ) {
                                Icon(
                                    imageVector = if (activity.isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = "Save Activity",
                                    tint = if (activity.isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InteractiveWebViewMap(
    mapSource: String,
    googleMapsApiKey: String,
    activities: List<ActivityEntity>,
    selectedActivity: ActivityEntity?,
    onActivitySelected: (ActivityEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = with(MaterialTheme.colorScheme.background) { red + green + blue } < 1.2f

    // Convert activities to simple JSON structures
    val activitiesJson = remember(activities) {
        activities.map { act ->
            val lat = act.latitude
            val lng = act.longitude
            """{id: ${act.id}, title: "${act.title.replace("\"", "\\\"")}", location: "${act.location.replace("\"", "\\\"")}", lat: $lat, lng: $lng, category: "${act.category}"}"""
        }.joinToString(prefix = "[", postfix = "]")
    }

    val mapHtml = remember(mapSource, googleMapsApiKey, activitiesJson, isDark) {
        val isGoogleMaps = mapSource.startsWith("Google Maps")
        if (isGoogleMaps && googleMapsApiKey.isNotBlank()) {
            // Google Maps JS API with user-supplied key
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="initial-scale=1.0, user-scalable=no" />
                <style type="text/css">
                    html, body, #map { height: 100%; margin: 0; padding: 0; background-color: ${if (isDark) "#121212" else "#ffffff"}; }
                </style>
                <script src="https://maps.googleapis.com/maps/api/js?key=$googleMapsApiKey&callback=initMap" async defer></script>
                <script>
                    var map;
                    var markers = [];
                    var activeInfoWindow = null;

                    function initMap() {
                        var kathmandu = {lat: 27.7172, lng: 85.3240};
                        map = new google.maps.Map(document.getElementById('map'), {
                            zoom: 13,
                            center: kathmandu,
                            styles: ${if (isDark) """[
                                {elementType: 'geometry', stylers: [{color: '#242f3e'}]},
                                {elementType: 'labels.text.stroke', stylers: [{color: '#242f3e'}]},
                                {elementType: 'labels.text.fill', stylers: [{color: '#746855'}]},
                                {
                                  featureType: 'administrative.locality',
                                  elementType: 'labels.text.fill',
                                  stylers: [{color: '#d59563'}]
                                },
                                {
                                  featureType: 'poi',
                                  elementType: 'labels.text.fill',
                                  stylers: [{color: '#d59563'}]
                                },
                                {
                                  featureType: 'poi.park',
                                  elementType: 'geometry',
                                  stylers: [{color: '#263c3f'}]
                                },
                                {
                                  featureType: 'poi.park',
                                  elementType: 'labels.text.fill',
                                  stylers: [{color: '#6b9a76'}]
                                },
                                {
                                  featureType: 'road',
                                  elementType: 'geometry',
                                  stylers: [{color: '#38414e'}]
                                },
                                {
                                  featureType: 'road',
                                  elementType: 'geometry.stroke',
                                  stylers: [{color: '#212a37'}]
                                },
                                {
                                  featureType: 'road.labels.text.fill',
                                  stylers: [{color: '#9ca5b3'}]
                                },
                                {
                                  featureType: 'road.highway',
                                  elementType: 'geometry',
                                  stylers: [{color: '#746855'}]
                                },
                                {
                                  featureType: 'road.highway',
                                  elementType: 'geometry.stroke',
                                  stylers: [{color: '#1f2835'}]
                                },
                                {
                                  featureType: 'road.highway',
                                  elementType: 'labels.text.fill',
                                  stylers: [{color: '#f3d19c'}]
                                },
                                {
                                  featureType: 'transit',
                                  elementType: 'geometry',
                                  stylers: [{color: '#2f3930'}]
                                },
                                {
                                  featureType: 'transit.station',
                                  elementType: 'labels.text.fill',
                                  stylers: [{color: '#d59563'}]
                                },
                                {
                                  featureType: 'water',
                                  elementType: 'geometry',
                                  stylers: [{color: '#17263c'}]
                                },
                                {
                                  featureType: 'water',
                                  elementType: 'labels.text.fill',
                                  stylers: [{color: '#515c6d'}]
                                },
                                {
                                  featureType: 'water',
                                  elementType: 'labels.text.stroke',
                                  stylers: [{color: '#17263c'}]
                                }
                              ]""" else "[]"}
                        });

                        var activities = $activitiesJson;
                        activities.forEach(function(act) {
                            var marker = new google.maps.Marker({
                                position: {lat: act.lat, lng: act.lng},
                                map: map,
                                title: act.title
                            });

                            var contentString = '<div style="color: black; font-family: sans-serif; padding: 4px;">' +
                                '<h3>' + act.title + '</h3>' +
                                '<p>' + act.location + '</p>' +
                                '</div>';

                            var infowindow = new google.maps.InfoWindow({
                                content: contentString
                            });

                            marker.addListener('click', function() {
                                if (activeInfoWindow) {
                                    activeInfoWindow.close();
                                }
                                infowindow.open(map, marker);
                                activeInfoWindow = infowindow;
                                if (window.AndroidBridge) {
                                    window.AndroidBridge.onActivitySelected(act.id);
                                }
                            });
                        });
                    }
                </script>
            </head>
            <body>
                <div id="map"></div>
            </body>
            </html>
            """.trimIndent()
        } else {
            // Leaflet with OpenStreetMap
            val tileUrl = if (isDark) "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png" else "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            val attribution = if (isDark) "© OpenStreetMap contributors © CARTO" else "© OpenStreetMap contributors"

            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="initial-scale=1.0, user-scalable=no, width=device-width" />
                <style type="text/css">
                    html, body, #map { height: 100%; margin: 0; padding: 0; background-color: ${if (isDark) "#121212" else "#ffffff"}; }
                    .leaflet-popup-content-wrapper {
                        background: ${if (isDark) "#1E293B" else "#FFFFFF"};
                        color: ${if (isDark) "#F8FAFC" else "#0F172A"};
                        border-radius: 8px;
                        font-family: system-ui, -apple-system, sans-serif;
                    }
                    .leaflet-popup-tip {
                        background: ${if (isDark) "#1E293B" else "#FFFFFF"};
                    }
                </style>
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    var map = L.map('map', { zoomControl: false }).setView([27.7172, 85.3240], 13);
                    L.control.zoom({ position: 'bottomright' }).addTo(map);

                    L.tileLayer('$tileUrl', {
                        attribution: '$attribution'
                    }).addTo(map);

                    var activities = $activitiesJson;
                    var markerGroup = L.featureGroup().addTo(map);

                    activities.forEach(function(act) {
                        var marker = L.marker([act.lat, act.lng]).addTo(markerGroup);
                        
                        var popupContent = '<div style="padding: 2px;">' +
                            '<strong style="font-size: 14px; display: block; margin-bottom: 4px;">' + act.title + '</strong>' +
                            '<span style="font-size: 12px; opacity: 0.8;">' + act.location + '</span>' +
                            '</div>';
                            
                        marker.bindPopup(popupContent);
                        
                        marker.on('click', function() {
                            if (window.AndroidBridge) {
                                window.AndroidBridge.onActivitySelected(act.id);
                            }
                        });
                    });

                    if (activities.length > 0) {
                        map.fitBounds(markerGroup.getBounds().pad(0.1));
                    }
                </script>
            </body>
            </html>
            """.trimIndent()
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onActivitySelected(id: String) {
                        val intId = id.toIntOrNull() ?: return
                        val found = activities.find { it.id == intId }
                        if (found != null) {
                            onActivitySelected(found)
                        }
                    }
                }, "AndroidBridge")
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://localhost", mapHtml, "text/html", "UTF-8", null)
        },
        modifier = modifier
    )
}
