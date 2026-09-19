package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.ConnectViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocalGuideAndChatTest {

    private lateinit var application: Application
    private lateinit var viewModel: ConnectViewModel

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        viewModel = ConnectViewModel(application)
    }

    @Test
    fun testBlockUserAndUnblockUser() = runBlocking {
        // Initially empty
        var blocked = viewModel.blockedUserIds.value
        assertTrue(blocked.isEmpty())

        // Block a user
        viewModel.blockUser(10)
        blocked = viewModel.blockedUserIds.value
        assertEquals(1, blocked.size)
        assertTrue(blocked.contains(10))

        // Unblock a user
        viewModel.unblockUser(10)
        blocked = viewModel.blockedUserIds.value
        assertTrue(blocked.isEmpty())
    }

    @Test
    fun testReportUser() = runBlocking {
        // Initially empty
        var reports = viewModel.reportedUsers.value
        assertTrue(reports.isEmpty())

        // Report a user
        viewModel.reportUser(12, "Spam: sending duplicate meetups")
        reports = viewModel.reportedUsers.value
        assertEquals(1, reports.size)
        assertEquals("Spam: sending duplicate meetups", reports[12])
    }

    @Test
    fun testAiGuideInitialAndClear() = runBlocking {
        // Verify initial message is present
        var messages = viewModel.aiMessages.value
        assertEquals(1, messages.size)
        assertFalse(messages[0].second) // User flag should be false

        // Clear messages
        viewModel.clearAiMessages()
        messages = viewModel.aiMessages.value
        assertEquals(1, messages.size)
    }
}
