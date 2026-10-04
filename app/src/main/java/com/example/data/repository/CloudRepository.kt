package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.database.ConnectDao
import com.example.data.model.ActivityEntity
import com.example.data.model.MessageEntity
import com.example.data.model.ParticipantEntity
import com.example.data.model.UserEntity
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.tasks.await

/**
 * Firebase-backed data source: Firebase Auth accounts plus shared users, activities and group
 * chats in Firestore. Room stays the single source the UI reads from; this class mirrors
 * Firestore into it and sends the signed-in user's changes to Firestore.
 *
 * Firestore layout (see firestore.rules):
 * - users/{uid}: public profile (name, bio, profilePictureUrl, city, interests)
 * - activities/{id}: activity fields, organizerId (uid) and participants {uid: {name, avatar}}
 * - activities/{id}/messages/{id}: group chat, readable and writable by participants only
 * - reports/{id}: write-only abuse reports for moderation
 *
 * Writes are not awaited (except during sign-up/sign-in/account deletion) so they queue while
 * offline; Firestore applies them to its local cache immediately and the snapshot listeners
 * mirror that into Room.
 */
class CloudRepository private constructor(private val dao: ConnectDao) {

    private val auth get() = Firebase.auth
    private val db get() = Firebase.firestore

    val currentUid: String? get() = auth.currentUser?.uid

    // --- Accounts ---

