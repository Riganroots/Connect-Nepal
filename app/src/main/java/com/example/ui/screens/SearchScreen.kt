package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.ActivityEntity
import com.example.data.model.UserEntity
import com.example.ui.viewmodel.ConnectViewModel
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: ConnectViewModel,
    onNavigateToDetails: (Int) -> Unit,
    onNavigateToProfile: (Int) -> Unit
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchType by viewModel.searchType.collectAsStateWithLifecycle()
    val searchActivities by viewModel.searchActivitiesResult.collectAsStateWithLifecycle()
    val searchUsers by viewModel.searchUsersResult.collectAsStateWithLifecycle()
    val currentCity by viewModel.currentCity.collectAsStateWithLifecycle()

    val searchTabs = listOf("Activities", "Places", "Events", "Users")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Safe drawing spacer
        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.safeDrawing))

        // Large Search Bar Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search activities, places, events, people...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_text_input"),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        // Search Category Tabs
        ScrollableTabRow(
            selectedTabIndex = searchTabs.indexOf(searchType).coerceAtLeast(0),
            edgePadding = 20.dp,
            divider = {},
            indicator = { tabPositions ->
                val index = searchTabs.indexOf(searchType).coerceAtLeast(0)
                if (index < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[index]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            searchTabs.forEach { tab ->
                val isSelected = searchType == tab
                Tab(
                    selected = isSelected,
                    onClick = { viewModel.setSearchType(tab) },
                    text = {
                        Text(
                            text = tab,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search Results List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (searchType) {
                "Activities" -> {
                    val filtered = searchActivities.filter { it.category != "Events" }
                    if (filtered.isEmpty()) {
                        EmptySearchResults(query = searchQuery, type = "activities")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filtered) { act ->
                                ActivitySearchRow(
                                    activity = act,
                                    onClick = { onNavigateToDetails(act.id) }
                                )
                            }
                        }
                    }
                }

                "Places" -> {
                    // Custom Simulated searchable Places in Nepal
                    val mockPlaces = remember(currentCity, searchQuery) {
                        listOf(
                            PlaceSearchItem("Boudhanath Stupa", "Heritage & Cafés", "Chabahil Road, Kathmandu", "https://images.unsplash.com/photo-1544735716-392fe2489ffa?q=80&w=600"),
                            PlaceSearchItem("Phewa Lake", "Boating & Walks", "Lakeside Road, Pokhara", "https://images.unsplash.com/photo-1578593139888-39622e2077ef?q=80&w=600"),
                            PlaceSearchItem("Garden of Dreams", "Quiet Park & Coffee", "Tridevi Marg, Thamel", "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?q=80&w=600"),
                            PlaceSearchItem("Patan Museum Cafe", "Historic & Dining", "Patan Durbar Square, Lalitpur", "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?q=80&w=600"),
                            PlaceSearchItem("Nyatapola Cafe", "Traditional Treats", "Taumadhi Square, Bhaktapur", "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?q=80&w=600")
                        ).filter {
                            it.address.contains(currentCity, ignoreCase = true) &&
                            (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.tag.contains(searchQuery, ignoreCase = true))
                        }
                    }

                    if (mockPlaces.isEmpty()) {
                        EmptySearchResults(query = searchQuery, type = "places")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(mockPlaces) { place ->
                                PlaceSearchRow(
                                    place = place,
                                    onClick = {
                                        Toast.makeText(context, "Welcome to ${place.name}! Check out activities here.", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }

                "Events" -> {
                    // Filter activities that are tagged as "Events" or simulate events
                    val eventsList = searchActivities.filter { it.category == "Events" }
                    if (eventsList.isEmpty()) {
                        EmptySearchResults(query = searchQuery, type = "events")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(eventsList) { act ->
                                ActivitySearchRow(
                                    activity = act,
                                    onClick = { onNavigateToDetails(act.id) }
                                )
                            }
                        }
                    }
                }

                "Users" -> {
                    if (searchUsers.isEmpty()) {
                        EmptySearchResults(query = searchQuery, type = "people")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(searchUsers) { user ->
                                UserSearchRow(
                                    user = user,
                                    onUserClick = { onNavigateToProfile(user.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class PlaceSearchItem(
    val name: String,
    val tag: String,
    val address: String,
    val imageUrl: String
)

@Composable
fun EmptySearchResults(query: String, type: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No $type found",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (query.isNotBlank()) "We couldn't find any match for \"$query\". Check for spelling or search tags." else "Nothing matches this criteria in your city currently.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ActivitySearchRow(
    activity: ActivityEntity,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(activity.coverImageUrl),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${activity.date} • ${activity.time}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.outline
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

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun PlaceSearchRow(
    place: PlaceSearchItem,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(place.imageUrl),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = place.tag,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PinDrop,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = place.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = "Map view",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun UserSearchRow(
    user: UserEntity,
    onUserClick: () -> Unit
) {
    val context = LocalContext.current
    var isFollowing by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUserClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(user.profilePictureUrl),
                contentDescription = user.name,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = user.bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${user.followersCount} followers • ${user.city}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Follow Toggle Button
            Button(
                onClick = {
                    isFollowing = !isFollowing
                    val msg = if (isFollowing) "Followed ${user.name}!" else "Unfollowed ${user.name}."
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                    contentColor = if (isFollowing) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = if (isFollowing) "Following" else "Follow",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
