package com.example.data.database

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectDao {

    // --- Users ---
    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: Int): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    fun getCurrentUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("UPDATE users SET isCurrentUser = 0")
    suspend fun clearCurrentUser()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE isCurrentUser = 0")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE name LIKE '%' || :query || '%' AND isCurrentUser = 0")
    fun searchUsers(query: String): Flow<List<UserEntity>>


    // --- Activities ---
    @Query("SELECT * FROM activities ORDER BY createdAt DESC")
    fun getAllActivities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE city = :city ORDER BY createdAt DESC")
    fun getActivitiesByCity(city: String): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE isSaved = 1 ORDER BY createdAt DESC")
    fun getSavedActivities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE isJoined = 1 ORDER BY createdAt DESC")
    fun getJoinedActivities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE id = :id")
    fun getActivityById(id: Int): Flow<ActivityEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity): Long

    @Update
    suspend fun updateActivity(activity: ActivityEntity)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteActivityById(id: Int)

    @Query("""
        SELECT * FROM activities 
        WHERE city = :city 
        AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR location LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
    """)
    fun searchActivities(query: String, city: String): Flow<List<ActivityEntity>>


    // --- Participants ---
    @Query("SELECT * FROM activity_participants WHERE activityId = :activityId")
    fun getParticipantsByActivity(activityId: Int): Flow<List<ParticipantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: ParticipantEntity)

    @Query("DELETE FROM activity_participants WHERE activityId = :activityId AND userId = :userId")
    suspend fun deleteParticipant(activityId: Int, userId: Int)


    // --- Messages ---
    @Query("SELECT * FROM messages WHERE activityId = :activityId ORDER BY timestamp ASC")
    fun getMessagesByActivity(activityId: Int): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    // --- User Interests ---
    @Query("SELECT * FROM user_interests WHERE userId = :userId")
    fun getUserInterests(userId: String): Flow<List<UserInterest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserInterest(interest: UserInterest)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserInterests(interests: List<UserInterest>)

    @Query("DELETE FROM user_interests WHERE userId = :userId")
    suspend fun deleteUserInterests(userId: String)

    @Query("DELETE FROM user_interests WHERE userId = :userId AND interestName = :interestName")
    suspend fun deleteUserInterest(userId: String, interestName: String)
}
