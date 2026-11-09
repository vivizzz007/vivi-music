/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.my.kizzy.gateway.entities.presence

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Timestamps(
    @SerialName("start")
    val start: Long? = null,
    @SerialName("end")
    val end: Long? = null,
)