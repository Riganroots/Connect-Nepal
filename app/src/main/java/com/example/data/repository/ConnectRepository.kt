package com.example.data.repository

import com.example.data.database.ConnectDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * App data access. Reads always come from Room. When [cloud] is available (Firebase configured
 * and the user signed in with Firebase Auth), activities, participants and chats for
 * Firestore-backed activities go through it; everything else stays local.
 */
class ConnectRepository(
    private val connectDao: ConnectDao,
    private val cloud: CloudRepository? = null
) {

    val currentUser: Flow<UserEntity?> = connectDao.getCurrentUser()

    fun getAllUsers(): Flow<List<UserEntity>> = connectDao.getAllUsers()

    fun searchUsers(query: String): Flow<List<UserEntity>> = connectDao.searchUsers(query)

    fun getActivitiesByCity(city: String): Flow<List<ActivityEntity>> = 
        connectDao.getActivitiesByCity(city)

    fun getSavedActivities(): Flow<List<ActivityEntity>> = 
        connectDao.getSavedActivities()

    fun getJoinedActivities(): Flow<List<ActivityEntity>> = 
        connectDao.getJoinedActivities()

    fun getActivitiesJoinedByUser(userId: Int): Flow<List<ActivityEntity>> =
        connectDao.getActivitiesJoinedByUser(userId)

    fun getActivityById(id: Int): Flow<ActivityEntity?> = 
        connectDao.getActivityById(id)

    fun getParticipantsByActivity(activityId: Int): Flow<List<ParticipantEntity>> = 
        connectDao.getParticipantsByActivity(activityId)

    /** Local messages, plus a live Firestore sync while collected for cloud-backed activities. */
    fun getMessagesByActivity(activityId: Int): Flow<List<MessageEntity>> = channelFlow {
        val remoteId = connectDao.getActivityByIdOnce(activityId)?.remoteId
        if (cloud != null && remoteId != null) {
            launch { cloud.syncMessages(remoteId, activityId) }
        }
        connectDao.getMessagesByActivity(activityId).collect { send(it) }
    }

    /** Creates an activity, in Firestore when signed in to the cloud, otherwise locally. */
    suspend fun createActivity(activity: ActivityEntity, organizer: UserEntity) {
        if (cloud != null && organizer.remoteId != null) {
            cloud.createActivity(activity, organizer)
        } else {
            insertActivity(activity)
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
                userAvatar = activity.organizerAvatar
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

    suspend fun joinActivity(activityId: Int, user: UserEntity) {
        val activity = connectDao.getActivityById(activityId).firstOrNull()
        if (activity != null && !activity.isJoined) {
            if (cloud != null && activity.remoteId != null) {
                cloud.joinActivity(activity.remoteId, activityId, user)
                return
            }
            val userId = user.id
            val userName = user.name
            val userAvatar = user.profilePictureUrl
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

    suspend fun leaveActivity(activityId: Int, user: UserEntity) {
        val activity = connectDao.getActivityById(activityId).firstOrNull()
        if (activity != null && activity.isJoined) {
            if (cloud != null && activity.remoteId != null) {
                cloud.leaveActivity(activity.remoteId, activityId, user)
                return
            }
            val userId = user.id
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
        connectDao.insertMessage(
            MessageEntity(
                activityId = activityId,
                senderId = senderId,
                senderName = senderName,
                senderAvatar = senderAvatar,
                text = text
            )
        )
    }

    /** Sends a chat message as [sender]; cloud activities only accept messages from participants. */
    suspend fun sendMessage(activityId: Int, sender: UserEntity, text: String) {
        val activity = connectDao.getActivityByIdOnce(activityId) ?: return
        if (cloud != null && activity.remoteId != null) {
            if (activity.isJoined) cloud.sendMessage(activity.remoteId, activityId, sender, text)
            return
        }
        sendMessage(activityId, sender.id, sender.name, sender.profilePictureUrl, text)
    }

    fun searchActivities(query: String, city: String): Flow<List<ActivityEntity>> = 
        connectDao.searchActivities(query, city)

    suspend fun updateUser(user: UserEntity) {
        connectDao.updateUser(user)
        cloud?.updateProfile(user)
    }
}