    suspend fun signUp(
        name: String,
        email: String,
        password: String,
        city: String,
        bio: String,
        interests: List<String>,
        profilePictureUrl: String
    ): UserEntity {
        val uid = auth.createUserWithEmailAndPassword(email, password).await().user?.uid
            ?: error("Sign-up returned no user")
        val profile = hashMapOf(
            "name" to name.take(80),
            "bio" to bio.take(500),
            "profilePictureUrl" to profilePictureUrl,
            "city" to city,
            "interests" to interests,
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection(USERS).document(uid).set(profile, SetOptions.merge()).await()
        return saveCurrentUser(uid, email, name, bio, profilePictureUrl, city, interests)
    }

    suspend fun signIn(email: String, password: String): UserEntity {
        val uid = auth.signInWithEmailAndPassword(email, password).await().user?.uid
            ?: error("Sign-in returned no user")
        val profile = db.collection(USERS).document(uid).get().await()
        return saveCurrentUser(
            uid = uid,
            email = email,
            name = profile.getString("name") ?: email.substringBefore("@"),
            bio = profile.getString("bio").orEmpty(),
            profilePictureUrl = profile.getString("profilePictureUrl").orEmpty(),
            city = profile.getString("city") ?: "Kathmandu",
            interests = profile.stringList("interests")
        )
    }

    fun signOut() {
        auth.signOut()
    }

    /**
     * Deletes the signed-in user's Firestore data and Firebase Auth account. The password is
     * checked first (Firebase requires a recent sign-in to delete an account), so nothing is
     * deleted if it is wrong.
     */
    suspend fun deleteAccount(password: String) {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val email = user.email ?: error("Account has no email address")
        user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()

        // Activities this user organises (and their chats' parent docs) are removed.
        val organised = db.collection(ACTIVITIES).whereEqualTo("organizerId", uid).get().await()
        organised.documents.forEach { it.reference.delete().await() }
        val organisedIds = organised.documents.map { it.id }.toSet()

        // Leave every other activity so their name and photo are no longer shown.
        dao.getRemoteActivityIds().filter { it !in organisedIds }.forEach { remoteId ->
            val local = dao.getActivityByRemoteId(remoteId)
            if (local?.isJoined == true) {
                db.collection(ACTIVITIES).document(remoteId)
                    .update(FieldPath.of("participants", uid), FieldValue.delete())
                    .await()
            }
        }

        val userDoc = db.collection(USERS).document(uid)
        userDoc.collection("interests").get().await().documents.forEach { it.reference.delete().await() }
        userDoc.delete().await()

        user.delete().await()
    }

    /** Pushes the signed-in user's editable profile fields to Firestore. */
    fun updateProfile(user: UserEntity) {
        val uid = currentUid ?: return
        if (user.remoteId != uid) return
        db.collection(USERS).document(uid)
            .set(
                hashMapOf(
                    "name" to user.name.take(80),
                    "bio" to user.bio.take(500),
                    "profilePictureUrl" to user.profilePictureUrl,
                    "city" to user.city
                ),
                SetOptions.merge()
            )
            .addOnFailureListener { Log.e(TAG, "Profile update failed", it) }
    }

    /** Refreshes a cached user's profile (bio, city, interests) from Firestore. */
    suspend fun refreshProfile(localUserId: Int) {
        val cached = dao.getUserByIdOnce(localUserId) ?: return
        val uid = cached.remoteId ?: return
        try {
            val doc = db.collection(USERS).document(uid).get().await()
            if (!doc.exists()) return
            dao.updateUser(
                cached.copy(
                    name = doc.getString("name") ?: cached.name,
                    bio = doc.getString("bio") ?: cached.bio,
                    profilePictureUrl = doc.getString("profilePictureUrl") ?: cached.profilePictureUrl,
                    city = doc.getString("city") ?: cached.city,
                    interests = doc.stringList("interests").joinToString(", ").ifBlank { cached.interests }
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not refresh profile for $uid", e)
        }
    }

    // --- Activities ---

    /**
     * Mirrors the most recent activities from Firestore into Room until cancelled. Each snapshot
     * is reconciled in full, so intermediate snapshots can safely be skipped.
     */
    suspend fun syncActivities() {
        snapshots(
            db.collection(ACTIVITIES)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(ACTIVITY_SYNC_LIMIT)
        ).collect { snapshot ->
            val remoteIds = mutableSetOf<String>()
            for (doc in snapshot.documents) {
                try {
                    upsertActivity(doc)
                    remoteIds += doc.id
                } catch (e: Exception) {
                    Log.e(TAG, "Could not map activity ${doc.id}", e)
                }
            }
            dao.getRemoteActivityIds()
                .filter { it !in remoteIds }
                .forEach { dao.deleteActivityByRemoteId(it) }
        }
    }

    private suspend fun upsertActivity(doc: DocumentSnapshot) {
        val organizerUid = doc.getString("organizerId") ?: return
        val participants = doc.participants()
        val existing = dao.getActivityByRemoteId(doc.id)
        val uid = currentUid

        val entity = ActivityEntity(
            id = existing?.id ?: 0,
            remoteId = doc.id,
            title = doc.getString("title").orEmpty(),
            description = doc.getString("description").orEmpty(),
            category = doc.getString("category").orEmpty(),
            subCategory = doc.getString("subCategory").orEmpty(),
            city = doc.getString("city").orEmpty(),
            location = doc.getString("location").orEmpty(),
            date = doc.getString("date").orEmpty(),
            time = doc.getString("time").orEmpty(),
            maxParticipants = doc.getLong("maxParticipants")?.toInt() ?: 10,
            cost = doc.getString("cost") ?: "Free",
            meetingPoint = doc.getString("meetingPoint").orEmpty(),
            visibility = doc.getString("visibility") ?: "Public",
            coverImageUrl = doc.getString("coverImageUrl").orEmpty(),
            organizerId = localUserIdFor(organizerUid, doc.getString("organizerName"), doc.getString("organizerAvatar")),
            organizerName = doc.getString("organizerName").orEmpty(),
            organizerAvatar = doc.getString("organizerAvatar").orEmpty(),
            organizerBio = doc.getString("organizerBio").orEmpty(),
            latitude = doc.getDouble("latitude") ?: 27.7172,
            longitude = doc.getDouble("longitude") ?: 85.3240,
            // A pending local write has no server timestamp yet.
            createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: existing?.createdAt ?: System.currentTimeMillis(),
            participantCount = participants.size.coerceAtLeast(1),
            isSaved = existing?.isSaved ?: false,
            isJoined = uid != null && uid in participants
        )

        val localId = if (existing == null) {
            dao.insertActivity(entity).toInt()
        } else {
            dao.updateActivity(entity)
            existing.id
        }

        dao.deleteParticipantsForActivity(localId)
        dao.insertParticipants(
            participants.map { (participantUid, info) ->
                ParticipantEntity(
                    activityId = localId,
                    userId = localUserIdFor(participantUid, info.name, info.avatar),
                    userName = info.name.orEmpty(),
                    userAvatar = info.avatar.orEmpty()
                )
            }
        )
    }

    fun createActivity(activity: ActivityEntity, organizer: UserEntity) {
        val uid = currentUid ?: return
        val data = hashMapOf(
            "title" to activity.title.take(120),
            "description" to activity.description.take(2000),
            "category" to activity.category,
            "subCategory" to activity.subCategory,
            "city" to activity.city,
            "location" to activity.location,
            "date" to activity.date,
            "time" to activity.time,
            "maxParticipants" to activity.maxParticipants,
            "cost" to activity.cost,
            "meetingPoint" to activity.meetingPoint,
            "visibility" to activity.visibility,
            "coverImageUrl" to activity.coverImageUrl,
            "organizerId" to uid,
            "organizerName" to organizer.name,
            "organizerAvatar" to organizer.profilePictureUrl,
            "organizerBio" to organizer.bio,
            "latitude" to activity.latitude,
            "longitude" to activity.longitude,
            "createdAt" to FieldValue.serverTimestamp(),
            "participants" to mapOf(uid to participantInfo(organizer.name, organizer.profilePictureUrl))
        )
        db.collection(ACTIVITIES).document().set(data)
            .addOnFailureListener { Log.e(TAG, "Creating activity failed", it) }
    }

    fun joinActivity(remoteId: String, localActivityId: Int, user: UserEntity) {
        val uid = currentUid ?: return
        db.collection(ACTIVITIES).document(remoteId)
            .update(FieldPath.of("participants", uid), participantInfo(user.name, user.profilePictureUrl))
            .addOnFailureListener { Log.e(TAG, "Joining activity failed", it) }
        // Firestore commits writes in order, so the participant entry exists before the message.
        sendMessage(remoteId, localActivityId, user, "👋 Joined the activity! Let's do this together!")
    }

    fun leaveActivity(remoteId: String, localActivityId: Int, user: UserEntity) {
        val uid = currentUid ?: return
        // Post while still a participant; the rules only let participants write to the chat.
        sendMessage(remoteId, localActivityId, user, "Left the activity.")
        db.collection(ACTIVITIES).document(remoteId)
            .update(FieldPath.of("participants", uid), FieldValue.delete())
            .addOnFailureListener { Log.e(TAG, "Leaving activity failed", it) }
    }

    // --- Group chat ---

    /** Mirrors an activity's latest chat messages into Room until cancelled. */
    suspend fun syncMessages(remoteId: String, localActivityId: Int) {
        snapshots(
            db.collection(ACTIVITIES).document(remoteId).collection(MESSAGES)
                .orderBy("timestamp")
                .limitToLast(MESSAGE_SYNC_LIMIT)
        ).collect { snapshot ->
            for (doc in snapshot.documents) {
                try {
                    val senderUid = doc.getString("senderId") ?: continue
                    val senderName = doc.getString("senderName").orEmpty()
                    val senderAvatar = doc.getString("senderAvatar").orEmpty()
                    dao.insertMessageIfAbsent(
                        MessageEntity(
                            remoteId = doc.id,
                            activityId = localActivityId,
                            senderId = localUserIdFor(senderUid, senderName, senderAvatar),
                            senderName = senderName,
                            senderAvatar = senderAvatar,
                            text = doc.getString("text").orEmpty(),
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Could not map message ${doc.id}", e)
                }
            }
        }
    }

    fun sendMessage(remoteId: String, localActivityId: Int, sender: UserEntity, text: String) {
        val uid = currentUid ?: return
        val ref = db.collection(ACTIVITIES).document(remoteId).collection(MESSAGES).document()
        val timestamp = System.currentTimeMillis()
        ref.set(
            hashMapOf(
                "senderId" to uid,
                "senderName" to sender.name,
                "senderAvatar" to sender.profilePictureUrl,
                "text" to text.take(2000),
                "timestamp" to timestamp,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).addOnFailureListener { Log.e(TAG, "Sending message failed", it) }
    }

    // --- Moderation ---

    suspend fun reportUser(reportedLocalUserId: Int, reason: String) {
        val uid = currentUid ?: return
        val reportedUid = dao.getUserByIdOnce(reportedLocalUserId)?.remoteId ?: return
        db.collection(REPORTS).document()
            .set(
                hashMapOf(
                    "reporterId" to uid,
                    "reportedUserId" to reportedUid,
                    "reason" to reason.take(2000),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnFailureListener { Log.e(TAG, "Sending report failed", it) }
    }

    // --- Helpers ---

    private suspend fun saveCurrentUser(
        uid: String,
        email: String,
        name: String,
        bio: String,
        profilePictureUrl: String,
        city: String,
        interests: List<String>
    ): UserEntity {
        dao.clearCurrentUser()
        val existing = dao.getUserByRemoteId(uid)
        val user = UserEntity(
            id = existing?.id ?: 0,
            name = name,
            bio = bio,
            profilePictureUrl = profilePictureUrl,
            city = city,
            interests = interests.joinToString(", "),
            followersCount = existing?.followersCount ?: 0,
            followingCount = existing?.followingCount ?: 0,
            isCurrentUser = true,
            email = email,
            remoteId = uid
        )
        if (existing == null) dao.insertUser(user) else dao.updateUser(user)
        return user
    }

    /** Maps a Firebase uid to a local Room user id, caching a minimal profile if needed. */
    private suspend fun localUserIdFor(uid: String, name: String?, avatar: String?): Int {
        dao.getUserByRemoteId(uid)?.let { return it.id }
        dao.insertUserIfAbsent(
            UserEntity(
                name = name?.takeIf { it.isNotBlank() } ?: "Connect Nepal member",
                bio = "",
                profilePictureUrl = avatar.orEmpty(),
                city = "",
                interests = "",
                followersCount = 0,
                followingCount = 0,
                remoteId = uid
            )
        )
        return dao.getUserByRemoteId(uid)?.id ?: error("Could not cache user $uid")
    }

    private fun snapshots(query: Query): Flow<QuerySnapshot> = callbackFlow {
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Firestore listener error", error)
                return@addSnapshotListener
            }
            if (snapshot != null) trySend(snapshot)
        }
        awaitClose { registration.remove() }
    }.conflate()

    private data class ParticipantInfo(val name: String?, val avatar: String?)

    private fun DocumentSnapshot.participants(): Map<String, ParticipantInfo> {
        val raw = get("participants") as? Map<*, *> ?: return emptyMap()
        return raw.entries.mapNotNull { (key, value) ->
            val uid = key as? String ?: return@mapNotNull null
            val info = value as? Map<*, *>
            uid to ParticipantInfo(info?.get("name") as? String, info?.get("avatar") as? String)
        }.toMap()
    }

    private fun DocumentSnapshot.stringList(field: String): List<String> =
        (get(field) as? List<*>)?.filterIsInstance<String>().orEmpty()

    private fun participantInfo(name: String, avatar: String) = mapOf("name" to name, "avatar" to avatar)

    companion object {
        private const val TAG = "CloudRepository"
        private const val USERS = "users"
        private const val ACTIVITIES = "activities"
        private const val MESSAGES = "messages"
        private const val REPORTS = "reports"
        private const val ACTIVITY_SYNC_LIMIT = 300L
        private const val MESSAGE_SYNC_LIMIT = 300L

        /** Returns null when the app has no Firebase configuration (google-services.json). */
        fun createIfConfigured(context: Context, dao: ConnectDao): CloudRepository? =
            if (FirebaseApp.getApps(context).isEmpty()) null else CloudRepository(dao)
    }
}
