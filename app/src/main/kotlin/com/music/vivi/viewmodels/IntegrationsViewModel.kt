package com.music.vivi.viewmodels

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.vivi.constants.DiscordTokenKey
import com.music.vivi.constants.LastFMSessionKey
import com.music.vivi.constants.LastFMUsernameKey
import com.music.vivi.utils.get
import com.my.kizzy.rpc.KizzyRPC
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

public data class DiscordState(
    val token: String = "",
    val username: String = "",
    val name: String = "",
    val isLoggedIn: Boolean = false,
)

public data class LastFMState(val sessionKey: String = "", val username: String = "", val isLoggedIn: Boolean = false)

/**
 * ViewModel for managing 3rd party integrations (Discord RPC, Last.fm).
 * Observes datastore keys for tokens/sessions and updates connection state.
 */
@HiltViewModel
public class IntegrationsViewModel @Inject constructor(private val dataStore: DataStore<Preferences>) : ViewModel() {

    private val _discordState = MutableStateFlow(DiscordState())
    public val discordState: StateFlow<DiscordState> = _discordState.asStateFlow()

    private val _lastFmState = MutableStateFlow(LastFMState())
    public val lastFmState: StateFlow<LastFMState> = _lastFmState.asStateFlow()

    init {
        // Observe Discord
        viewModelScope.launch {
            dataStore.data
                .map { it[DiscordTokenKey] ?: "" }
                .distinctUntilChanged()
                .collect { token ->
                    if (token.isNotEmpty()) {
                        // Fetch user info if token exists
                        KizzyRPC.getUserInfo(token).onSuccess { info ->
                            _discordState.value = DiscordState(
                                token = token,
                                username = info.username,
                                name = info.name,
                                isLoggedIn = true
                            )
                        }.onFailure {
                            // Even if fetch fails, if we have a token we are "logged in" technically until it fails?
                            // But better to assume valid token implies logged in for UI purposes initially
                            _discordState.value = DiscordState(token = token, isLoggedIn = true)
                        }
                    } else {
                        _discordState.value = DiscordState(isLoggedIn = false)
                    }
                }
        }

        // Observe LastFM
        viewModelScope.launch {
            dataStore.data
                .map { (it[LastFMSessionKey] ?: "") to (it[LastFMUsernameKey] ?: "") }
                .distinctUntilChanged()
                .collect { (session, username) ->
                    _lastFmState.value = LastFMState(
                        sessionKey = session,
                        username = username,
                        isLoggedIn = session.isNotEmpty()
                    )
                }
        }
    }
}
