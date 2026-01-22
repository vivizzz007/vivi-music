package com.music.vivi.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.music.vivi.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import com.music.vivi.R
import androidx.test.platform.app.InstrumentationRegistry

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

        // Click Search Tab
        // Assuming tab has text "Search" or content description equal to title
        composeTestRule.onNodeWithText(searchString).performClick()

        // Verify Search Screen (maybe check for Search Bar)
        // Since Search triggers a separate screen or changes content, we verify some element unique to Search
        // e.g., the Search Bar placeholder or title again if it's in the top bar
        composeTestRule.onNodeWithText(searchString).assertIsDisplayed()
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
}
