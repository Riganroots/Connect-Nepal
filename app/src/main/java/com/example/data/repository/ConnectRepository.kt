package com.example.data.repository

import com.example.data.database.ConnectDao
import com.example.data.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class ConnectRepository(private val connectDao: ConnectDao) {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            android.util.Log.e("ConnectRepository", "Failed to initialize Firestore: ${e.message}")
            null
        }
    }

    val currentUser: Flow<UserEntity?> = connectDao.getCurrentUser()

    fun getAllUsers(): Flow<List<UserEntity>> = connectDao.getAllUsers()

    fun searchUsers(query: String): Flow<List<UserEntity>> = connectDao.searchUsers(query)

    fun getActivitiesByCity(city: String): Flow<List<ActivityEntity>> = 
        connectDao.getActivitiesByCity(city)

    fun getSavedActivities(): Flow<List<ActivityEntity>> = 
        connectDao.getSavedActivities()

    fun getJoinedActivities(): Flow<List<ActivityEntity>> = 
        connectDao.getJoinedActivities()

    fun getActivityById(id: Int): Flow<ActivityEntity?> = 
        connectDao.getActivityById(id)

    fun getParticipantsByActivity(activityId: Int): Flow<List<ParticipantEntity>> = 
        connectDao.getParticipantsByActivity(activityId)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getMessagesByActivity(activityId: Int): Flow<List<MessageEntity>> {
        val localFlow = connectDao.getMessagesByActivity(activityId)
        val db = firestore ?: return localFlow

        return callbackFlow<Unit> {
            val listenerRegistration = db.collection("messages")
                .whereEqualTo("activityId", activityId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("ConnectRepository", "Firestore listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        launch {
                            for (doc in snapshot.documents) {
                                try {
                                    val msgId = doc.getLong("id")?.toInt() ?: continue
                                    val actId = doc.getLong("activityId")?.toInt() ?: activityId
                                    val sendId = doc.getLong("senderId")?.toInt() ?: 0
                                    val sendName = doc.getString("senderName") ?: ""
                                    val sendAvatar = doc.getString("senderAvatar") ?: ""
                                    val msgText = doc.getString("text") ?: ""
                                    val ts = doc.getLong("timestamp") ?: System.currentTimeMillis()

                                    val message = MessageEntity(
                                        id = msgId,
                                        activityId = actId,
                                        senderId = sendId,
                                        senderName = sendName,
                                        senderAvatar = sendAvatar,
                                        text = msgText,
                                        timestamp = ts
                                    )
                                    connectDao.insertMessage(message)
                                } catch (e: Exception) {
                                    android.util.Log.e("ConnectRepository", "Mapping error: ${e.message}")
                                }
                            }
                            trySend(Unit)
                        }
                    }
                }
            awaitClose {
                listenerRegistration.remove()
            }
        }.flatMapLatest {
            localFlow
        }
    }

    suspend fun insertActivity(activity: ActivityEntity): Long {
        val id = connectDao.insertActivity(activity)
        // Automatically add the organizer as a participant
        connectDao.insertParticipant(
            ParticipantEntity(
                activityId = id.toInt(),
                userId = activity.organizerId,
                userName = activity.organizerName,
                userAvatar = activity.coverImageUrl // Or organizer avatar
            )
        )
        return id
    }

    suspend fun updateActivity(activity: ActivityEntity) {
        connectDao.updateActivity(activity)
    }

    suspend fun toggleSaveActivity(activityId: Int) {
        val activity = connectDao.getActivityById(activityId).firstOrNull()
        if (activity != null) {
            connectDao.updateActivity(activity.copy(isSaved = !activity.isSaved))
        }
    }

    suspend fun joinActivity(activityId: Int, userId: Int, userName: String, userAvatar: String) {
        val activity = connectDao.getActivityById(activityId).firstOrNull()
        if (activity != null && !activity.isJoined) {
            val updated = activity.copy(
                isJoined = true,
                participantCount = activity.participantCount + 1
            )
            connectDao.updateActivity(updated)
            connectDao.insertParticipant(
                ParticipantEntity(
                    activityId = activityId,
                    userId = userId,
                    userName = userName,
                    userAvatar = userAvatar
                )
            )
            // Add automatic system join message
            sendMessage(
                activityId = activityId,
                senderId = userId,
                senderName = userName,
                senderAvatar = userAvatar,
                text = "👋 Joined the activity! Let's do this together!"
            )
        }
    }

    suspend fun leaveActivity(activityId: Int, userId: Int) {
        val activity = connectDao.getActivityById(activityId).firstOrNull()
        if (activity != null && activity.isJoined) {
            val updated = activity.copy(
                isJoined = false,
                participantCount = (activity.participantCount - 1).coerceAtLeast(1)
            )
            connectDao.updateActivity(updated)
            connectDao.deleteParticipant(activityId, userId)
            // Add system leave message
            sendMessage(
                activityId = activityId,
                senderId = userId,
                senderName = "System",
                senderAvatar = "",
                text = "Left the activity."
            )
        }
    }

    suspend fun sendMessage(activityId: Int, senderId: Int, senderName: String, senderAvatar: String, text: String) {
        val uniqueId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val timestamp = System.currentTimeMillis()

        val message = MessageEntity(
            id = uniqueId,
            activityId = activityId,
            senderId = senderId,
            senderName = senderName,
            senderAvatar = senderAvatar,
            text = text,
            timestamp = timestamp
        )

        connectDao.insertMessage(message)

        val db = firestore
        if (db != null) {
            try {
                val data = hashMapOf(
                    "id" to uniqueId,
                    "activityId" to activityId,
                    "senderId" to senderId,
                    "senderName" to senderName,
                    "senderAvatar" to senderAvatar,
                    "text" to text,
                    "timestamp" to timestamp
                )
                db.collection("messages")
                    .document(uniqueId.toString())
                    .set(data)
            } catch (e: Exception) {
                android.util.Log.e("ConnectRepository", "Error sending message to Firestore: ${e.message}")
            }
        }
    }

    fun searchActivities(query: String, city: String): Flow<List<ActivityEntity>> = 
        connectDao.searchActivities(query, city)

    suspend fun updateUser(user: UserEntity) {
        connectDao.updateUser(user)
    }
}
