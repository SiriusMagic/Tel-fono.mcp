package com.zirakusa.telefono

import com.zirakusa.telefono.data.MemoryBook
import com.zirakusa.telefono.mcp.Access
import com.zirakusa.telefono.mcp.HubLink
import com.zirakusa.telefono.mcp.Kind
import com.zirakusa.telefono.mcp.McpHandler
import com.zirakusa.telefono.mcp.PhoneData
import com.zirakusa.telefono.mcp.Token
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset

/** Teléfono's MCP: tools list (no dialling), good calls (with the SIM slot), the call request, bad arguments, not found, no permission, read-only, tokens, hub link. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class McpTest {
    private val memory = MemoryBook(Book.calls, Book.people, Book.sims)
    private val data = object : PhoneData {
        override val book = memory
        val asked = mutableListOf<Triple<String, String, String>>()
        var notifications = true
        override fun zone() = ZoneOffset.UTC
        override fun today(): LocalDate = LocalDate.of(2026, 11, 21)
        override fun askToCall(who: String, number: String, name: String) = notifications.also { if (it) asked += Triple(who, number, name) }
    }
    private val writer = Token("w", "Claude", "tel_" + "w".repeat(40), Access.WRITE, Kind.HUB, "Teléfono")
    private val reader = Token("r", "Lector", "tel_" + "r".repeat(40), Access.READ)
    private val paused = Token("p", "Viejo", "tel_" + "p".repeat(40), Access.WRITE, paused = true)
    private val handler = McpHandler({ listOf(writer, reader, paused) }, data, "0.1.0")

    private fun rpc(token: Token, method: String, params: JSONObject = JSONObject()) = runBlocking {
        JSONObject(handler.handle("POST", "/mcp", mapOf("Authorization" to "Bearer ${token.secret}"), emptyMap(),
            JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", method).put("params", params).toString()).body).getJSONObject("result")
    }
    private fun tool(name: String, args: JSONObject, token: Token = writer) = rpc(token, "tools/call", JSONObject().put("name", name).put("arguments", args))
    private fun ok(name: String, args: JSONObject = JSONObject()) =
        tool(name, args).also { assertFalse(it.toString(), it.optBoolean("isError")) }.getJSONObject("structuredContent")
    private fun code(name: String, args: JSONObject = JSONObject(), token: Token = writer) = tool(name, args, token).also { assertTrue(it.optBoolean("isError")) }
        .getJSONObject("structuredContent").getJSONObject("error").getString("code")
    private fun names(token: Token) = rpc(token, "tools/list").getJSONArray("tools").let { t -> (0 until t.length()).map { t.getJSONObject(it).getString("name") } }

    @Test fun listsTheToolsWithoutDialling() {
        assertEquals(listOf("phone_calls", "phone_missed", "phone_call", "phone_call_request"), names(writer))
        assertEquals(listOf("phone_calls", "phone_missed", "phone_call"), names(reader))
        assertEquals("UNKNOWN_TOOL", code("phone_dial", JSONObject().put("number", "0991234567")))
        assertEquals("telefono", rpc(writer, "initialize").getJSONObject("serverInfo").getString("name"))
    }

    @Test fun readsAndAsks() {
        val calls = ok("phone_calls", JSONObject().put("limit", 2)).getJSONArray("calls")
        assertEquals(2, calls.length()); assertEquals("missed", calls.getJSONObject(0).getString("type")); assertEquals("2026-11-21T09:05", calls.getJSONObject(0).getString("at"))
        assertEquals(2, calls.getJSONObject(0).getInt("sim")); assertFalse(calls.getJSONObject(1).has("sim"))           // SIM slot only when known
        assertEquals(1, ok("phone_call", JSONObject().put("id", 3)).getJSONObject("call").getInt("sim"))
        assertEquals(1, ok("phone_calls", JSONObject().put("type", "outgoing")).getJSONArray("calls").length())
        assertEquals(2, ok("phone_missed").getInt("total")); assertEquals(3, ok("phone_missed", JSONObject().put("since", "2026-11-19")).getInt("total"))
        assertEquals(3723, ok("phone_call", JSONObject().put("id", 4)).getJSONObject("call").getInt("seconds"))
        val asked = ok("phone_call_request", JSONObject().put("number", "+593 99 123 4567").put("reason", "Confirmar la cena"))
        assertEquals("Ana Pérez", asked.getString("name"))
        assertEquals(Triple("Claude", "+593991234567", "Ana Pérez · Confirmar la cena"), data.asked.single())
    }

    @Test fun badArgumentsAreRefused() {
        assertEquals("BAD_ARGUMENT", code("phone_calls", JSONObject().put("type", "lost")))
        assertEquals("BAD_ARGUMENT", code("phone_calls", JSONObject().put("limit", 500)))
        assertEquals("BAD_ARGUMENT", code("phone_missed", JSONObject().put("since", "2020-01-01")))
        assertEquals("BAD_ARGUMENT", code("phone_missed", JSONObject().put("since", "mañana")))
        assertEquals("BAD_ARGUMENT", code("phone_call_request", JSONObject().put("number", "12")))
        assertEquals("BAD_ARGUMENT", code("phone_call_request", JSONObject().put("number", "*#06#")))          // no codes for the AI
        assertEquals("BAD_ARGUMENT", code("phone_call_request", JSONObject().put("number", "0991234567").put("auto", true)))
        assertTrue(data.asked.isEmpty())
    }

    @Test fun notFoundNoPermissionNotificationsAndReadOnly() {
        assertEquals("NOT_FOUND", code("phone_call", JSONObject().put("id", 99)))
        assertEquals("READ_ONLY", code("phone_call_request", JSONObject().put("number", "0991234567"), reader))
        data.notifications = false
        assertEquals("NOTIFICATIONS_OFF", code("phone_call_request", JSONObject().put("number", "0991234567")))
        memory.allowed = false
        assertEquals("NO_PERMISSION", code("phone_calls"))
    }

    @Test fun wrongAndPausedTokensAndHubLink() = runBlocking {
        val body = """{"jsonrpc":"2.0","id":1,"method":"tools/list"}"""
        assertEquals(401, handler.handle("POST", "/mcp", mapOf("Authorization" to "Bearer ${paused.secret}"), emptyMap(), body).status)
        assertEquals(401, handler.handle("POST", "/mcp", mapOf("Authorization" to "Bearer tel_wrong"), emptyMap(), body).status)
        assertTrue(Token.secret().startsWith("tel_"))
        assertEquals("NOT_SAME_DEVELOPER", HubLink.answer(false).getString("error"))
        assertEquals("REDIRECT_OFF", HubLink.answer(true).getString("error"))
    }
}
