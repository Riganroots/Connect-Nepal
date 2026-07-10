package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val bio: String,
    val profilePictureUrl: String,
    val city: String,
    val interests: String, // Comma separated interests
    val followersCount: Int,
    val followingCount: Int,
    val isCurrentUser: Boolean = false,
    val email: String = "",
    val password: String = ""
)

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val category: String, // e.g. "Food & Cafés", "Drinks & Nightlife", "Events", "Sports", "Outdoor", "Meet People"
    val subCategory: String, // e.g. "Street Food", "Live Music", "Hiking", etc.
    val city: String,
    val location: String,
    val date: String,
    val time: String,
    val maxParticipants: Int,
    val cost: String, // "Free" or price
    val meetingPoint: String,
    val visibility: String, // "Public" or "Friends"
    val coverImageUrl: String,
    val organizerId: Int,
    val organizerName: String,
    val organizerAvatar: String,
    val organizerBio: String = "",
    val latitude: Double = 27.7172, // Kathmandu default
    val longitude: Double = 85.3240,
    val createdAt: Long = System.currentTimeMillis(),
    val participantCount: Int = 1,
    val isSaved: Boolean = false,
    val isJoined: Boolean = false
)

@Entity(tableName = "activity_participants")
data class ParticipantEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val activityId: Int,
    val userId: Int,
    val userName: String,
    val userAvatar: String
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val activityId: Int,
    val senderId: Int,
    val senderName: String,
    val senderAvatar: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
