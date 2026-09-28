package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.database.ConnectDao
import com.example.data.model.ActivityEntity
import com.example.data.model.MessageEntity
import com.example.data.model.UserEntity
import com.example.data.repository.ConnectRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Exercises the offline (no Firebase) data paths and the Room queries the cloud sync relies on. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ConnectRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ConnectDao
    private lateinit var repository: ConnectRepository

    private val organizer = user(id = 1, name = "Organizer")
    private val joiner = user(id = 2, name = "Joiner")

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.connectDao()
        repository = ConnectRepository(dao, cloud = null)
        runBlocking {
            dao.insertUser(organizer)
            dao.insertUser(joiner)
        }
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun createdActivityListsOrganizerAsParticipant() = runBlocking {
        repository.createActivity(activity(), organizer)

        val created = dao.getAllActivities().first().single()
        val participants = dao.getParticipantsByActivity(created.id).first()
        assertEquals(listOf(organizer.id), participants.map { it.userId })
        assertEquals(organizer.profilePictureUrl, participants.single().userAvatar)
    }

    @Test
    fun joinAndLeaveUpdateParticipantsAndChat() = runBlocking {
        val activityId = dao.insertActivity(activity()).toInt()

        repository.joinActivity(activityId, joiner)
        var joined = dao.getActivityByIdOnce(activityId)!!
        assertTrue(joined.isJoined)
        assertEquals(2, joined.participantCount)
        assertEquals(listOf(activityId), dao.getActivitiesJoinedByUser(joiner.id).first().map { it.id })

        repository.leaveActivity(activityId, joiner)
        joined = dao.getActivityByIdOnce(activityId)!!
        assertFalse(joined.isJoined)
        assertEquals(1, joined.participantCount)
        assertTrue(dao.getActivitiesJoinedByUser(joiner.id).first().isEmpty())

        val messages = dao.getMessagesByActivity(activityId).first()
        assertEquals(2, messages.size)
    }

    @Test
    fun localMessagesGetUniqueIds() = runBlocking {
        val activityId = dao.insertActivity(activity()).toInt()

        repeat(3) { repository.sendMessage(activityId, joiner, "Hello $it") }

        val messages = repository.getMessagesByActivity(activityId).first()
        assertEquals(setOf("Hello 0", "Hello 1", "Hello 2"), messages.map { it.text }.toSet())
        assertEquals(3, messages.map { it.id }.toSet().size)
    }

    @Test
    fun remoteIdsAreUniqueAndQueryable() = runBlocking {
        dao.insertActivity(activity().copy(remoteId = "abc"))
        dao.insertActivity(activity().copy(title = "Local only"))

        assertEquals(listOf("abc"), dao.getRemoteActivityIds())
        assertEquals("Futsal", dao.getActivityByRemoteId("abc")?.title)

        dao.deleteActivityByRemoteId("abc")
        assertNull(dao.getActivityByRemoteId("abc"))
        assertEquals(listOf("Local only"), dao.getAllActivities().first().map { it.title })
    }

    @Test
    fun syncedMessagesAreNotDuplicated() = runBlocking {
        val activityId = dao.insertActivity(activity()).toInt()
        val message = MessageEntity(
            activityId = activityId,
            senderId = joiner.id,
            senderName = joiner.name,
            senderAvatar = "",
            text = "Hi",
            remoteId = "msg-1"
        )

        dao.insertMessageIfAbsent(message)
        dao.insertMessageIfAbsent(message)

        assertEquals(1, dao.getMessagesByActivity(activityId).first().size)
    }

    @Test
    fun cachedUsersAreFoundByRemoteId() = runBlocking {
        dao.insertUserIfAbsent(user(id = 0, name = "Cloud user").copy(remoteId = "uid-1"))
        dao.insertUserIfAbsent(user(id = 0, name = "Duplicate").copy(remoteId = "uid-1"))

        assertEquals("Cloud user", dao.getUserByRemoteId("uid-1")?.name)
    }

    private fun user(id: Int, name: String) = UserEntity(
        id = id,
        name = name,
        bio = "",
        profilePictureUrl = "https://example.com/$name.png",
        city = "Kathmandu",
        interests = "",
        followersCount = 0,
        followingCount = 0
    )

    private fun activity() = ActivityEntity(
        title = "Futsal",
        description = "Evening game",
        category = "Sports",
        subCategory = "Futsal",
        city = "Kathmandu",
        location = "Baneshwor",
        date = "Saturday",
        time = "17:00",
        maxParticipants = 10,
        cost = "Free",
        meetingPoint = "Main gate",
        visibility = "Public",
        coverImageUrl = "",
        organizerId = organizer.id,
        organizerName = organizer.name,
        organizerAvatar = organizer.profilePictureUrl
    )
}
