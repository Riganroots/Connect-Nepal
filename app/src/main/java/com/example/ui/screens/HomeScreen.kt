package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Application
import android.content.Intent
import com.example.ui.viewmodel.HomeBloc
import com.example.ui.viewmodel.HomeBlocFactory
import com.example.ui.viewmodel.HomeEvent
import com.example.ui.viewmodel.HomeState
import com.example.ui.viewmodel.SortOption
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.ActivityEntity
import com.example.data.model.MockData
import com.example.ui.viewmodel.ConnectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ConnectViewModel,
    onNavigateToDetails: (Int) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: (Int) -> Unit,
    onNavigateToMap: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val homeBloc: HomeBloc = viewModel(
        factory = HomeBlocFactory(application)
    )

    val homeState by homeBloc.state.collectAsStateWithLifecycle()

    // Dialog state variables for Save/Bookmark feedback
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveDialogTitle by remember { mutableStateOf("") }
    var saveDialogMessage by remember { mutableStateOf("") }
    var saveDialogIcon by remember { mutableStateOf<ImageVector>(Icons.Default.Bookmark) }

    // Dialog state variables for Join Success feedback
    var showJoinSuccessDialog by remember { mutableStateOf(false) }
    var joinSuccessTitle by remember { mutableStateOf("") }
    var joinSuccessMessage by remember { mutableStateOf("") }

    // Dialog state variables for Leave Confirmation
    var showLeaveConfirmDialog by remember { mutableStateOf(false) }
    var leaveConfirmActivityId by remember { mutableStateOf<Int?>(null) }
    var leaveConfirmActivityTitle by remember { mutableStateOf("") }

    // Filter drawer/sheet modal state variable
    var showFilterSheet by remember { mutableStateOf(false) }

    // Reusable handler to save/unsave activity with a custom confirmation dialog feedback
    val handleSaveToggle: (ActivityEntity) -> Unit = { activity ->
        homeBloc.onEvent(HomeEvent.ToggleSave(activity.id))
        if (!activity.isSaved) {
            saveDialogTitle = "Activity Saved!"
            saveDialogMessage = "You have successfully saved \"${activity.title}\" to your bookmarks."
            saveDialogIcon = Icons.Default.Bookmark
            showSaveDialog = true
        } else {
            saveDialogTitle = "Removed Bookmark"
            saveDialogMessage = "\"${activity.title}\" has been removed from your saved list."
            saveDialogIcon = Icons.Default.BookmarkBorder
            showSaveDialog = true
        }
    }

    // Reusable handler to join/leave activity with a custom confirmation dialog feedback
    val handleJoinToggle: (ActivityEntity) -> Unit = { activity ->
        if (activity.isJoined) {
            leaveConfirmActivityId = activity.id
            leaveConfirmActivityTitle = activity.title
            showLeaveConfirmDialog = true
        } else {
            homeBloc.onEvent(HomeEvent.ToggleJoin(activity.id, false))
            joinSuccessTitle = "Slot Reserved!"
            joinSuccessMessage = "You have successfully joined \"${activity.title}\". We've reserved your slot, and you've been added to the participant list!"
            showJoinSuccessDialog = true
        }
    }

    val categories = listOf(
        CategoryItem("Food & Cafés", "🍽", Icons.Default.Restaurant),
        CategoryItem("Drinks & Nightlife", "🍻", Icons.Default.LocalBar),
        CategoryItem("Events", "🎉", Icons.Default.Event),
        CategoryItem("Sports", "⚽", Icons.Default.SportsSoccer),
        CategoryItem("Outdoor", "🏔", Icons.Default.Terrain),
        CategoryItem("Meet People", "👥", Icons.Default.Groups)
    )

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToCreate,
                icon = { Icon(Icons.Default.Add, contentDescription = "Create Activity") },
                text = { Text(text = "Create Activity", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .testTag("create_activity_fab")
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(bottom = 80.dp) // Avoid overlap with bottom nav
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val state = homeState
            val selectedCategory = if (state is HomeState.Success) state.selectedCategory else null
            val activeCity = if (state is HomeState.Success) state.city else "Kathmandu"

            // Header with City and Settings Trigger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Exploring",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onNavigateToSettings() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Active City",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = activeCity,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Change City",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Map Shortcut Button
                    IconButton(
                        onClick = onNavigateToMap,
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .testTag("home_map_shortcut_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Activities Map",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Profile Image / Settings icon
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Categories Filter Row
            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    SleekAllCategoryItem(
                        isSelected = selectedCategory == null,
                        onClick = { homeBloc.onEvent(HomeEvent.SelectCategory(null)) }
                    )
                }

                items(categories) { cat ->
                    CategoryIcon(
                        categoryName = cat.name,
                        isSelected = selectedCategory == cat.name,
                        onClick = { homeBloc.onEvent(HomeEvent.SelectCategory(if (selectedCategory == cat.name) null else cat.name)) }
                    )
                }
            }

            if (state is HomeState.Success) {
                val successState = state as HomeState.Success
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (successState.selectedCategory != null) {
                            "Category: ${successState.selectedCategory}"
                        } else {
                            "All Activities"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("selected_category_label")
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Sorting Dropdown Selector
                        var expandedSortMenu by remember { mutableStateOf(false) }
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { expandedSortMenu = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("sort_by_dropdown_trigger"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort Options",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sort: ${successState.sortBy.name}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            DropdownMenu(
                                expanded = expandedSortMenu,
                                onDismissRequest = { expandedSortMenu = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                DropdownMenuItem(
                                    text = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.NewReleases,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = if (successState.sortBy == SortOption.Newest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Newest", fontWeight = if (successState.sortBy == SortOption.Newest) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    },
                                    onClick = {
                                        homeBloc.onEvent(HomeEvent.ChangeSortOption(SortOption.Newest))
                                        expandedSortMenu = false
                                    },
                                    modifier = Modifier.testTag("sort_option_newest")
                                )
                                DropdownMenuItem(
                                    text = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = if (successState.sortBy == SortOption.Date) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Date", fontWeight = if (successState.sortBy == SortOption.Date) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    },
                                    onClick = {
                                        homeBloc.onEvent(HomeEvent.ChangeSortOption(SortOption.Date))
                                        expandedSortMenu = false
                                    },
                                    modifier = Modifier.testTag("sort_option_date")
                                )
                            }
                        }

                        // Filter Button Trigger
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (successState.selectedSubcategories.isNotEmpty() || successState.selectedVisibilities.size < 2) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    }
                                )
                                .clickable { showFilterSheet = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("filter_drawer_trigger"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter Options",
                                modifier = Modifier.size(16.dp),
                                tint = if (successState.selectedSubcategories.isNotEmpty() || successState.selectedVisibilities.size < 2) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Filter",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (successState.selectedSubcategories.isNotEmpty() || successState.selectedVisibilities.size < 2) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            if (successState.selectedSubcategories.isNotEmpty() || successState.selectedVisibilities.size < 2) {
                                val activeFilterCount = successState.selectedSubcategories.size + (2 - successState.selectedVisibilities.size)
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = activeFilterCount.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            when (state) {
                is HomeState.Loading -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                is HomeState.Error -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
                is HomeState.Success -> {
                    val hasNoActivities = state.popularToday.isEmpty() &&
                            state.nearby.isEmpty() &&
                            state.trending.isEmpty() &&
                            state.recommended.isEmpty() &&
                            state.newActivities.isEmpty()

                    if (hasNoActivities) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ExploreOff,
                                        contentDescription = "No activities",
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "No Activities in $activeCity yet",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (selectedCategory != null) {
                                        "Try clearing the \"$selectedCategory\" filter or be the first to create one!"
                                    } else {
                                        "Be the spark that connects the city. Create a new activity to meet friends!"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = onNavigateToCreate,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Create First Activity")
                                }
                            }
                        }
                    } else {
                        // Sectioned Activities View
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 100.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // 1. Popular Today (Horizontally scrollable high impact cards)
                            if (state.popularToday.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Text(
                                            text = "Popular Today",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        )
                                        Text(
                                            text = "See all",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.clickable {
                                                homeBloc.onEvent(HomeEvent.SelectCategory(null))
                                            }
                                        )
                                    }
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(state.popularToday, key = { it.id }) { activity ->
                                            val participants by viewModel.getParticipantsStream(activity.id).collectAsStateWithLifecycle(initialValue = emptyList())
                                            PopularActivityCard(
                                                activity = activity,
                                                participants = participants,
                                                onCardClick = { onNavigateToDetails(activity.id) },
                                                onSaveToggle = { handleSaveToggle(activity) },
                                                onJoinToggle = { handleJoinToggle(activity) }
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. Section: Nearby
                            if (state.nearby.isNotEmpty()) {
                                item {
                                    SectionHeader(title = "Nearby Activities", onSeeAll = {})
                                }
                                val nearbyList = state.nearby.take(3)
                                items(nearbyList, key = { it.id }) { activity ->
                                    val participants by viewModel.getParticipantsStream(activity.id).collectAsStateWithLifecycle(initialValue = emptyList())
                                    ActivityRowItem(
                                        activity = activity,
                                        participants = participants,
                                        onCardClick = { onNavigateToDetails(activity.id) },
                                        onSaveToggle = { handleSaveToggle(activity) },
                                        onJoinToggle = { handleJoinToggle(activity) }
                                    )
                                }
                            }

                            // 3. Section: Trending
                            if (state.trending.isNotEmpty()) {
                                item {
                                    SectionHeader(title = "Trending", onSeeAll = {})
                                }
                                val trendingList = state.trending.take(2)
                                items(trendingList, key = { it.id }) { activity ->
                                     val participants by viewModel.getParticipantsStream(activity.id).collectAsStateWithLifecycle(initialValue = emptyList())
                                     ActivityRowItem(
                                         activity = activity,
                                         participants = participants,
                                        onCardClick = { onNavigateToDetails(activity.id) },
                                        onSaveToggle = { handleSaveToggle(activity) },
                                        onJoinToggle = { handleJoinToggle(activity) }
                                    )
                                }
                            }

                            // 4. Section: Recommended (Horizontal slider)
                            if (state.recommended.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Recommended for You",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(state.recommended, key = { it.id }) { activity ->
                                             val participants by viewModel.getParticipantsStream(activity.id).collectAsStateWithLifecycle(initialValue = emptyList())
                                             PopularActivityCard(
                                                 activity = activity,
                                                 participants = participants,
                                                onCardClick = { onNavigateToDetails(activity.id) },
                                                onSaveToggle = { handleSaveToggle(activity) },
                                                onJoinToggle = { handleJoinToggle(activity) }
                                            )
                                        }
                                    }
                                }
                            }

                            // 5. Section: New Activities / Upcoming Activities
                            if (state.newActivities.isNotEmpty()) {
                                item {
                                    Text(
                                        text = if (state.sortBy == SortOption.Newest) "New Activities" else "Upcoming Activities",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                                    )
                                }
                                items(state.newActivities, key = { it.id }) { activity ->
                                    val participants by viewModel.getParticipantsStream(activity.id).collectAsStateWithLifecycle(initialValue = emptyList())
                                    ActivityCard(
                                        activity = activity,
                                        participants = participants,
                                        onCardClick = { onNavigateToDetails(activity.id) },
                                        onSaveToggle = { handleSaveToggle(activity) },
                                        onJoinToggle = { handleJoinToggle(activity) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- CONFIRMATION DIALOGS ---

    // 1. Saved/Bookmarked Feedback Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            icon = {
                Icon(
                    imageVector = saveDialogIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = saveDialogTitle,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = saveDialogMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { showSaveDialog = false },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Great!", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    // 2. Join Successful Confirmation Dialog
    if (showJoinSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showJoinSuccessDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32), // Direct Success Green Color Accent
                    modifier = Modifier.size(44.dp)
                )
            },
            title = {
                Text(
                    text = joinSuccessTitle,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = joinSuccessMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { showJoinSuccessDialog = false },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Awesome!", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    // 3. Leave Warning Confirmation Dialog
    if (showLeaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "Leave Activity?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to leave \"$leaveConfirmActivityTitle\"? You will be removed from the participant list.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        leaveConfirmActivityId?.let { id ->
                            homeBloc.onEvent(HomeEvent.ToggleJoin(id, true))
                        }
                        showLeaveConfirmDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Yes, Leave", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLeaveConfirmDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    // 4. Filters Bottom Sheet / Drawer Modal
    if (showFilterSheet && homeState is HomeState.Success) {
        val successState = homeState as HomeState.Success
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.testTag("filter_bottom_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp)
            ) {
                // Title and clear action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter Activities",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    TextButton(
                        onClick = {
                            homeBloc.onEvent(HomeEvent.ClearFilters)
                        },
                        modifier = Modifier.testTag("clear_filters_button")
                    ) {
                        Text(
                            text = "Clear All",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Visibility Filter
                Text(
                    text = "Visibility Setting",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val visibilities = listOf("Public", "Friends")
                    visibilities.forEach { vis ->
                        val isSelected = successState.selectedVisibilities.contains(vis)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                homeBloc.onEvent(HomeEvent.ToggleVisibility(vis))
                            },
                            label = { Text(vis) },
                            leadingIcon = {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (vis == "Public") Icons.Default.Public else Icons.Default.People,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            modifier = Modifier.testTag("visibility_chip_$vis")
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Subcategories Filter
                Text(
                    text = "Sub-categories",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                if (successState.availableSubcategories.isEmpty()) {
                    Text(
                        text = "No sub-categories available in this city.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                } else {
                    val chunkedSubcats = successState.availableSubcategories.chunked(3)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        chunkedSubcats.forEach { rowSubcats ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowSubcats.forEach { sub ->
                                    val isSelected = successState.selectedSubcategories.contains(sub)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            homeBloc.onEvent(HomeEvent.ToggleSubcategory(sub))
                                        },
                                        label = { Text(sub, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        leadingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("subcategory_chip_$sub")
                                    )
                                }
                                // Fill empty spaces if the row has less than 3 items
                                if (rowSubcats.size < 3) {
                                    repeat(3 - rowSubcats.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Show Results / Close Button
                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("apply_filters_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Apply & View Results",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

data class CategoryItem(
    val name: String,
    val emoji: String,
    val icon: ImageVector
)

@Composable
fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun ParticipantFacepile(
    participants: List<com.example.data.model.ParticipantEntity>,
    maxAvatars: Int = 4,
    modifier: Modifier = Modifier
) {
    if (participants.isEmpty()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.padding(vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Be the first to join!",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.padding(vertical = 4.dp)
        ) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                participants.take(maxAvatars).forEachIndexed { index, participant ->
                    Box(
                        modifier = Modifier
                            .padding(start = (index * 14).dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(participant.userAvatar),
                            contentDescription = participant.userName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            
            val namesText = when {
                participants.size == 1 -> "Joined by ${participants[0].userName}"
                participants.size == 2 -> "Joined by ${participants[0].userName} & ${participants[1].userName}"
                participants.size == 3 -> "Joined by ${participants[0].userName}, ${participants[1].userName} & 1 other"
                else -> "Joined by ${participants[0].userName} & ${participants.size - 1} others"
            }
            
            Text(
                text = namesText,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}

@Composable
fun PopularActivityCard(
    activity: ActivityEntity,
    participants: List<com.example.data.model.ParticipantEntity>,
    onCardClick: () -> Unit,
    onSaveToggle: () -> Unit,
    onJoinToggle: () -> Unit
) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .width(260.dp)
            .padding(vertical = 4.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .clickable { onCardClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            Image(
                painter = rememberAsyncImagePainter(activity.coverImageUrl),
                contentDescription = activity.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Category tag (Pristine Sleek Capsule Badge)
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "${activity.category.uppercase()} • ${activity.location.split(",").firstOrNull() ?: activity.location}",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // Save Toggle
            IconButton(
                onClick = onSaveToggle,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = if (activity.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Save Activity",
                    tint = if (activity.isSaved) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = activity.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${activity.date} • ${activity.time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            ParticipantFacepile(participants = participants)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Slots left
                Text(
                    text = "${activity.participantCount}/${activity.maxParticipants} joined",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )

                // Join toggle CTA
                Button(
                    onClick = onJoinToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activity.isJoined) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                        contentColor = if (activity.isJoined) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(24.dp), // rounded-full style
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = if (activity.isJoined) "Joined" else "Join",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityRowItem(
    activity: ActivityEntity,
    participants: List<com.example.data.model.ParticipantEntity>,
    onCardClick: () -> Unit,
    onSaveToggle: () -> Unit,
    onJoinToggle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable { onCardClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(activity.coverImageUrl),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${activity.category} • ${activity.subCategory}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                if (participants.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.CenterStart) {
                            participants.take(3).forEachIndexed { idx, p ->
                                Box(
                                    modifier = Modifier
                                        .padding(start = (idx * 10).dp)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(0.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(p.userAvatar),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width((participants.take(3).size * 10 + 6).dp))
                        Text(
                            text = if (participants.size == 1) "1 joined" else "${participants.size} joined",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = activity.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = onSaveToggle,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (activity.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save",
                        tint = if (activity.isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onJoinToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activity.isJoined) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                        contentColor = if (activity.isJoined) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = if (activity.isJoined) "Joined" else "Join",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityCard(
    activity: ActivityEntity,
    participants: List<com.example.data.model.ParticipantEntity>,
    onCardClick: () -> Unit,
    onSaveToggle: () -> Unit,
    onJoinToggle: () -> Unit
) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .clickable { onCardClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(activity.coverImageUrl),
                    contentDescription = activity.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Category overlay tag (Pristine Sleek Capsule Badge)
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopStart)
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${activity.category.uppercase()} • ${activity.subCategory.uppercase()}",
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Top right actions
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onSaveToggle,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (activity.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (activity.isSaved) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Connect Activity: ${activity.title}")
                                val shareText = """
                                    Join me for ${activity.title}!
                                    
                                    📅 Date: ${activity.date}
                                    ⏰ Time: ${activity.time}
                                    📍 Location: ${activity.location} (${activity.city})
                                    🤝 Meeting Point: ${activity.meetingPoint}
                                    💰 Cost: ${activity.cost}
                                    👤 Organized by: ${activity.organizerName}
                                    
                                    About this activity:
                                    ${activity.description}
                                    
                                    Shared via Connect App
                                 """.trimIndent()
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share activity via"))
                        },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Title
                    Text(
                        text = activity.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Optional Cost
                    Badge(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = activity.cost,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Organizer, Date, Time & Distance Info Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Organizer details
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = rememberAsyncImagePainter(activity.organizerAvatar),
                            contentDescription = "Organizer avatar",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "By ${activity.organizerName}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Organizer",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // Distance info (e.g. mock "1.2 km away")
                    val distance = remember(activity.id) { "${String.format("%.1f", (1..5).random() + (0..9).random()/10.0)} km away" }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = distance,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                ParticipantFacepile(
                    participants = participants,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                // Bottom row: Time Details and Join Control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${activity.date} • ${activity.time}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${activity.participantCount} / ${activity.maxParticipants} joined",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (activity.participantCount >= activity.maxParticipants) Color.Red else MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Join Button
                    Button(
                        onClick = onJoinToggle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activity.isJoined) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                            contentColor = if (activity.isJoined) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(24.dp), // rounded-full style
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (activity.isJoined) Icons.Default.CheckCircle else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (activity.isJoined) "Joined" else "Join",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SleekCategoryItem(
    category: CategoryItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(68.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.emoji,
                fontSize = 24.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = category.name.split(" ").firstOrNull() ?: category.name,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SleekAllCategoryItem(
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(68.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✨",
                fontSize = 24.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "All",
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
