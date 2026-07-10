package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.data.model.ActivityEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserInterest
import com.example.data.model.MockData
import com.example.ui.viewmodel.ConnectViewModel
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: Int,
    viewModel: ConnectViewModel,
    onNavigateToDetails: (Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    
    // Check if we are viewing current user or another user
    val isCurrentUser = currentUser != null && currentUser!!.id == userId
    val userFlow = remember(userId) { viewModel.getOtherUserStream(userId) }
    val viewedUser by userFlow.collectAsStateWithLifecycle(initialValue = null)

    // Load user interests state from flow synced with Firebase
    val interestsListState by viewModel.getUserInterestsStream(userId).collectAsStateWithLifecycle(initialValue = emptyList())

    // Load created/joined lists
    val createdActivitiesFlow = remember(userId) { viewModel.getCreatedActivitiesStream(userId) }
    val createdActivities by createdActivitiesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val joinedActivities by viewModel.joinedActivities.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("Joined") } // "Joined" or "Created"
    var isFollowingUser by remember { mutableStateOf(false) }

    // Dialog states for editing interests
    var showAddInterestDialog by remember { mutableStateOf(false) }
    var newInterestText by remember { mutableStateOf("") }

    if (viewedUser == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val user = viewedUser!!

    // Sync comma-separated interests to our new database-backed UserInterest collection if empty
    LaunchedEffect(interestsListState, user.interests) {
        if (interestsListState.isEmpty() && user.interests.isNotEmpty()) {
            val splitList = user.interests.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            splitList.forEach { interestName ->
                viewModel.attachInterest(userId, interestName, "General")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Safe spacing
        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.safeDrawing))

        // Custom Title Header with Back/Settings buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isCurrentUser) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            } else {
                Text(
                    text = "My Profile",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            if (isCurrentUser) {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Profile Header Info Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Profile Avatar
                        Image(
                            painter = rememberAsyncImagePainter(user.profilePictureUrl),
                            contentDescription = user.name,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Name
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                        )

                        // City Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = user.city,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Stats Layout
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(32.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatItem(count = user.followersCount + (if (isFollowingUser) 1 else 0), label = "Followers")
                            StatItem(count = user.followingCount, label = "Following")
                            StatItem(count = createdActivities.size, label = "Created")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Bio
                        Text(
                            text = user.bio,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // Follow Action if not current user
                        if (!isCurrentUser) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    isFollowingUser = !isFollowingUser
                                    val msg = if (isFollowingUser) "You followed ${user.name}!" else "Unfollowed ${user.name}."
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowingUser) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                                    contentColor = if (isFollowingUser) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = if (isFollowingUser) Icons.Default.CheckCircle else Icons.Default.PersonAdd,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isFollowingUser) "Following" else "Follow",
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            // Interests Segment
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Interests",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (isCurrentUser) {
                        IconButton(
                            onClick = { showAddInterestDialog = true },
                            modifier = Modifier.testTag("add_interest_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "Add Interest",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                val displayedInterests = if (interestsListState.isNotEmpty()) {
                    interestsListState.map { it.interestName }
                } else {
                    user.interests.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }

                if (displayedInterests.isEmpty()) {
                    Text(
                        text = "No interests added yet. Tap + to add some!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    val chunkedInterests = displayedInterests.chunked(3)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        chunkedInterests.forEach { rowInterests ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowInterests.forEach { interest ->
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                                            .clickable {
                                                if (isCurrentUser) {
                                                    viewModel.detachInterest(userId, interest)
                                                    Toast.makeText(context, "Removed $interest", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                            .testTag("interest_tag_$interest")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = interest,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            if (isCurrentUser) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove $interest",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                if (rowInterests.size < 3) {
                                    repeat(3 - rowInterests.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tabs for Joined / Created activities list
            item {
                TabRow(
                    selectedTabIndex = if (activeTab == "Joined") 0 else 1,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = { tabPositions ->
                        val index = if (activeTab == "Joined") 0 else 1
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[index]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Tab(
                        selected = activeTab == "Joined",
                        onClick = { activeTab = "Joined" },
                        text = { Text("Upcoming Plans", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("profile_tab_upcoming")
                    )
                    Tab(
                        selected = activeTab == "Created",
                        onClick = { activeTab = "Created" },
                        text = { Text(if (isCurrentUser) "My Activities" else "Organized Activities", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("profile_tab_activities")
                    )
                }
            }

            // List of selected activities
            val listToShow = if (activeTab == "Joined") {
                // If it's the current user, show actual joined. If another user, filter initialActivities
                if (isCurrentUser) joinedActivities else MockData.initialActivities.filter { it.organizerId != userId && it.isJoined }
            } else {
                createdActivities
            }

            if (listToShow.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.EventNote,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No activities here yet",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            } else {
                items(listToShow, key = { it.id }) { activity ->
                    val participants by viewModel.getParticipantsStream(activity.id).collectAsStateWithLifecycle(initialValue = emptyList())
                    ActivityRowItem(
                        activity = activity,
                        participants = participants,
                        onCardClick = { onNavigateToDetails(activity.id) },
                        onSaveToggle = { viewModel.toggleSaveActivity(activity.id) },
                        onJoinToggle = {
                            if (activity.isJoined) viewModel.leaveActivity(activity.id)
                            else viewModel.joinActivity(activity.id)
                        }
                    )
                }
            }
        }
    }

    if (showAddInterestDialog) {
        AlertDialog(
            onDismissRequest = { showAddInterestDialog = false },
            title = { Text("Add Interest Tag", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newInterestText,
                    onValueChange = { newInterestText = it },
                    label = { Text("Interest Name") },
                    singleLine = true,
                    placeholder = { Text("e.g. Hiking, Coding, Music") },
                    modifier = Modifier.fillMaxWidth().testTag("add_interest_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newInterestText.isNotBlank()) {
                            viewModel.attachInterest(userId, newInterestText.trim(), "General")
                            newInterestText = ""
                            showAddInterestDialog = false
                            Toast.makeText(context, "Interest added!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_interest")
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddInterestDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatItem(count: Int, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
