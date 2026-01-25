package com.music.vivi.viewmodels

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import app.cash.turbine.test
import com.music.vivi.constants.LastFMSessionKey
import com.music.vivi.constants.LastFMUsernameKey
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IntegrationsViewModelTest {

    private lateinit var viewModel: IntegrationsViewModel
    private val dataStore: DataStore<Preferences> = mockk(relaxed = true)
    private val preferencesFlow = MutableStateFlow(preferencesOf())
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { dataStore.data } returns preferencesFlow

        viewModel = IntegrationsViewModel(dataStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `lastFmState updates when DataStore changes`() = runTest {
        viewModel.lastFmState.test {
            // Initial State (empty)
            val initialState = awaitItem()
            assertEquals(LastFMState(), initialState)

            // Simulate DataStore update
            preferencesFlow.value = preferencesOf(
                LastFMSessionKey to "test_session",
                LastFMUsernameKey to "test_user"
            )

            // Verify State update
            val updatedState = awaitItem()
            assertEquals(
                LastFMState(
                    sessionKey = "test_session",
                    username = "test_user",
                    isLoggedIn = true
                ),
                updatedState
            )
        }
    }
}
