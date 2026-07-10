package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.ui.screens.ActivityRowItem
import com.example.data.model.ActivityEntity
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ConnectViewModel
import com.example.ui.viewmodel.ConnectViewModelFactory

class MainActivity : ComponentActivity() {
    
    private val viewModel: ConnectViewModel by viewModels {
        ConnectViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            
            MyApplicationTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ConnectApp(viewModel = viewModel)
                }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    object Discover : Screen("discover", "Discover", Icons.Outlined.Explore, Icons.Filled.Explore)
    object Search : Screen("search", "Search", Icons.Outlined.Search, Icons.Filled.Search)
    object Local : Screen("local", "AI Guide", Icons.Outlined.LocationOn, Icons.Filled.LocationOn)
    object Saved : Screen("saved", "Saved", Icons.Outlined.BookmarkBorder, Icons.Filled.Bookmark)
    object Profile : Screen("profile/1", "My Profile", Icons.Outlined.Person, Icons.Filled.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectApp(viewModel: ConnectViewModel) {
    val navController = rememberNavController()
    val isOnboarded by viewModel.isOnboarded.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    // If not logged in, show the sleek Login / Sign Up screen
    if (currentUser == null) {
        AuthScreen(
            viewModel = viewModel,
            onAuthSuccess = {
                // Handled internally by ViewModel flows
            }
        )
        return
    }

    // Determine if onboarding screen should show
    if (!isOnboarded) {
        OnboardingScreen(
            onCitySelected = { city ->
                viewModel.completeOnboarding(city)
            }
        )
        return
    }

    val items = listOf(
        Screen.Discover,
        Screen.Search,
        Screen.Local,
        Screen.Saved,
        Screen.Profile
    )

    Scaffold(
        bottomBar = {
            // Only show bottom navigation on main dashboard screens
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val currentRoute = currentDestination?.route

            val showBottomBar = currentRoute in listOf(
                Screen.Discover.route,
                Screen.Search.route,
                Screen.Local.route,
                Screen.Saved.route,
                "profile/{userId}" // Include profile route pattern
            ) && currentRoute != "profile/1" // Do show on normal profiles but simplify routing if needed

            if (showBottomBar || currentRoute == "profile/1" || currentRoute == "profile/{userId}") {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .testTag("bottom_nav_bar")
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                ) {
                    items.forEach { screen ->
                        val isSelected = currentDestination?.hierarchy?.any { 
                            it.route == screen.route || (screen == Screen.Profile && it.route == "profile/{userId}")
                        } == true
                        
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title, fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal) },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Discover.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Discover/Home
            composable(Screen.Discover.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToDetails = { activityId ->
                        navController.navigate("activity_details/$activityId")
                    },
                    onNavigateToCreate = {
                        navController.navigate("create_activity")
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    },
                    onNavigateToProfile = { userId ->
                        navController.navigate("profile/$userId")
                    },
                    onNavigateToMap = {
                        navController.navigate("local?tab=1") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            // Search
            composable(Screen.Search.route) {
                SearchScreen(
                    viewModel = viewModel,
                    onNavigateToDetails = { activityId ->
                        navController.navigate("activity_details/$activityId")
                    },
                    onNavigateToProfile = { userId ->
                        navController.navigate("profile/$userId")
                    }
                )
            }

            // Local Guide (AI & Nearby)
            composable(
                route = "local?tab={tab}",
                arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 })
            ) { backStackEntry ->
                val initialTab = backStackEntry.arguments?.getInt("tab") ?: 0
                LocalGuideScreen(
                    viewModel = viewModel,
                    initialTab = initialTab,
                    onNavigateToProfile = { userId ->
                        navController.navigate("profile/$userId")
                    },
                    onNavigateToDetails = { activityId ->
                        navController.navigate("activity_details/$activityId")
                    }
                )
            }

            // Saved Activities
            composable(Screen.Saved.route) {
                SavedActivitiesScreen(
                    viewModel = viewModel,
                    onNavigateToDetails = { activityId ->
                        navController.navigate("activity_details/$activityId")
                    }
                )
            }

            // Profile
            composable(
                route = "profile/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.IntType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getInt("userId") ?: 1
                ProfileScreen(
                    userId = userId,
                    viewModel = viewModel,
                    onNavigateToDetails = { activityId ->
                        navController.navigate("activity_details/$activityId")
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Create Activity
            composable("create_activity") {
                CreateActivityScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Activity Details
            composable(
                route = "activity_details/{activityId}",
                arguments = listOf(navArgument("activityId") { type = NavType.IntType })
            ) { backStackEntry ->
                val activityId = backStackEntry.arguments?.getInt("activityId") ?: 0
                ActivityDetailsScreen(
                    activityId = activityId,
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToChat = { id ->
                        navController.navigate("chat/$id")
                    },
                    onNavigateToProfile = { userId ->
                        navController.navigate("profile/$userId")
                    }
                )
            }

            // Group Chat
            composable(
                route = "chat/{activityId}",
                arguments = listOf(navArgument("activityId") { type = NavType.IntType })
            ) { backStackEntry ->
                val activityId = backStackEntry.arguments?.getInt("activityId") ?: 0
                ChatScreen(
                    activityId = activityId,
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Settings
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onLogout = {
                        navController.navigate(Screen.Discover.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SavedActivitiesScreen(
    viewModel: ConnectViewModel,
    onNavigateToDetails: (Int) -> Unit
) {
    val savedActivities by viewModel.savedActivities.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.safeDrawing))

        Text(
            text = "Saved Activities",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        if (savedActivities.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your bookmarks list is empty",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Save interesting coffee, hiking or futsal meetups so you can find them later!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(savedActivities, key = { it.id }) { activity ->
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
}
