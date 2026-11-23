/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.my.kizzy.gateway

import com.my.kizzy.gateway.entities.Heartbeat
import com.my.kizzy.gateway.entities.Identify.Companion.toIdentifyPayload
import com.my.kizzy.gateway.entities.Payload
import com.my.kizzy.gateway.entities.Ready
import com.my.kizzy.gateway.entities.Resume
import com.my.kizzy.gateway.entities.op.OpCode
import com.my.kizzy.gateway.entities.op.OpCode.DISPATCH
import com.my.kizzy.gateway.entities.op.OpCode.HEARTBEAT
import com.my.kizzy.gateway.entities.op.OpCode.HELLO
import com.my.kizzy.gateway.entities.op.OpCode.IDENTIFY
import com.my.kizzy.gateway.entities.op.OpCode.INVALID_SESSION
import com.my.kizzy.gateway.entities.op.OpCode.PRESENCE_UPDATE
import com.my.kizzy.gateway.entities.op.OpCode.RECONNECT
import com.my.kizzy.gateway.entities.op.OpCode.RESUME
import com.my.kizzy.gateway.entities.presence.Presence
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import java.util.logging.Level
import java.util.logging.Level.INFO
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Modified by Zion Huang
 */
open class DiscordWebSocket(
    private val token: String,
    private val os: String = "Android",
    private val browser: String = "Discord Android",
    private val device: String = "Generic Android Device",
) : CoroutineScope {
    private val logger = Logger.getLogger(DiscordWebSocket::class.java.name)
    private val gatewayUrl = "wss://gateway.discord.gg/?v=9&encoding=json"
    private var websocket: DefaultClientWebSocketSession? = null
    private var sequence = 0
    private var sessionId: String? = null
    private var heartbeatInterval = 0L
    private var resumeGatewayUrl: String? = null
    private var heartbeatJob: Job? = null
    private var connected = false
    private var client: HttpClient = HttpClient {
        install(WebSockets)
    }
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var reconnectionJob: Job? = null
    private var currentReconnectDelay = INITIAL_RECONNECT_DELAY

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        logger.log(Level.WARNING, "DiscordWebSocket unhandled error: ${throwable.message}", throwable)
    }
    private var scopeJob = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = scopeJob + Dispatchers.IO + coroutineExceptionHandler

    fun connect() {
        if (connected) {
            logger.info("Gateway already connected.")
            return
        }
        if (!scopeJob.isActive) {
            scopeJob = SupervisorJob()
        }
        reconnectionJob?.cancel()
        reconnectionJob = launch {
            try {
                val url = resumeGatewayUrl ?: gatewayUrl
                logger.info("Connecting to Discord Gateway at $url")
                websocket = client.webSocketSession(url) {
                    header("User-Agent", "Discord-Android/314013;RNA")
                    header("Accept-Language", "en-US")
                    header("Cache-Control", "no-cache")
                    header("Pragma", "no-cache")
                }
                connected = true
                logger.info("Successfully connected to Discord Gateway.")
                currentReconnectDelay = INITIAL_RECONNECT_DELAY
                // start receiving messages
                websocket!!.incoming.receiveAsFlow()
                    .collect {
                        when (it) {
                            is Frame.Text -> {
                                val jsonString = it.readText()
                                onMessage(json.decodeFromString(jsonString))
                            }

                            else -> {}
                        }
                    }
                handleClose()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Gateway connection error: ${e.message}")
                scheduleReconnection()
            }
        }
    }

    private fun scheduleReconnection() {
        if (!scopeJob.isActive) {
            return
        }
        if (reconnectionJob?.isActive == true) {
            return
        }
        heartbeatJob?.cancel()
        connected = false
        reconnectionJob = launch {
            try {
                delay(currentReconnectDelay)
                if (!scopeJob.isActive) return@launch
                logger.info("Attempting to reconnect...")
                connect()
                currentReconnectDelay = (currentReconnectDelay * 2).coerceAtMost(MAX_RECONNECT_DELAY)
            } catch (e: CancellationException) {
                // Cancelled during reconnection delay
            }
        }
    }


    private suspend fun handleClose() {
        heartbeatJob?.cancel()
        connected = false
        val close = try {
            websocket?.closeReason?.await()
        } catch (e: Exception) {
            null
        }
        logger.warning("Gateway closed with code: ${close?.code}, reason: ${close?.message}, can_reconnect: ${close?.code?.toInt() == 4000}")
        if (close?.code?.toInt() == 4000) {
            delay(200.milliseconds)
            connect()
        } else
            scheduleReconnection()
    }

    private suspend fun onMessage(payload: Payload) {
        logger.info("Gateway received: op=${payload.op}, seq=${payload.s}, event=${payload.t}")
        payload.s?.let {
            sequence = it
        }
        when (payload.op) {
            DISPATCH -> payload.handleDispatch()
            HEARTBEAT -> sendHeartBeat()
            RECONNECT -> reconnectWebSocket()
            INVALID_SESSION -> handleInvalidSession()
            HELLO -> payload.handleHello()
            else -> {}
        }
    }

    open fun Payload.handleDispatch() {
        when (this.t.toString()) {
            "READY" -> {
                val ready = json.decodeFromJsonElement<Ready>(this.d!!)
                sessionId = ready.sessionId
                resumeGatewayUrl = ready.resumeGatewayUrl + "/?v=9&encoding=json"
                logger.info("Gateway READY: resume_gateway_url updated to $resumeGatewayUrl, session_id updated to $sessionId")
                connected = true
                return
            }

            "RESUMED" -> {
                logger.info("Gateway: Session Resumed")
            }

            else -> {}
        }
    }

    private suspend inline fun handleInvalidSession() {
        logger.warning("Gateway: Handling Invalid Session. Sending Identify after 150ms")
        delay(150)
        sendIdentify()
    }

    private suspend inline fun Payload.handleHello() {
        if (sequence > 0 && !sessionId.isNullOrBlank()) {
            sendResume()
        } else {
            sendIdentify()
        }
        heartbeatInterval = json.decodeFromJsonElement<Heartbeat>(this.d!!).heartbeatInterval
        logger.info("Gateway: Setting heartbeatInterval=$heartbeatInterval")
        startHeartbeatJob(heartbeatInterval)
    }

    private suspend fun sendHeartBeat() {
        logger.info("Gateway: Sending $HEARTBEAT with seq: $sequence")
        send(
            op = HEARTBEAT,
            d = if (sequence == 0) "null" else sequence.toString(),
        )
    }

    private suspend inline fun reconnectWebSocket() {
        try {
            websocket?.close(
                CloseReason(
                    code = 4000,
                    message = "Attempting to reconnect"
                )
            )
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error during websocket reconnect", e)
        }
    }

    private suspend fun sendIdentify() {
        logger.info("Gateway: Sending $IDENTIFY")
        send(
            op = IDENTIFY,
            d = token.toIdentifyPayload(
                os = os,
                browser = browser,
                device = device
            )
        )
    }

    private suspend fun sendResume() {
        logger.info("Gateway: Sending $RESUME")
        send(
            op = RESUME,
            d = Resume(
                seq = sequence,
                sessionId = sessionId,
                token = token
            )
        )
    }

    private fun startHeartbeatJob(interval: Long) {
        heartbeatJob?.cancel()
        heartbeatJob = launch {
            try {
                while (isActive) {
                    sendHeartBeat()
                    delay(interval)
                }
            } catch (e: CancellationException) {
                // Heartbeat cancelled normally
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Heartbeat error: ${e.message}")
            }
        }
    }

    private fun isSocketConnectedToAccount(): Boolean {
        return connected && websocket?.isActive == true
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun isWebSocketConnected(): Boolean {
        return websocket?.incoming != null && websocket?.outgoing?.isClosedForSend == false
    }

    private suspend inline fun <reified T> send(op: OpCode, d: T?) {
        try {
            if (websocket?.isActive == true) {
                val payload = json.encodeToString(
                    Payload(
                        op = op,
                        d = json.encodeToJsonElement(d),
                    )
                )
                if (op == IDENTIFY) {
                    logger.info("Gateway sending payload: [REDACTED IDENTIFY PAYLOAD]")
                } else {
                    logger.info("Gateway sending payload: $payload")
                }
                websocket?.send(Frame.Text(payload))
            }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Gateway error sending payload $op: ${e.message}")
        }
    }

    fun close() {
        reconnectionJob?.cancel()
        heartbeatJob?.cancel()
        heartbeatJob = null
        resumeGatewayUrl = null
        sessionId = null
        connected = false
        scopeJob.cancelChildren()
        try {
            runBlocking {
                websocket?.close()
                websocket = null
                logger.info("Gateway: Connection to gateway closed")
            }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error closing websocket", e)
        }
    }

    suspend fun sendActivity(presence: Presence) {
        var attempts = 0
        while (!isSocketConnectedToAccount()) {
            delay(10.milliseconds)
            attempts++
            if (attempts > 500) { // 5-second timeout
                logger.warning("Gateway: Timeout waiting for socket connection to send activity")
                return
            }
        }
        logger.info("Gateway: Sending $PRESENCE_UPDATE")
        send(
            op = PRESENCE_UPDATE,
            d = presence
        )
    }
    companion object {
        private val INITIAL_RECONNECT_DELAY = 1.seconds
        private val MAX_RECONNECT_DELAY = 60.seconds
    }
}
