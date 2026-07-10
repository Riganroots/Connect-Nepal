package com.example.data.repository

import android.util.Log
import com.example.data.database.ConnectDao
import com.example.data.model.UserInterest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/**
 * Repository class to sync User Interests with Firebase Firestore and cache them locally in Room.
 * Ensures interest tags can be seamlessly attached to user profiles.
 */
class InterestsRepository(private val connectDao: ConnectDao) {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("InterestsRepository", "Failed to initialize Firestore: ${e.message}")
            null
        }
    }

    /**
     * Gets user interests as a local reactive flow.
     */
    fun getLocalUserInterests(userId: String): Flow<List<UserInterest>> {
        return connectDao.getUserInterests(userId)
    }

    /**
     * Listens to user interests from Firestore in real-time and caches them locally.
     * Uses a fallback flow if Firebase is not initialized.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun syncAndGetInterests(userId: String): Flow<List<UserInterest>> {
        val localFlow = connectDao.getUserInterests(userId)
        val db = firestore ?: return localFlow

        return callbackFlow<Unit> {
            // Listen to subcollection users/{userId}/interests
            val listenerRegistration = db.collection("users")
                .document(userId)
                .collection("interests")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("InterestsRepository", "Firestore interests listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        launch {
                            // Clear local database interests for this user to sync cleanly
                            connectDao.deleteUserInterests(userId)
                            
                            val interestsList = mutableListOf<UserInterest>()
                            for (doc in snapshot.documents) {
                                try {
                                    val id = doc.id
                                    val interestName = doc.getString("interestName") ?: ""
                                    val category = doc.getString("category") ?: ""
                                    val interest = UserInterest(
                                        id = id,
                                        userId = userId,
                                        interestName = interestName,
                                        category = category
                                    )
                                    interestsList.add(interest)
                                } catch (e: Exception) {
                                    Log.e("InterestsRepository", "Mapping Firestore interest error: ${e.message}")
                                }
                            }
                            if (interestsList.isNotEmpty()) {
                                connectDao.insertUserInterests(interestsList)
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

    /**
     * Attaches an interest tag to a user's profile both locally and in Firestore.
     */
    suspend fun attachInterestToProfile(userId: String, interestName: String, category: String = "General") {
        val uniqueId = "${userId}_${interestName.replace(" ", "_").lowercase()}"
        val userInterest = UserInterest(
            id = uniqueId,
            userId = userId,
            interestName = interestName,
            category = category
        )

        // Save locally first
        connectDao.insertUserInterest(userInterest)

        // Sync to Firebase Firestore
        val db = firestore
        if (db != null) {
            try {
                val data = hashMapOf(
                    "id" to uniqueId,
                    "userId" to userId,
                    "interestName" to interestName,
                    "category" to category,
                    "timestamp" to System.currentTimeMillis()
                )

                // 1. Store in the user's specific subcollection
                db.collection("users")
                    .document(userId)
                    .collection("interests")
                    .document(uniqueId)
                    .set(data)

                // 2. Also keep a denormalized interests array on the parent user profile document for quick queries
                db.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener { doc ->
                        val currentInterests = doc.get("interests") as? List<*> ?: emptyList<Any>()
                        if (!currentInterests.contains(interestName)) {
                            val updatedInterests = currentInterests.map { it.toString() } + interestName
                            db.collection("users")
                                .document(userId)
                                .update("interests", updatedInterests)
                        }
                    }
                    .addOnFailureListener {
                        // Document might not exist yet, let's set it
                        db.collection("users")
                            .document(userId)
                            .set(hashMapOf("interests" to listOf(interestName)), com.google.firebase.firestore.SetOptions.merge())
                    }

                Log.d("InterestsRepository", "Successfully synced interest: $interestName to profile.")
            } catch (e: Exception) {
                Log.e("InterestsRepository", "Error syncing interest to Firestore: ${e.message}")
            }
        }
    }

    /**
     * Detaches an interest tag from a user's profile both locally and in Firestore.
     */
    suspend fun detachInterestFromProfile(userId: String, interestName: String) {
        val uniqueId = "${userId}_${interestName.replace(" ", "_").lowercase()}"

        // Delete locally first
        connectDao.deleteUserInterest(userId, interestName)

        // Sync to Firebase Firestore
        val db = firestore
        if (db != null) {
            try {
                // 1. Remove from subcollection
                db.collection("users")
                    .document(userId)
                    .collection("interests")
                    .document(uniqueId)
                    .delete()

                // 2. Remove from parent user profile document's denormalized interests array
                db.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener { doc ->
                        val currentInterests = doc.get("interests") as? List<*> ?: emptyList<Any>()
                        if (currentInterests.contains(interestName)) {
                            val updatedInterests = currentInterests.map { it.toString() }.filter { it != interestName }
                            db.collection("users")
                                .document(userId)
                                .update("interests", updatedInterests)
                        }
                    }

                Log.d("InterestsRepository", "Successfully removed interest: $interestName from profile.")
            } catch (e: Exception) {
                Log.e("InterestsRepository", "Error deleting interest from Firestore: ${e.message}")
            }
        }
    }

    /**
     * Syncs a list of interests to Firestore at once (useful during profile creation or offline synchronization).
     */
    suspend fun syncAllLocalInterestsToFirestore(userId: String, interests: List<UserInterest>) {
        val db = firestore ?: return
        try {
            // Write to subcollection in batch or individual tasks
            interests.forEach { interest ->
                val uniqueId = interest.id.ifEmpty { "${userId}_${interest.interestName.replace(" ", "_").lowercase()}" }
                val data = hashMapOf(
                    "id" to uniqueId,
                    "userId" to userId,
                    "interestName" to interest.interestName,
                    "category" to interest.category
                )
                db.collection("users")
                    .document(userId)
                    .collection("interests")
                    .document(uniqueId)
                    .set(data)
            }

            // Also update parent profile's interests field
            val interestNames = interests.map { it.interestName }
            db.collection("users")
                .document(userId)
                .set(hashMapOf("interests" to interestNames), com.google.firebase.firestore.SetOptions.merge())

            Log.d("InterestsRepository", "All local interests successfully batch synced to Firestore.")
        } catch (e: Exception) {
            Log.e("InterestsRepository", "Failed to batch sync interests: ${e.message}")
        }
    }
}
