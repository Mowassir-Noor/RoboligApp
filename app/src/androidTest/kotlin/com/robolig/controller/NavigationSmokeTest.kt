package com.robolig.controller

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.robolig.controller.presentation.MainActivity
import org.junit.Rule
import org.junit.Test

class NavigationSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchesDriveScreen() {
        composeRule.onNodeWithText("Driving Mode").assertIsDisplayed()
    }

    @Test
    fun navigatesToSettingsAndBackToDrive() {
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithText("Driving Mode").assertIsDisplayed()
    }

    @Test
    fun navigatesToGripperMode() {
        composeRule.onNodeWithText("Gripper").performClick()
        composeRule.onNodeWithText("Gripper Mode").assertIsDisplayed()
        composeRule.onNodeWithText("DRIVE").assertIsDisplayed()
    }

    @Test
    fun navigatesToZiplineModeShowsHeightControl() {
        composeRule.onNodeWithText("Zipline").performClick()
        composeRule.onNodeWithText("Zipline Mode").assertIsDisplayed()
        composeRule.onNodeWithText("Height").assertIsDisplayed()
    }
}
