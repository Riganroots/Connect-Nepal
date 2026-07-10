package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.database.ConnectDao
import com.example.data.model.UserInterest
import com.example.data.repository.InterestsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UserInterestsTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ConnectDao
    private lateinit var repository: InterestsRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.connectDao()
        repository = InterestsRepository(dao)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testUserInterestModelAndStorage() = runBlocking {
        val interest = UserInterest(
            id = "user1_hiking",
            userId = "user1",
            interestName = "Hiking",
            category = "Outdoors"
        )

        dao.insertUserInterest(interest)

        val retrieved = dao.getUserInterests("user1").first()
        assertEquals(1, retrieved.size)
        assertEquals("Hiking", retrieved[0].interestName)
        assertEquals("Outdoors", retrieved[0].category)
    }

    @Test
    fun testRepositoryAttachAndDetach() = runBlocking {
        // Attach interest via Repository
        repository.attachInterestToProfile("user2", "Music", "Art")

        val interests = repository.getLocalUserInterests("user2").first()
        assertEquals(1, interests.size)
        assertEquals("Music", interests[0].interestName)
        assertEquals("Art", interests[0].category)

        // Detach interest via Repository
        repository.detachInterestFromProfile("user2", "Music")

        val interestsAfterDelete = repository.getLocalUserInterests("user2").first()
        assertTrue(interestsAfterDelete.isEmpty())
    }
}
