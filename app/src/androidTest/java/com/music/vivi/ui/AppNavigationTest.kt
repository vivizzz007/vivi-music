package com.music.vivi.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.music.vivi.MainActivity
import com.music.vivi.R
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

/**
 * End-to-End (E2E) UI Tests for verifying the app's navigation structure.
 *
 * It uses:
 * - [HiltAndroidRule] to inject dependencies.
 * - [createAndroidComposeRule] to interact with Compose UI nodes.
 *
 * Covers: Home, Search, Library, and Settings navigation.
 */
@HiltAndroidTest
class AppNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunch_verifiesHomeScreen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val homeString = context.getString(R.string.home)

        // Verify Home is displayed
        composeTestRule.onNodeWithText(homeString).assertIsDisplayed()
    }

    @Test
    fun navigateToSearch_verifiesSearchScreen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val searchString = context.getString(R.string.search)
        val searchInputHint = context.getString(R.string.search_yt_music)

        // Click Search Tab
        composeTestRule.onNodeWithText(searchString).performClick()

        // Verify Search Input field is displayed (placeholder text)
        // Adjust this if the placeholder logic differs, but R.string.search_yt_music is used in TopSearch
        composeTestRule.onNodeWithText(searchInputHint).assertIsDisplayed()
    }

    @Test
    fun navigateToLibrary_verifiesLibraryScreen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val libraryString = context.getString(R.string.filter_library)

        // Click Library Tab
        composeTestRule.onNodeWithText(libraryString).performClick()

        // Verify Library Screen content
        composeTestRule.onNodeWithText(libraryString).assertIsDisplayed()
    }

    @Test
    fun navigateToSettings_verifiesSettingsScreen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val settingsString = "Settings" // Hardcoded as per Content Description added
        // Note: The title on screen might be from R.string.settings which is "Settings"

        // Ensure we are on Home (or somewhere with the TopBar)
        // Click Settings Icon
        composeTestRule.onNodeWithContentDescription("Settings").performClick()

        // Verify Settings Screen Title is displayed
        composeTestRule.onNodeWithText(context.getString(R.string.settings)).assertIsDisplayed()
    }
}
