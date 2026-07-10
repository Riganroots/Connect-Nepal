package com.example

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.KathmanduMapTab
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ConnectViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class KathmanduMapScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun map_tab_screenshot() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = ConnectViewModel(application)

        composeTestRule.setContent {
            MyApplicationTheme {
                KathmanduMapTab(
                    viewModel = viewModel,
                    onNavigateToDetails = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/kathmandu_map_tab.png")
    }
}
