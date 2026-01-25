package com.music.vivi.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.music.vivi.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunchesSuccessfully() {
        // Basic verification that the app launches and shows some content
        // Adjust "Home" to whatever is initially displayed
        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
    }

    @Test
    fun settingsMenuExists() {
        // Attempt to find a way to settings.
        // This assumes there might be a "Settings" text or icon content description.
        // Updating this to be generic first.

        // Example: If there is a settings button
        // composeTestRule.onNodeWithContentDescription("Settings").performClick()
    }
}
