package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
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
    version = 5,
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
                .addCallback(ConnectDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class ConnectDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val dao = database.connectDao()
                    
                    // Insert current user
                    dao.insertUser(MockData.currentUser)
                    
                    // Insert initial users
                    MockData.initialUsers.forEach { user ->
                        dao.insertUser(user)
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
