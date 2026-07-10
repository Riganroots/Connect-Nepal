package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.ActivityEntity
import com.example.data.model.MockData
import com.example.data.repository.ConnectRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// 1. Home Events for BLoC
enum class SortOption {
    Newest, Date
}

sealed interface HomeEvent {
    data class SelectCategory(val category: String?) : HomeEvent
    data class ToggleSave(val activityId: Int) : HomeEvent
    data class ToggleJoin(val activityId: Int, val isAlreadyJoined: Boolean) : HomeEvent
    data class ChangeCity(val city: String) : HomeEvent
    data class ChangeSortOption(val sortBy: SortOption) : HomeEvent
    data class ToggleSubcategory(val subcategory: String) : HomeEvent
    data class ToggleVisibility(val visibility: String) : HomeEvent
    object ClearFilters : HomeEvent
}

// 2. Home States for BLoC
sealed interface HomeState {
    object Loading : HomeState
    data class Success(
        val city: String,
        val selectedCategory: String?,
        val popularToday: List<ActivityEntity>,
        val nearby: List<ActivityEntity>,
        val trending: List<ActivityEntity>,
        val recommended: List<ActivityEntity>,
        val newActivities: List<ActivityEntity>,
        val sortBy: SortOption = SortOption.Newest,
        val availableSubcategories: List<String> = emptyList(),
        val selectedSubcategories: Set<String> = emptySet(),
        val selectedVisibilities: Set<String> = setOf("Public", "Friends")
    ) : HomeState
    data class Error(val message: String) : HomeState
}

// Helper data classes for stream pairing
data class HomeParamsTuple(
    val user: com.example.data.model.UserEntity?,
    val category: String?,
    val sortBy: SortOption,
    val selectedSubcategories: Set<String>,
    val selectedVisibilities: Set<String>
)

data class HomeParams(
    val user: com.example.data.model.UserEntity?,
    val category: String?,
    val sortBy: SortOption,
    val selectedSubcategories: Set<String>,
    val selectedVisibilities: Set<String>,
    val list: List<ActivityEntity>
)

