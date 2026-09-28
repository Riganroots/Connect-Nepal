package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.BuildConfig
import com.example.data.model.*
import com.example.data.security.PasswordHasher
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        ActivityEntity::class,
        ParticipantEntity::class,
        MessageEntity::class,
        UserInterest::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun connectDao(): ConnectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "connect_database"
                )
                .fallbackToDestructiveMigration()
                // Sample people and activities are only for offline/demo builds. With Firebase
                // configured, every activity comes from Firestore so testers never join fake ones.
                .addCallback(ConnectDatabaseCallback(scope, seedSampleData = FirebaseApp.getApps(context).isEmpty()))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class ConnectDatabaseCallback(
        private val scope: CoroutineScope,
        private val seedSampleData: Boolean
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            if (!seedSampleData) return
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val dao = database.connectDao()

                    // Sample accounts can only be logged into on debug builds; release builds
                    // store a blank password hash, which PasswordHasher.verify always rejects.
                    val seedPasswordHash = if (BuildConfig.DEBUG) PasswordHasher.hash(MockData.DEMO_PASSWORD) else ""

                    // Insert current user
                    dao.insertUser(MockData.currentUser.copy(password = seedPasswordHash))

                    // Insert initial users
                    MockData.initialUsers.forEach { user ->
                        dao.insertUser(user.copy(password = seedPasswordHash))
                    }

                    // Insert initial activities
                    MockData.initialActivities.forEach { activity ->
                        dao.insertActivity(activity)
                    }

                    // Insert initial participants
                    MockData.initialParticipants.forEach { participant ->
                        dao.insertParticipant(participant)
                    }

                    // Insert initial messages
                    MockData.initialMessages.forEach { message ->
                        dao.insertMessage(message)
                    }
                }
            }
        }
    }
}
