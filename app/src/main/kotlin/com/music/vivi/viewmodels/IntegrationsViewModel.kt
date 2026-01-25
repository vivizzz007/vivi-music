package com.music.vivi.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.lastfm.LastFM
import com.music.vivi.constants.DiscordTokenKey
import com.music.vivi.constants.LastFMSessionKey
import com.music.vivi.constants.LastFMUsernameKey
import com.music.vivi.utils.dataStore
import com.my.kizzy.rpc.KizzyRPC
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

public data class DiscordState(
    val token: String = "",
    val username: String = "",
    val name: String = "",
    val isLoggedIn: Boolean = false
)

public data class LastFMState(
    val sessionKey: String = "",
    val username: String = "",
    val isLoggedIn: Boolean = false
)

@HiltViewModel
public class IntegrationsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _discordState = MutableStateFlow(DiscordState())
    public val discordState: StateFlow<DiscordState> = _discordState.asStateFlow()

    private val _lastFmState = MutableStateFlow(LastFMState())
    public val lastFmState: StateFlow<LastFMState> = _lastFmState.asStateFlow()

    init {
        // Observe Discord
        viewModelScope.launch {
            context.dataStore.data
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
            context.dataStore.data
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
