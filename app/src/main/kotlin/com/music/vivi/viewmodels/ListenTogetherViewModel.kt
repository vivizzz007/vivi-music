/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.viewmodels

import androidx.lifecycle.ViewModel
import com.music.vivi.listentogether.ListenTogetherManager
import androidx.lifecycle.viewModelScope
import com.music.vivi.listentogether.ListenTogetherEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListenTogetherViewModel @Inject constructor(
    private val manager: ListenTogetherManager
) : ViewModel() {

    val connectionState = manager.connectionState
    val roomState = manager.roomState
    val role = manager.role
    val userId = manager.userId
    val pendingJoinRequests = manager.pendingJoinRequests
    val bufferingUsers = manager.bufferingUsers
    val logs = manager.logs
    val events = manager.events
    val hasPersistedSession = manager.hasPersistedSession
    val blockedUsernames = manager.blockedUsernames

    val roomCodeInput = MutableStateFlow("")
    val usernameInput = MutableStateFlow("")
    val isCreatingRoom = MutableStateFlow(false)
    val isJoiningRoom = MutableStateFlow(false)
    val joinErrorMessage = MutableStateFlow<String?>(null)
    
    val selectedUserForMenu = MutableStateFlow<String?>(null)
    val selectedUsername = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            manager.events.collect { event ->
                when (event) {
                    is ListenTogetherEvent.JoinRejected -> {
                        val reason = event.reason
                        joinErrorMessage.value = when {
                            reason.isNullOrBlank() -> "Join request denied"
                            reason.contains("invalid", ignoreCase = true) -> "Invalid room code"
                            else -> "Join request denied: $reason"
                        }
                        isJoiningRoom.value = false
                        isCreatingRoom.value = false
                    }
                    is ListenTogetherEvent.JoinApproved -> {
                        isJoiningRoom.value = false
                        joinErrorMessage.value = null
                    }
                    is ListenTogetherEvent.RoomCreated -> {
                        isCreatingRoom.value = false
                    }
                    else -> {}
                }
            }
        }
    }

    fun connect() {
        manager.connect()
    }

    fun disconnect() {
        manager.disconnect()
    }

    fun createRoom(username: String) {
        manager.createRoom(username)
    }

    fun joinRoom(roomCode: String, username: String) {
        manager.joinRoom(roomCode, username)
    }

    fun leaveRoom() {
        manager.leaveRoom()
    }

    fun approveJoin(userId: String) {
        manager.approveJoin(userId)
    }

    fun rejectJoin(userId: String, reason: String? = null) {
        manager.rejectJoin(userId, reason)
    }

    fun kickUser(userId: String, reason: String? = null) {
        manager.kickUser(userId, reason)
    }

    fun blockUser(username: String) {
        manager.blockUser(username)
    }

    fun unblockUser(username: String) {
        manager.unblockUser(username)
    }

    fun clearLogs() {
        manager.clearLogs()
    }

    fun forceReconnect() {
        manager.forceReconnect()
    }
    
    fun reconnect() {
        manager.forceReconnect()
    }
    
    fun getPersistedRoomCode(): String? = manager.getPersistedRoomCode()
    
    fun getSessionAge(): Long = manager.getSessionAge()
}
