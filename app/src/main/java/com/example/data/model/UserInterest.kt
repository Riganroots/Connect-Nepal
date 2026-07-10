package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Data model for User Interests.
 * Supports standard constructor, Room Database entity, and Firebase Firestore serialization.
 */
@Entity(tableName = "user_interests")
data class UserInterest(
    @PrimaryKey
    @DocumentId
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    
    @get:PropertyName("userId") @set:PropertyName("userId") var userId: String = "",
    
    @get:PropertyName("interestName") @set:PropertyName("interestName") var interestName: String = "",
    
    @get:PropertyName("category") @set:PropertyName("category") var category: String = ""
)
