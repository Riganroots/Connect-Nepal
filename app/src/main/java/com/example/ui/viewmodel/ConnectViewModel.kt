package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.CloudRepository
import com.example.data.repository.ConnectRepository
import com.example.data.repository.InterestsRepository
import com.example.data.security.PasswordHasher
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConnectViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("connect_prefs", Context.MODE_PRIVATE)
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val cloud = CloudRepository.createIfConfigured(application, database.connectDao())
    private val repository = ConnectRepository(database.connectDao(), cloud)
    private val interestsRepository = InterestsRepository(database.connectDao())

    /** True when Firebase is configured, so accounts, activities and chats are shared online. */
    val isCloudEnabled: Boolean = cloud != null

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

    init {
        cloud?.let { c ->
            viewModelScope.launch {
                // Keep Room's signed-in user consistent with Firebase Auth, e.g. after the app's
                // data is restored onto a new device or a local-only account from an older build.
                val dao = database.connectDao()
                val localUser = dao.getCurrentUser().first()
                if (localUser != null && (localUser.remoteId == null || localUser.remoteId != c.currentUid)) {
                    c.signOut()
                    dao.clearCurrentUser()
                    resetOnboarding()
                }
            }
            viewModelScope.launch {
                // Mirror shared activities into Room while someone is signed in.
                repository.currentUser
                    .map { it?.remoteId }
                    .distinctUntilChanged()
                    .collectLatest { remoteId ->
                        if (remoteId != null && remoteId == c.currentUid) c.syncActivities()
                    }
            }
        }
    }

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
                repository.joinActivity(activityId, user)
            }
        }
    }

    fun leaveActivity(activityId: Int) {
        viewModelScope.launch {
            currentUser.value?.let { user ->
                repository.leaveActivity(activityId, user)
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
                repository.sendMessage(activityId, user, text)
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
            val user = currentUser.value ?: return@launch
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
            repository.createActivity(newAct, user)
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
            if (cloud != null) {
                val error = runCloudAuth { cloud.signIn(normalizeEmail(email), password) }
                if (error == null) {
                    _isOnboarded.value = true
                    sharedPrefs.edit().putBoolean("is_onboarded", true).apply()
                }
                _loginError.value = error
                onComplete(error == null)
                return@launch
            }
            val user = database.connectDao().getUserByEmail(normalizeEmail(email))
            if (user != null) {
                val passwordMatches = withContext(Dispatchers.Default) {
                    PasswordHasher.verify(password, user.password)
                }
                if (passwordMatches) {
                    database.connectDao().clearCurrentUser()
                    database.connectDao().updateUser(user.copy(isCurrentUser = true))
                    _isOnboarded.value = true
                    sharedPrefs.edit().putBoolean("is_onboarded", true).apply()
                    onComplete(true)
                } else {
                    _loginError.value = "Incorrect email or password"
                    onComplete(false)
                }
            } else {
                _loginError.value = "Incorrect email or password"
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
            val normalizedEmail = normalizeEmail(email)
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
                _loginError.value = "Please enter a valid email address"
                onComplete(false)
                return@launch
            }
            if (password.length < MIN_PASSWORD_LENGTH) {
                _loginError.value = "Password must be at least $MIN_PASSWORD_LENGTH characters"
                onComplete(false)
                return@launch
            }
            if (cloud != null) {
                val error = runCloudAuth {
                    cloud.signUp(
                        name = name.trim(),
                        email = normalizedEmail,
                        password = password,
                        city = city.ifBlank { "Kathmandu" },
                        bio = bio.ifBlank { "Excited to meet new people!" },
                        interests = interests.ifBlank { "Meetups, Outdoors, Cafés" }
                            .split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        profilePictureUrl = DEFAULT_AVATAR_URL
                    )
                }
                if (error == null) {
                    _isOnboarded.value = true
                    sharedPrefs.edit().putBoolean("is_onboarded", true).apply()
                }
                _loginError.value = error
                onComplete(error == null)
                return@launch
            }
            val existing = database.connectDao().getUserByEmail(normalizedEmail)
            if (existing != null) {
                _loginError.value = "A user with this email already exists"
                onComplete(false)
                return@launch
            }
            val passwordHash = withContext(Dispatchers.Default) { PasswordHasher.hash(password) }
            database.connectDao().clearCurrentUser()
            val newUser = UserEntity(
                name = name.trim(),
                bio = bio.ifBlank { "Excited to meet new people!" },
                profilePictureUrl = DEFAULT_AVATAR_URL,
                city = city.ifBlank { "Kathmandu" },
                interests = interests.ifBlank { "Meetups, Outdoors, Cafés" },
                followersCount = 0,
                followingCount = 0,
                isCurrentUser = true,
                email = normalizedEmail,
                password = passwordHash
            )
            database.connectDao().insertUser(newUser)
            _isOnboarded.value = true
            sharedPrefs.edit().putBoolean("is_onboarded", true).apply()
            onComplete(true)
        }
    }

    private fun normalizeEmail(email: String): String = email.trim().lowercase()

    /** Runs a Firebase Auth call and returns a user-facing error message, or null on success. */
    private suspend fun runCloudAuth(block: suspend () -> Unit): String? = try {
        block()
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseNetworkException) {
        "No internet connection. Please try again."
    } catch (e: FirebaseAuthException) {
        when (e.errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE" -> "A user with this email already exists"
            "ERROR_WEAK_PASSWORD" -> "Please choose a stronger password"
            "ERROR_INVALID_EMAIL" -> "Please enter a valid email address"
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please wait a moment and try again."
            "ERROR_USER_DISABLED" -> "This account has been disabled"
            else -> "Incorrect email or password"
        }
    } catch (e: Exception) {
        android.util.Log.e("ConnectViewModel", "Authentication failed", e)
        "Something went wrong. Please try again."
    }

    fun logout() {
        viewModelScope.launch {
            cloud?.signOut()
            database.connectDao().clearCurrentUser()
            resetOnboarding()
        }
    }

    /**
     * Permanently deletes the signed-in account and its online data (required by Google Play for
     * apps with account sign-up). Reports a user-facing error message, or null on success.
     */
    fun deleteAccount(password: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val dao = database.connectDao()
            val user = dao.getCurrentUser().first()
            val error = try {
                if (cloud != null) {
                    cloud.deleteAccount(password)
                    cloud.signOut()
                } else if (user != null && !withContext(Dispatchers.Default) { PasswordHasher.verify(password, user.password) }) {
                    onResult("Incorrect password")
                    return@launch
                }
                if (user != null) {
                    dao.clearCurrentUser()
                    // Strip credentials so the account can no longer be logged into on this device.
                    dao.updateUser(user.copy(isCurrentUser = false, email = "", password = ""))
                }
                resetOnboarding()
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: FirebaseNetworkException) {
                "No internet connection. Please try again."
            } catch (e: FirebaseAuthException) {
                "Incorrect password"
            } catch (e: Exception) {
                android.util.Log.e("ConnectViewModel", "Account deletion failed", e)
                "Could not delete your account. Please check your connection and try again."
            }
            onResult(error)
        }
    }

    fun getOtherUserStream(userId: Int): Flow<UserEntity?> {
        cloud?.let { c -> viewModelScope.launch { c.refreshProfile(userId) } }
        return database.connectDao().getUserById(userId)
    }

    fun getJoinedActivitiesStream(userId: Int): Flow<List<ActivityEntity>> {
        return repository.getActivitiesJoinedByUser(userId)
    }

    fun getCreatedActivitiesStream(userId: Int): Flow<List<ActivityEntity>> {
        return database.connectDao().getAllActivities().map { list ->
            list.filter { it.organizerId == userId }
        }
    }

    // --- User Interests & Sync with Firebase ---
    // Interests are stored under the Firebase uid for cloud users, else under the local id.
    private suspend fun interestsKey(userId: Int): String =
        database.connectDao().getUserByIdOnce(userId)?.remoteId ?: userId.toString()

    fun getUserInterestsStream(userId: Int): Flow<List<UserInterest>> = flow {
        emitAll(interestsRepository.syncAndGetInterests(interestsKey(userId)))
    }

    fun attachInterest(userId: Int, interestName: String, category: String = "General") {
        viewModelScope.launch {
            interestsRepository.attachInterestToProfile(interestsKey(userId), interestName, category)
        }
    }

    fun detachInterest(userId: Int, interestName: String) {
        viewModelScope.launch {
            interestsRepository.detachInterestFromProfile(interestsKey(userId), interestName)
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
                // Gemini is called through Firebase AI Logic so no API key ships inside the APK.
                // This needs the app registered in Firebase (google-services.json) with AI Logic enabled.
                if (FirebaseApp.getApps(getApplication<Application>()).isEmpty()) {
                    _aiMessages.value = _aiMessages.value + Pair("The AI guide isn't available in this build yet. Please try again in a later update.", false)
                    return@launch
                }
                val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                    modelName = AI_GUIDE_MODEL,
                    systemInstruction = content { text(AI_GUIDE_SYSTEM_INSTRUCTION) }
                )
                val response = model.generateContent(prompt)
                val text = response.text?.takeIf { it.isNotBlank() } ?: "Sorry, I couldn't understand that response."
                _aiMessages.value = _aiMessages.value + Pair(text, false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("ConnectViewModel", "AI guide request failed", e)
                _aiMessages.value = _aiMessages.value + Pair("Sorry, I couldn't reach the guide right now. Please check your connection and try again.", false)
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    // --- Block & Report States ---
    private val _blockedUserIds = MutableStateFlow(
        sharedPrefs.getStringSet("blocked_user_ids", emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
    )
    val blockedUserIds: StateFlow<Set<Int>> = _blockedUserIds.asStateFlow()

    fun blockUser(userId: Int) {
        updateBlockedUsers(_blockedUserIds.value + userId)
    }

    fun unblockUser(userId: Int) {
        updateBlockedUsers(_blockedUserIds.value - userId)
    }

    private fun updateBlockedUsers(ids: Set<Int>) {
        _blockedUserIds.value = ids
        sharedPrefs.edit().putStringSet("blocked_user_ids", ids.map { it.toString() }.toSet()).apply()
    }

    private val _reportedUsers = MutableStateFlow<Map<Int, String>>(emptyMap()) // UserId -> Reason
    val reportedUsers: StateFlow<Map<Int, String>> = _reportedUsers.asStateFlow()

    fun reportUser(userId: Int, reason: String) {
        _reportedUsers.value = _reportedUsers.value + (userId to reason)
        cloud?.let { c -> viewModelScope.launch { c.reportUser(userId, reason) } }
    }
}

private const val MIN_PASSWORD_LENGTH = 8

private const val DEFAULT_AVATAR_URL = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?q=80&w=200"

private const val AI_GUIDE_MODEL = "gemini-3.5-flash"

private const val AI_GUIDE_SYSTEM_INSTRUCTION =
    "You are a professional, helpful and friendly local guide for Kathmandu and Nepal. " +
        "Provide direct, highly relevant, and visually structured local recommendations. Use clean spacing and bullet points."

class ConnectViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConnectViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ConnectViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
