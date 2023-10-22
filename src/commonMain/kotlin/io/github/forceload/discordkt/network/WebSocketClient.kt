package io.github.forceload.discordkt.network

import io.github.forceload.discordkt.type.gateway.GatewayEvent
import io.github.forceload.discordkt.type.gateway.event.GatewayEventType
import io.github.forceload.discordkt.util.SerializerUtil
import io.github.forceload.discordkt.util.logger.DebugLogger
import io.github.forceload.discordkt.util.logger.WarnLogger
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.KSerializer

class WebSocketClient(val host: String, val url: String, val params: HashMap<String, Any>) {
    init {
        println(url)
    }

    companion object {
        private val client = HttpClient(CIO) {
            install(WebSockets)
        }

        private fun convertURL(url: String) =
            if (url.startsWith("ws://")) { url.drop(5) }
            else if (url.startsWith("wss://")) { url.drop(6) }
            else { url }

        fun newInstance(host: String, url: String = "", version: Int) =
            WebSocketClient(convertURL(host), url, hashMapOf("v" to version, "encoding" to "json"))
    }

    private val events = ArrayDeque<String>()
    private val messageQueue = ArrayDeque<String>()
    private var isRunning = false

    private var session: DefaultClientWebSocketSession? = null
    private var reason: CloseReason? = null

    fun send(message: String) = messageQueue.add(message)
    fun <T> send(obj: T, serializer: KSerializer<T>) =
        send(SerializerUtil.jsonBuild.encodeToString(serializer, obj))

    fun send(message: GatewayEvent) = send(message, GatewayEvent.Serializer)
    fun send(message: GatewayEventType) = send(GatewayEvent(message.opCode, message))

    fun close(reason: CloseReason = CloseReason(CloseReason.Codes.NORMAL, "Closed Normally")) {
        this.reason = reason
        isRunning = false
    }

    fun close(code: Short) = close("", code)
    fun close(reason: String) = close(reason, CloseReason.Codes.NORMAL)
    fun close(reason: String, code: CloseReason.Codes) = close(CloseReason(code, reason))
    fun close(reason: String, code: Short) = close(CloseReason(code, reason))

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun launch(code: WebSocketClient.(messages: Array<String>) -> Unit) {
        if (isRunning) return
        else isRunning = true

        val paramUrl = StringBuilder()
        params.onEachIndexed { index, entry ->
            paramUrl.append(if (index == 0) "?" else "&")
            paramUrl.append("${entry.key}=${entry.value}")
        }

        @Suppress("LocalVariableName")
        val IOScope = CoroutineScope(Dispatchers.IO)

        client.webSocket(
            method = HttpMethod.Get, host = host, path = "/${url}${paramUrl}"
        ) client@ {
            session = this
            while (isRunning) {
                var i = 0
                try {
                    if (!incoming.isEmpty) IOScope.launch {
                        loop@ while (!incoming.isEmpty) {
                            val message = incoming.receive() as? Frame.Text? ?: break@loop
                            val msgString = message.readText()
                            DebugLogger.log("Receive ${i++}: $msgString")
                            events.add(msgString)
                        }
                    }
                } catch (err: ClosedReceiveChannelException) {
                    val reason = this.closeReason.await()!!
                    WarnLogger.log("Close Code: ${reason.code}\nMessage: ${reason.message}")
                    return@client
                }

                this@WebSocketClient.code(events.clean())

                i = 0
                while (messageQueue.isNotEmpty()) {
                    val msgString = messageQueue.removeFirst()
                    DebugLogger.log("Send ${i++}: $msgString")
                    IOScope.launch { send(msgString) }
                }

                if ((!isRunning || !this.isActive) && reason != null) {
                    val knownReason = when (reason!!.knownReason) {
                        CloseReason.Codes.GOING_AWAY -> {
                            if (!isRunning) CloseReason.Codes.INTERNAL_ERROR
                            else reason!!.knownReason
                        }

                        else -> reason!!.knownReason
                    }

                    WarnLogger.log("${knownReason}: ${reason!!.message}")
                } else if (!this.isActive) {
                    val reason = this.closeReason.await()!!
                    WarnLogger.log("Close Code: ${reason.code}\nMessage: ${reason.message}")
                    return@client
                }
            }

            this.close(reason!!)
        }
    }
}

private inline fun <reified E> ArrayDeque<E>.clean(): Array<E> {
    val length = this.size
    val arrayList = ArrayList<E>()
    for (i in 0..<length) arrayList.add(this.removeFirst())
    return arrayList.toTypedArray()
}
