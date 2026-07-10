package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.ConnectRepository
import com.example.data.repository.InterestsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class ConnectViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("connect_prefs", Context.MODE_PRIVATE)
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = ConnectRepository(database.connectDao())
    private val interestsRepository = InterestsRepository(database.connectDao())

    // UI Configuration States
    private val _isDarkMode = MutableStateFlow(sharedPrefs.getBoolean("dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _language = MutableStateFlow(sharedPrefs.getString("language", "English") ?: "English")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(sharedPrefs.getBoolean("notifications", true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _privacyEnabled = MutableStateFlow(sharedPrefs.getBoolean("privacy_friends", false))
    val privacyEnabled: StateFlow<Boolean> = _privacyEnabled.asStateFlow()

    // Map & Self-Serve Auth Configuration
    private val _mapSource = MutableStateFlow(sharedPrefs.getString("map_source", "OpenStreetMap") ?: "OpenStreetMap")
    val mapSource: StateFlow<String> = _mapSource.asStateFlow()

    private val _googleMapsApiKey = MutableStateFlow(sharedPrefs.getString("google_maps_api_key", "") ?: "")
    val googleMapsApiKey: StateFlow<String> = _googleMapsApiKey.asStateFlow()

    private val _googleClientId = MutableStateFlow(sharedPrefs.getString("google_client_id", "") ?: "")
    val googleClientId: StateFlow<String> = _googleClientId.asStateFlow()

    private val _manualOAuthEnabled = MutableStateFlow(sharedPrefs.getBoolean("manual_oauth_enabled", false))
    val manualOAuthEnabled: StateFlow<Boolean> = _manualOAuthEnabled.asStateFlow()

    // Dynamic Navigation & UI states
    private val _isOnboarded = MutableStateFlow(sharedPrefs.getBoolean("is_onboarded", false))
    val isOnboarded: StateFlow<Boolean> = _isOnboarded.asStateFlow()

    // Current User State
    val currentUser: StateFlow<UserEntity?> = repository.currentUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Current City - Derived from user profile, falls back to preferences
    val currentCity: StateFlow<String> = currentUser
        .map { user -> user?.city ?: sharedPrefs.getString("selected_city", "Kathmandu") ?: "Kathmandu" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = sharedPrefs.getString("selected_city", "Kathmandu") ?: "Kathmandu"
        )

    // Active Category Filter for Home
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // Filtered Activities by City & Category
    @OptIn(ExperimentalCoroutinesApi::class)
    val activities: StateFlow<List<ActivityEntity>> = combine(
        currentCity,
        _selectedCategory
    ) { city, category ->
        Pair(city, category)
    }.flatMapLatest { (city, category) ->
        repository.getActivitiesByCity(city).map { list ->
            if (category == null) list else list.filter { it.category == category }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Kathmandu activities for Map visualization
    val kathmanduActivities: StateFlow<List<ActivityEntity>> = repository.getActivitiesByCity("Kathmandu")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Saved & Joined activities
    val savedActivities: StateFlow<List<ActivityEntity>> = repository.getSavedActivities().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val joinedActivities: StateFlow<List<ActivityEntity>> = repository.getJoinedActivities().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Search query states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchType = MutableStateFlow("Activities") // "Activities", "Places", "Events", "Users"
    val searchType: StateFlow<String> = _searchType.asStateFlow()

    // Search activities list
    @OptIn(ExperimentalCoroutinesApi::class)
    val searchActivitiesResult: StateFlow<List<ActivityEntity>> = combine(
        _searchQuery,
        currentCity
    ) { query, city ->
        Pair(query, city)
    }.flatMapLatest { (query, city) ->
        if (query.isBlank()) {
            repository.getActivitiesByCity(city)
        } else {
            repository.searchActivities(query, city)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Search users list
    @OptIn(ExperimentalCoroutinesApi::class)
    val searchUsersResult: StateFlow<List<UserEntity>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            repository.getAllUsers()
        } else {
            repository.searchUsers(query)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Settings modifiers
    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        sharedPrefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun setLanguage(lang: String) {
        _language.value = lang
        sharedPrefs.edit().putString("language", lang).apply()
    }

    fun updateMapSource(source: String) {
        _mapSource.value = source
        sharedPrefs.edit().putString("map_source", source).apply()
    }

    fun updateGoogleMapsApiKey(key: String) {
        _googleMapsApiKey.value = key
        sharedPrefs.edit().putString("google_maps_api_key", key).apply()
    }

    fun updateGoogleClientId(clientId: String) {
        _googleClientId.value = clientId
        sharedPrefs.edit().putString("google_client_id", clientId).apply()
    }

    fun toggleManualOAuth(enabled: Boolean) {
        _manualOAuthEnabled.value = enabled
        sharedPrefs.edit().putBoolean("manual_oauth_enabled", enabled).apply()
    }

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        sharedPrefs.edit().putBoolean("notifications", enabled).apply()
    }

    fun togglePrivacy(enabled: Boolean) {
        _privacyEnabled.value = enabled
        sharedPrefs.edit().putBoolean("privacy_friends", enabled).apply()
    }

    fun selectCity(city: String) {
        viewModelScope.launch {
            currentUser.value?.let { user ->
                repository.updateUser(user.copy(city = city))
            }
            sharedPrefs.edit().putString("selected_city", city).apply()
        }
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchType(type: String) {
        _searchType.value = type
    }

    fun completeOnboarding(city: String) {
        viewModelScope.launch {
            selectCity(city)
            _isOnboarded.value = true
            sharedPrefs.edit().putBoolean("is_onboarded", true).apply()
        }
    }

    fun resetOnboarding() {
        _isOnboarded.value = false
        sharedPrefs.edit().putBoolean("is_onboarded", false).apply()
    }

    // Database Actions
    fun toggleSaveActivity(activityId: Int) {
        viewModelScope.launch {
            repository.toggleSaveActivity(activityId)
        }
    }

    fun joinActivity(activityId: Int) {
        viewModelScope.launch {
            currentUser.value?.let { user ->
                repository.joinActivity(
                    activityId = activityId,
                    userId = user.id,
                    userName = user.name,
                    userAvatar = user.profilePictureUrl
                )
            }
        }
    }

    fun leaveActivity(activityId: Int) {
        viewModelScope.launch {
            currentUser.value?.let { user ->
                repository.leaveActivity(activityId, user.id)
            }
        }
    }

    // Get live details stream
    fun getActivityStream(activityId: Int): Flow<ActivityEntity?> {
        return repository.getActivityById(activityId)
    }

    fun getParticipantsStream(activityId: Int): Flow<List<ParticipantEntity>> {
        return repository.getParticipantsByActivity(activityId)
    }

    fun getMessagesStream(activityId: Int): Flow<List<MessageEntity>> {
        return repository.getMessagesByActivity(activityId)
    }

    fun sendChatMessage(activityId: Int, text: String) {
        viewModelScope.launch {
            currentUser.value?.let { user ->
                repository.sendMessage(
                    activityId = activityId,
                    senderId = user.id,
                    senderName = user.name,
                    senderAvatar = user.profilePictureUrl,
                    text = text
                )
            }
        }
    }

    fun createNewActivity(
        title: String,
        description: String,
        category: String,
        subCategory: String,
        city: String,
        location: String,
        date: String,
        time: String,
        maxParticipants: Int,
        cost: String,
        meetingPoint: String,
        visibility: String,
        coverImageUrl: String
    ) {
        viewModelScope.launch {
            val user = currentUser.value ?: MockData.currentUser
            val newAct = ActivityEntity(
                title = title,
                description = description,
                category = category,
                subCategory = subCategory,
                city = city,
                location = location,
                date = date,
                time = time,
                maxParticipants = maxParticipants,
                cost = cost,
                meetingPoint = meetingPoint,
                visibility = visibility,
                coverImageUrl = coverImageUrl.ifBlank {
                    // Provide beautiful fallback based on category
                    when (category) {
                        "Food & Cafés" -> "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?q=80&w=600"
                        "Drinks & Nightlife" -> "https://images.unsplash.com/photo-1528605248644-14dd04022da1?q=80&w=600"
                        "Events" -> "https://images.unsplash.com/photo-1511578314322-379afb476865?q=80&w=600"
                        "Sports" -> "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=600"
                        "Outdoor" -> "https://images.unsplash.com/photo-1454496522488-7a8e488e8606?q=80&w=600"
                        else -> "https://images.unsplash.com/photo-1511578314322-379afb476865?q=80&w=600"
                    }
                },
                organizerId = user.id,
                organizerName = user.name,
                organizerAvatar = user.profilePictureUrl,
                organizerBio = user.bio,
                participantCount = 1,
                isJoined = true
            )
            repository.insertActivity(newAct)
        }
    }

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    fun login(email: String, password: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _loginError.value = null
            if (email.isBlank() || password.isBlank()) {
                _loginError.value = "Email and password cannot be empty"
                onComplete(false)
                return@launch
            }
            val user = database.connectDao().getUserByEmail(email)
            if (user != null) {
                if (user.password == password) {
                    database.connectDao().clearCurrentUser()
                    database.connectDao().updateUser(user.copy(isCurrentUser = true))
                    _isOnboarded.value = true
                    sharedPrefs.edit().putBoolean("is_onboarded", true).apply()
                    onComplete(true)
                } else {
                    _loginError.value = "Incorrect password"
                    onComplete(false)
                }
            } else {
                _loginError.value = "No user found with this email"
                onComplete(false)
            }
        }
    }

    fun signup(
        name: String,
        email: String,
        password: String,
        city: String,
        bio: String,
        interests: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _loginError.value = null
            if (name.isBlank() || email.isBlank() || password.isBlank()) {
                _loginError.value = "Name, email, and password are required"
                onComplete(false)
                return@launch
            }
            val existing = database.connectDao().getUserByEmail(email)
            if (existing != null) {
                _loginError.value = "A user with this email already exists"
                onComplete(false)
                return@launch
            }
            database.connectDao().clearCurrentUser()
            val newUser = UserEntity(
                name = name,
                bio = bio.ifBlank { "Excited to meet new people!" },
                profilePictureUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?q=80&w=200", // Beautiful default avatar
                city = city.ifBlank { "Kathmandu" },
                interests = interests.ifBlank { "Meetups, Outdoors, Cafés" },
                followersCount = 0,
                followingCount = 0,
                isCurrentUser = true,
                email = email,
                password = password
            )
            database.connectDao().insertUser(newUser)
            _isOnboarded.value = true
            sharedPrefs.edit().putBoolean("is_onboarded", true).apply()
            onComplete(true)
        }
    }

    fun logout() {
        viewModelScope.launch {
            database.connectDao().clearCurrentUser()
            resetOnboarding()
        }
    }

    fun getOtherUserStream(userId: Int): Flow<UserEntity?> {
        return database.connectDao().getUserById(userId)
    }

    fun getCreatedActivitiesStream(userId: Int): Flow<List<ActivityEntity>> {
        return database.connectDao().getAllActivities().map { list ->
            list.filter { it.organizerId == userId }
        }
    }

    // --- User Interests & Sync with Firebase ---
    fun getUserInterestsStream(userId: Int): Flow<List<UserInterest>> {
        return interestsRepository.syncAndGetInterests(userId.toString())
    }

    fun attachInterest(userId: Int, interestName: String, category: String = "General") {
        viewModelScope.launch {
            interestsRepository.attachInterestToProfile(userId.toString(), interestName, category)
        }
    }

    fun detachInterest(userId: Int, interestName: String) {
        viewModelScope.launch {
            interestsRepository.detachInterestFromProfile(userId.toString(), interestName)
        }
    }

    // --- Nearby Users ---
    val allUsers: StateFlow<List<UserEntity>> = database.connectDao().getAllUsers().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- AI Local Guide States ---
    private val _aiMessages = MutableStateFlow<List<Pair<String, Boolean>>>(listOf(
        Pair("Hello! I am your AI Local Guide 🗺️. Ask me anything about where to go, peaceful cafes, solo travel spots, or plans in Kathmandu and Nepal!", false)
    )) // Pair of (Text, isUser)
    val aiMessages: StateFlow<List<Pair<String, Boolean>>> = _aiMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    fun clearAiMessages() {
        _aiMessages.value = listOf(
            Pair("Hello! I am your AI Local Guide 🗺️. Ask me anything about where to go, peaceful cafes, solo travel spots, or plans in Kathmandu and Nepal!", false)
        )
    }

    fun askAiGuide(prompt: String) {
        if (prompt.isBlank()) return
        // Append user prompt
        _aiMessages.value = _aiMessages.value + Pair(prompt, true)
        _isAiLoading.value = true

        viewModelScope.launch {
            try {
                val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    _aiMessages.value = _aiMessages.value + Pair("Guide: I don't have a valid Gemini API key configured. Please set GEMINI_API_KEY in the Secrets panel of AI Studio.", false)
                    _isAiLoading.value = false
                    return@launch
                }

                // Call Gemini API using Direct REST client or simple HTTP request via OkHttp
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                
                // Construct clean prompt & system instruction payload
                val systemInstruction = "You are a professional, helpful and friendly local guide for Kathmandu and Nepal. " +
                        "Provide direct, highly relevant, and visually structured local recommendations. Use clean spacing and bullet points."
                
                val jsonBody = """
                    {
                      "contents": [
                        {
                          "parts": [
                            {
                              "text": ${org.json.JSONObject.quote(prompt)}
                            }
                          ]
                        }
                      ],
                      "systemInstruction": {
                        "parts": [
                          {
                            "text": ${org.json.JSONObject.quote(systemInstruction)}
                          }
                        ]
                      }
                    }
                """.trimIndent()

                val okHttpClient = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val request = okhttp3.Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                    .post(okhttp3.RequestBody.create(mediaType, jsonBody))
                    .build()

                // Execute asynchronously
                val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    okHttpClient.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val jsonObject = org.json.JSONObject(responseBody)
                        val candidates = jsonObject.optJSONArray("candidates")
                        val firstCandidate = candidates?.optJSONObject(0)
                        val content = firstCandidate?.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        val text = parts?.optJSONObject(0)?.optString("text") ?: "Sorry, I couldn't understand that response."
                        _aiMessages.value = _aiMessages.value + Pair(text, false)
                    } else {
                        _aiMessages.value = _aiMessages.value + Pair("Error: Empty response from AI guide.", false)
                    }
                } else {
                    _aiMessages.value = _aiMessages.value + Pair("Error: Server returned code ${response.code}", false)
                }
            } catch (e: Exception) {
                _aiMessages.value = _aiMessages.value + Pair("Error contacting guide: ${e.message}", false)
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    // --- Block & Report States ---
    private val _blockedUserIds = MutableStateFlow<Set<Int>>(emptySet())
    val blockedUserIds: StateFlow<Set<Int>> = _blockedUserIds.asStateFlow()

    fun blockUser(userId: Int) {
        _blockedUserIds.value = _blockedUserIds.value + userId
    }

    fun unblockUser(userId: Int) {
        _blockedUserIds.value = _blockedUserIds.value - userId
    }

    private val _reportedUsers = MutableStateFlow<Map<Int, String>>(emptyMap()) // UserId -> Reason
    val reportedUsers: StateFlow<Map<Int, String>> = _reportedUsers.asStateFlow()

    fun reportUser(userId: Int, reason: String) {
        _reportedUsers.value = _reportedUsers.value + (userId to reason)
    }
}

class ConnectViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConnectViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ConnectViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
