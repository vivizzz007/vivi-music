/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.my.kizzy.rpc

/**
 * Created by Zion Huang
 * Modified by vivimusic contributors
 */
data class UserInfo(
    val id: String,
    val username: String,
    val name: String,
    val avatar: String?,
)
