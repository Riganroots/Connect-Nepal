package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

/**
 * Firestore-serializable Activity Participant representation.
 * Supports zero-argument constructor required by Firestore.
 */
data class ActivityParticipant(
    @get:PropertyName("userId") @set:PropertyName("userId") var userId: String = "",
    @get:PropertyName("userName") @set:PropertyName("userName") var userName: String = "",
    @get:PropertyName("userAvatar") @set:PropertyName("userAvatar") var userAvatar: String = ""
)

/**
 * Production-ready Firestore-serializable Activity model class.
 * Includes all requested fields: cityId, categoryId, location, timestamp, organizerId, and participant tracking.
 * Provides default values for all fields to support zero-argument constructor serialization.
 */
data class Activity(
    @DocumentId
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    
    @get:PropertyName("title") @set:PropertyName("title") var title: String = "",
    @get:PropertyName("description") @set:PropertyName("description") var description: String = "",
    
    // Requested fields
    @get:PropertyName("cityId") @set:PropertyName("cityId") var cityId: String = "",
    @get:PropertyName("categoryId") @set:PropertyName("categoryId") var categoryId: String = "",
    @get:PropertyName("location") @set:PropertyName("location") var location: String = "",
    @ServerTimestamp
    @get:PropertyName("timestamp") @set:PropertyName("timestamp") var timestamp: Timestamp? = null,
    @get:PropertyName("organizerId") @set:PropertyName("organizerId") var organizerId: String = "",
    
    // Additional fields aligned with ActivityEntity
    @get:PropertyName("organizerName") @set:PropertyName("organizerName") var organizerName: String = "",
    @get:PropertyName("organizerAvatar") @set:PropertyName("organizerAvatar") var organizerAvatar: String = "",
    @get:PropertyName("meetingPoint") @set:PropertyName("meetingPoint") var meetingPoint: String = "",
    @get:PropertyName("coverImageUrl") @set:PropertyName("coverImageUrl") var coverImageUrl: String = "",
    @get:PropertyName("maxParticipants") @set:PropertyName("maxParticipants") var maxParticipants: Int = 10,
    @get:PropertyName("cost") @set:PropertyName("cost") var cost: String = "Free",
    @get:PropertyName("visibility") @set:PropertyName("visibility") var visibility: String = "Public",
    @get:PropertyName("latitude") @set:PropertyName("latitude") var latitude: Double = 0.0,
    @get:PropertyName("longitude") @set:PropertyName("longitude") var longitude: Double = 0.0,
    
    // Participant tracking fields requested
    @get:PropertyName("participantCount") @set:PropertyName("participantCount") var participantCount: Int = 1,
    @get:PropertyName("participantIds") @set:PropertyName("participantIds") var participantIds: List<String> = emptyList(),
    @get:PropertyName("participants") @set:PropertyName("participants") var participants: List<ActivityParticipant> = emptyList()
)
