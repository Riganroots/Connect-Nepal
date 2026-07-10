package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.UserEntity
import com.example.ui.viewmodel.ConnectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalGuideScreen(
    viewModel: ConnectViewModel,
    initialTab: Int = 0,
    onNavigateToProfile: (Int) -> Unit,
    onNavigateToDetails: (Int) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) } // 0 = AI Guide, 1 = Activities Map, 2 = Nearby People

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.safeDrawing))

        // Screen Title Bar
        Text(
            text = "Local Guide & Nearby",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            color = MaterialTheme.colorScheme.onBackground
        )

        // Custom M3 Tab Row
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().testTag("local_guide_tab_row")
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("AI Local Guide", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Assistant, contentDescription = "AI Guide") },
                modifier = Modifier.testTag("ai_guide_tab")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Activities Map", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Map, contentDescription = "Activities Map") },
                modifier = Modifier.testTag("activities_map_tab")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("People Around Me", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.People, contentDescription = "People Around Me") },
                modifier = Modifier.testTag("nearby_people_tab")
            )
        }

        // Tab Contents
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                0 -> AiGuideTabContent(viewModel = viewModel)
                1 -> KathmanduMapTab(viewModel = viewModel, onNavigateToDetails = onNavigateToDetails)
                2 -> PeopleAroundMeContent(viewModel = viewModel, onNavigateToProfile = onNavigateToProfile)
            }
        }
    }
}

@Composable
fun AiGuideTabContent(viewModel: ConnectViewModel) {
    val context = LocalContext.current
    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var inputQuery by remember { mutableStateOf("") }

    val presetQueries = listOf(
        "Where should I go tonight in Kathmandu?",
        "Suggest peaceful cafe near Baneshwor.",
        "Make a half-day plan around Patan.",
        "Where can I meet new people this weekend?",
        "Suggest places for solo travelers in KTM."
    )

    // Automatically scroll to bottom when new AI messages arrive
    LaunchedEffect(aiMessages.size, isAiLoading) {
        if (aiMessages.isNotEmpty()) {
            listState.animateScrollToItem(aiMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Conversation List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(aiMessages) { (text, isUser) ->
                AiMessageBubble(text = text, isUser = isUser)
            }

            if (isAiLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "AI Guide is thinking...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Quick Suggestion Pills Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "💡 Quick Questions:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetQueries.forEach { query ->
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                if (!isAiLoading) {
                                    viewModel.askAiGuide(query)
                                } else {
                                    Toast.makeText(context, "Please wait for current query to finish", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("preset_query_${query.take(15)}")
                    ) {
                        Text(
                            text = query,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // Custom Query Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                placeholder = { Text("Ask your Kathmandu local guide...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_query_input"),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                trailingIcon = {
                    if (inputQuery.isNotEmpty()) {
                        IconButton(onClick = { inputQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputQuery.isNotBlank()) {
                        viewModel.askAiGuide(inputQuery.trim())
                        inputQuery = ""
                    }
                },
                enabled = !isAiLoading && inputQuery.isNotBlank(),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .size(44.dp)
                    .testTag("ai_send_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Query")
            }
        }
    }
}

@Composable
fun AiMessageBubble(text: String, isUser: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Assistant,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 0.dp,
                        bottomEnd = if (isUser) 0.dp else 16.dp
                    )
                )
                .background(
                    if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PeopleAroundMeContent(
    viewModel: ConnectViewModel,
    onNavigateToProfile: (Int) -> Unit
) {
    val context = LocalContext.current
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val blockedUsers by viewModel.blockedUserIds.collectAsStateWithLifecycle()
    var selectedInterestFilter by remember { mutableStateOf("All") }
    val connectionStatuses = remember { mutableStateMapOf<Int, String>() } // UserId -> Status ("None", "Sent")

    val interestFilters = listOf(
        "All",
        "Coffee friend",
        "Travel buddy",
        "Hiking friend",
        "Futsal player",
        "Photography friend",
        "Language exchange",
        "Startup/networking friend",
        "Food explorer",
        "Nightlife group"
    )

    // Map existing initial mock users/users to approximate areas and interests requested
    val nearbyUsers = remember(allUsers) {
        allUsers.map { user ->
            val area = when (user.id % 4) {
                0 -> "Near Baneshwor"
                1 -> "Near Thamel"
                2 -> "Near Patan"
                else -> "Near Boudha"
            }
            // Ensure they have appropriate mapped interests for the filters
            val mappedInterests = when (user.id) {
                2 -> listOf("Hiking friend", "Photography friend", "Travel buddy")
                3 -> listOf("Coffee friend", "Food explorer", "Language exchange")
                4 -> listOf("Food explorer", "Language exchange")
                5 -> listOf("Travel buddy", "Hiking friend")
                6 -> listOf("Futsal player", "Nightlife group", "Startup/networking friend")
                else -> listOf("Coffee friend", "Startup/networking friend")
            }
            NearbyUserItem(user = user, area = area, nearbyInterests = mappedInterests)
        }
    }

    val filteredNearbyUsers = remember(nearbyUsers, selectedInterestFilter, blockedUsers) {
        nearbyUsers.filter { item ->
            item.user.id !in blockedUsers &&
            (selectedInterestFilter == "All" || item.nearbyInterests.contains(selectedInterestFilter))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Interest Filter Chips Row
        Text(
            text = "🎯 Filter by Interest:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            interestFilters.forEach { filter ->
                FilterChip(
                    selected = selectedInterestFilter == filter,
                    onClick = { selectedInterestFilter = filter },
                    label = { Text(filter) },
                    modifier = Modifier.testTag("filter_chip_$filter")
                )
            }
        }

        if (filteredNearbyUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PersonPinCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No nearby users found matching \"$selectedInterestFilter\"",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredNearbyUsers) { item ->
                    val status = connectionStatuses[item.user.id] ?: "None"
                    NearbyUserCard(
                        item = item,
                        connectionStatus = status,
                        onConnectClick = {
                            connectionStatuses[item.user.id] = "Sent"
                            Toast.makeText(context, "Connection request sent to ${item.user.name}! 🤝", Toast.LENGTH_SHORT).show()
                        },
                        onCardClick = {
                            onNavigateToProfile(item.user.id)
                        }
                    )
                }
            }
        }
    }
}

data class NearbyUserItem(
    val user: UserEntity,
    val area: String,
    val nearbyInterests: List<String>
)

@Composable
fun NearbyUserCard(
    item: NearbyUserItem,
    connectionStatus: String,
    onConnectClick: () -> Unit,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("nearby_user_card_${item.user.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = rememberAsyncImagePainter(item.user.profilePictureUrl),
                    contentDescription = item.user.name,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.user.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = item.area,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.user.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Interests Tag Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item.nearbyInterests.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Connect button with minimum interactive component size (48dp)
            Button(
                onClick = onConnectClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (connectionStatus == "Sent") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("connect_btn_${item.user.id}"),
                enabled = connectionStatus != "Sent"
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (connectionStatus == "Sent") Icons.Default.CheckCircle else Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (connectionStatus == "Sent") "Connection Requested" else "Request to Connect",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
