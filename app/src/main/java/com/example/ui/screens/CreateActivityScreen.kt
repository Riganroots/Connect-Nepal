package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MockData
import com.example.ui.viewmodel.ConnectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateActivityScreen(
    viewModel: ConnectViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val categories = listOf(
        "Food & Cafés",
        "Drinks & Nightlife",
        "Events",
        "Sports",
        "Outdoor",
        "Meet People"
    )

    // Form states
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var subCategory by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf("Kathmandu") }
    var location by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var maxParticipants by remember { mutableStateOf(10) }
    var isFree by remember { mutableStateOf(true) }
    var costValue by remember { mutableStateOf("") }
    var meetingPoint by remember { mutableStateOf("") }
    var visibilityPublic by remember { mutableStateOf(true) }

    // Dropdown/Menu toggles
    var categoryExpanded by remember { mutableStateOf(false) }
    var cityExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Activity", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp)
        ) {
            // Title
            Text(
                text = "Share what you'd like to do with others in your city.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Section 1: Core Details
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Activity Title") },
                placeholder = { Text("e.g. Saturday Futsal, Cafe Hopping, etc.") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("activity_title_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Activity Description") },
                placeholder = { Text("Describe the plan, what you will do, what to bring...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .testTag("activity_desc_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Category Selector
            Text("Category", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Choose Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                selectedCategory = cat
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = subCategory,
                onValueChange = { subCategory = it },
                label = { Text("Subcategory (e.g. Futsal, Hiking, Cafe)") },
                placeholder = { Text("e.g. Board Games, Street Food, Live Band") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // City Selection
            Text("Target City", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = cityExpanded,
                onExpandedChange = { cityExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedCity,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Choose City") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = cityExpanded,
                    onDismissRequest = { cityExpanded = false }
                ) {
                    MockData.cities.forEach { city ->
                        DropdownMenuItem(
                            text = { Text(city) },
                            onClick = {
                                selectedCity = city
                                cityExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Specific Location") },
                placeholder = { Text("e.g. Jhamsikhel, Patan, Sauraha, etc.") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Time and Date
            Text("Schedule", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date") },
                    placeholder = { Text("e.g. Saturday, Jul 11") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Time") },
                    placeholder = { Text("e.g. 03:00 PM") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Max Participants Selector
            Text("Participants Limit", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = { if (maxParticipants > 2) maxParticipants-- },
                    modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease limit")
                }
                
                Text(
                    text = "$maxParticipants Participants max",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )

                IconButton(
                    onClick = { if (maxParticipants < 100) maxParticipants++ },
                    modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase limit")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Cost Settings
            Text("Cost Details", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = isFree,
                    onClick = { isFree = true }
                )
                Text(
                    text = "Free",
                    modifier = Modifier.clickable { isFree = true },
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(20.dp))
                RadioButton(
                    selected = !isFree,
                    onClick = { isFree = false }
                )
                Text(
                    text = "Paid / Share Cost",
                    modifier = Modifier.clickable { isFree = false },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            AnimatedVisibility(visible = !isFree) {
                OutlinedTextField(
                    value = costValue,
                    onValueChange = { costValue = it },
                    label = { Text("Cost Information (e.g. Rs. 500, split field cost)") },
                    placeholder = { Text("e.g. Rs. 350 for coffee/lunch") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Meeting Point
            OutlinedTextField(
                value = meetingPoint,
                onValueChange = { meetingPoint = it },
                label = { Text("Meeting Point") },
                placeholder = { Text("Where exactly will everyone assemble?") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Visibility
            Text("Visibility", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = visibilityPublic,
                    onClick = { visibilityPublic = true }
                )
                Text(
                    text = "Public (Anyone can discover)",
                    modifier = Modifier.clickable { visibilityPublic = true },
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = !visibilityPublic,
                    onClick = { visibilityPublic = false }
                )
                Text(
                    text = "Friends Only",
                    modifier = Modifier.clickable { visibilityPublic = false },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Submit Button
            Button(
                onClick = {
                    if (title.isBlank() || description.isBlank() || location.isBlank() || date.isBlank() || time.isBlank() || meetingPoint.isBlank()) {
                        Toast.makeText(context, "Please fill out all required fields!", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.createNewActivity(
                            title = title,
                            description = description,
                            category = selectedCategory,
                            subCategory = subCategory.ifBlank { selectedCategory.split(" ").first() },
                            city = selectedCity,
                            location = location,
                            date = date,
                            time = time,
                            maxParticipants = maxParticipants,
                            cost = if (isFree) "Free" else costValue.ifBlank { "Paid" },
                            meetingPoint = meetingPoint,
                            visibility = if (visibilityPublic) "Public" else "Friends",
                            coverImageUrl = "" // Automatically set inside ViewModel based on Category!
                        )
                        Toast.makeText(context, "Activity created successfully in $selectedCity!", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("publish_activity_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Publish, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Publish Activity", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
