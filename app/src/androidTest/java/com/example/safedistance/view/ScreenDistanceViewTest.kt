package com.example.safedistance.view

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.example.safedistance.utils.ServiceCommands
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class ScreenDistanceViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testCameraLaunch() {
        // Set Composable content
        composeTestRule.setContent {
            val screenDistanceView = ScreenDistanceView(ServiceCommands(LocalContext.current))
            screenDistanceView.View()
        }

        composeTestRule.onNodeWithText("Please look at the front camera")
            .assertExists()
    }
}