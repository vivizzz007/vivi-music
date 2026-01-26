package com.music.vivi.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.music.vivi.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SettingsTest {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1) val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunchesSuccessfully() {
        // Basic verification that the app launches and shows some content
        // Adjust "Home" to whatever is initially displayed
        // composeTestRule.onNodeWithText("Home").assertIsDisplayed()
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
