package com.zirakusa.telefono.mcp

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.URLDecoder
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Keeps Teléfono reachable at a fixed public link through the same relay Stone uses (one device id per app and phone).
 * The phone dials out over a WebSocket; each forwarded request is answered here by [McpHandler], which checks the
 * token. Encrypted only (https/wss), except a relay on this same machine in tests.
 */
class RelayLink(private val relayUrl: String, private val device: String, private val secret: String,
    private val handler: McpHandler, private val onState: (State) -> Unit) {
    enum class State { CONNECTING, ONLINE, OFFLINE }

    private val http = OkHttpClient.Builder().pingInterval(25, TimeUnit.SECONDS).readTimeout(0, TimeUnit.MILLISECONDS).build()
    private val workers = ThreadPoolExecutor(4, 4, 0, TimeUnit.MILLISECONDS, ArrayBlockingQueue(32))
    private val retry = Executors.newSingleThreadScheduledExecutor()
    @Volatile private var socket: WebSocket? = null
    @Volatile private var running = false
    private var attempt = 0

    fun start() { if (running) return; running = true; attempt = 0; connect() }

    fun stop() { running = false; socket?.close(1000, "off"); socket = null; onState(State.OFFLINE) }

    private fun connect() {
        if (!running) return
        if (!allowed(relayUrl)) { onState(State.OFFLINE); return }
        onState(State.CONNECTING)
        val url = relayUrl.replaceFirst("https://", "wss://").replaceFirst("http://", "ws://") + "/connect?device=$device"
        socket = http.newWebSocket(Request.Builder().url(url).header("X-Stone-Secret", secret).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { attempt = 0; onState(State.ONLINE) }
            override fun onMessage(webSocket: WebSocket, text: String) {
                val message = runCatching { JSONObject(text) }.getOrNull() ?: return
                if (message.optString("t") != "req") return
                try { workers.execute { webSocket.send(answer(message).toString()) } }
                catch (_: RejectedExecutionException) { webSocket.send(reply(message, Reply(503, """{"error":"busy"}""")).toString()) }
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = later(webSocket, reason == "replaced")
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) =
                later(webSocket, response?.code in setOf(400, 401, 403)) // another phone owns this id, or a bad request: stop
        })
    }

    private fun later(closed: WebSocket, permanent: Boolean) {
        if (socket !== closed) return
        socket = null
        if (!running) return
        if (permanent) { running = false; onState(State.OFFLINE); return }
        onState(State.CONNECTING)
        attempt++
        retry.schedule(::connect, minOf(300L, 2L shl minOf(attempt, 7)), TimeUnit.SECONDS)
    }

    private fun answer(message: JSONObject): JSONObject {
        val headers = message.optJSONObject("headers")?.let { h -> h.keys().asSequence().associateWith { h.getString(it) } }.orEmpty()
        val query = message.optString("query").removePrefix("?").split('&').filter { '=' in it }.associate {
            URLDecoder.decode(it.substringBefore('='), "UTF-8") to URLDecoder.decode(it.substringAfter('='), "UTF-8")
        }
        val result = runCatching {
            runBlocking { handler.handle(message.optString("method", "POST"), message.optString("path", "/mcp"), headers, query, message.optString("body")) }
        }.getOrElse { Reply(500, """{"jsonrpc":"2.0","id":null,"error":{"code":-32603,"message":"Teléfono: internal error"}}""") }
        return reply(message, result)
    }

    private fun reply(message: JSONObject, r: Reply) = JSONObject().put("t", "res").put("id", message.optString("id")).put("status", r.status)
        .put("headers", JSONObject().put("Content-Type", r.contentType)).put("body", r.body)

    companion object {
        fun allowed(url: String) = url.startsWith("https://") || Regex("^http://(127\\.0\\.0\\.1|localhost)(:\\d+)?(/.*)?$").matches(url)
    }
}