// 3. Home BLoC
@OptIn(ExperimentalCoroutinesApi::class)
class HomeBloc(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = ConnectRepository(database.connectDao())

    // Category selection event pipeline
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _selectedSort = MutableStateFlow<SortOption>(SortOption.Newest)
    private val _selectedSubcategories = MutableStateFlow<Set<String>>(emptySet())
    private val _selectedVisibilities = MutableStateFlow<Set<String>>(setOf("Public", "Friends"))

    // State flow exposed to UI
    private val _state = MutableStateFlow<HomeState>(HomeState.Loading)
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.currentUser,
                _selectedCategory,
                _selectedSort,
                _selectedSubcategories,
                _selectedVisibilities
            ) { user, category, sort, subcats, visibilities ->
                HomeParamsTuple(user, category, sort, subcats, visibilities)
            }.flatMapLatest { tuple ->
                val city = tuple.user?.city ?: "Kathmandu"
                repository.getActivitiesByCity(city).map { list ->
                    HomeParams(
                        user = tuple.user,
                        category = tuple.category,
                        sortBy = tuple.sortBy,
                        selectedSubcategories = tuple.selectedSubcategories,
                        selectedVisibilities = tuple.selectedVisibilities,
                        list = list
                    )
                }
            }.collectLatest { params ->
                val user = params.user
                val category = params.category
                val sort = params.sortBy
                val list = params.list
                val selectedSubcats = params.selectedSubcategories
                val selectedVis = params.selectedVisibilities
                val city = user?.city ?: "Kathmandu"
                val interests = user?.interests ?: ""

                // Dynamic subcategories available in this city's activities:
                val availableSubcats = list.map { it.subCategory }.distinct().sorted()

                // Filter by category if one is selected
                var filteredList = if (category == null) list else list.filter { it.category == category }

                // Filter by subcategory if any is selected
                if (selectedSubcats.isNotEmpty()) {
                    filteredList = filteredList.filter { it.subCategory in selectedSubcats }
                }

                // Filter by visibility setting
                filteredList = filteredList.filter { it.visibility in selectedVis }

                if (filteredList.isEmpty() && list.isEmpty()) {
                    _state.value = HomeState.Success(
                        city = city,
                        selectedCategory = category,
                        popularToday = emptyList(),
                        nearby = emptyList(),
                        trending = emptyList(),
                        recommended = emptyList(),
                        newActivities = emptyList(),
                        sortBy = sort,
                        availableSubcategories = availableSubcats,
                        selectedSubcategories = selectedSubcats,
                        selectedVisibilities = selectedVis
                    )
                } else {
                    // Precompute partitions using clean business logic helper methods
                    val popularToday = computePopularToday(filteredList)
                    val nearby = computeNearby(filteredList, city)
                    val trending = computeTrending(filteredList)
                    val recommended = computeRecommended(filteredList, interests)
                    val newActivities = computeNewActivities(filteredList, sort)

                    _state.value = HomeState.Success(
                        city = city,
                        selectedCategory = category,
                        popularToday = popularToday,
                        nearby = nearby,
                        trending = trending,
                        recommended = recommended,
                        newActivities = newActivities,
                        sortBy = sort,
                        availableSubcategories = availableSubcats,
                        selectedSubcategories = selectedSubcats,
                        selectedVisibilities = selectedVis
                    )
                }
            }
        }
    }

    // Process incoming events
    fun onEvent(event: HomeEvent) {
        viewModelScope.launch {
            when (event) {
                is HomeEvent.SelectCategory -> {
                    _selectedCategory.value = event.category
                }
                is HomeEvent.ToggleSave -> {
                    repository.toggleSaveActivity(event.activityId)
                }
                is HomeEvent.ToggleJoin -> {
                    if (event.isAlreadyJoined) {
                        getCurrentUser()?.id?.let { userId ->
                            repository.leaveActivity(event.activityId, userId)
                        }
                    } else {
                        val user = getCurrentUser() ?: MockData.currentUser
                        repository.joinActivity(
                            activityId = event.activityId,
                            userId = user.id,
                            userName = user.name,
                            userAvatar = user.profilePictureUrl
                        )
                    }
                }
                is HomeEvent.ChangeCity -> {
                    getCurrentUser()?.let { user ->
                        repository.updateUser(user.copy(city = event.city))
                    }
                }
                is HomeEvent.ChangeSortOption -> {
                    _selectedSort.value = event.sortBy
                }
                is HomeEvent.ToggleSubcategory -> {
                    val current = _selectedSubcategories.value
                    _selectedSubcategories.value = if (current.contains(event.subcategory)) {
                        current - event.subcategory
                    } else {
                        current + event.subcategory
                    }
                }
                is HomeEvent.ToggleVisibility -> {
                    val current = _selectedVisibilities.value
                    _selectedVisibilities.value = if (current.contains(event.visibility)) {
                        current - event.visibility
                    } else {
                        current + event.visibility
                    }
                }
                is HomeEvent.ClearFilters -> {
                    _selectedSubcategories.value = emptySet()
                    _selectedVisibilities.value = setOf("Public", "Friends")
                }
            }
        }
    }

    private suspend fun getCurrentUser() = repository.currentUser.firstOrNull()

    // --- Core Partition & Scoring Helper Functions ---

    private fun computePopularToday(list: List<ActivityEntity>): List<ActivityEntity> {
        // Highly popular activities sorted by descending participantCount
        return list.sortedByDescending { it.participantCount }
    }

    private fun computeNearby(list: List<ActivityEntity>, city: String): List<ActivityEntity> {
        // Geolocation-based distance sorting from the city's base center landmark
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
        val baseCoords = cityCoordinates[city] ?: Pair(27.7172, 85.3240)

        return list.sortedBy { activity ->
            val dLat = activity.latitude - baseCoords.first
            val dLon = activity.longitude - baseCoords.second
            // Euclidean square distance
            dLat * dLat + dLon * dLon
        }
    }

    private fun computeTrending(list: List<ActivityEntity>): List<ActivityEntity> {
        // Trending score: prioritization based on full slots ratio and recency
        return list.sortedByDescending { activity ->
            val fillRatio = activity.participantCount.toDouble() / activity.maxParticipants.toDouble()
            // Weighted formula
            (fillRatio * 80.0) + (activity.id * 1.5)
        }
    }

    private fun computeRecommended(list: List<ActivityEntity>, interestsString: String): List<ActivityEntity> {
        val tags = interestsString.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

        if (tags.isEmpty()) {
            return list.sortedByDescending { it.id }
        }

        return list.sortedByDescending { activity ->
            var score = 0
            val category = activity.category.lowercase()
            val subCategory = activity.subCategory.lowercase()
            val title = activity.title.lowercase()
            val description = activity.description.lowercase()

            for (tag in tags) {
                if (category.contains(tag)) score += 4
                if (subCategory.contains(tag)) score += 3
                if (title.contains(tag)) score += 2
                if (description.contains(tag)) score += 1
            }
            score
        }
    }

    private fun parseActivityDate(dateStr: String): Long {
        try {
            // Check if it's in the format "Saturday, Jul 11"
            val parts = dateStr.split(",")
            if (parts.size >= 2) {
                val monthDay = parts[1].trim().split(" ")
                if (monthDay.size >= 2) {
                    val monthStr = monthDay[0].lowercase()
                    val dayStr = monthDay[1]
                    val months = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
                    val monthIndex = months.indexOf(monthStr).coerceAtLeast(0)
                    val day = dayStr.toIntOrNull() ?: 1
                    val calendar = java.util.Calendar.getInstance()
                    calendar.set(java.util.Calendar.YEAR, 2026)
                    calendar.set(java.util.Calendar.MONTH, monthIndex)
                    calendar.set(java.util.Calendar.DAY_OF_MONTH, day)
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    calendar.set(java.util.Calendar.MINUTE, 0)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                    return calendar.timeInMillis
                }
            }
            // Fallback: try parsing direct digits (e.g., "07/11" or "2026-07-11")
            val digits = dateStr.filter { it.isDigit() }
            if (digits.length >= 4) {
                return digits.toLongOrNull() ?: 0L
            }
        } catch (e: Exception) {
            // Fallback
        }
        return 0L
    }

    private fun computeNewActivities(list: List<ActivityEntity>, sortBy: SortOption): List<ActivityEntity> {
        return when (sortBy) {
            SortOption.Newest -> list.sortedByDescending { it.id }
            SortOption.Date -> list.sortedWith(
                compareBy<ActivityEntity> { parseActivityDate(it.date) }
                    .thenByDescending { it.id }
            )
        }
    }
}

class HomeBlocFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeBloc::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeBloc(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
