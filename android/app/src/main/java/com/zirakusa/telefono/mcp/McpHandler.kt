package com.zirakusa.telefono.mcp

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.security.MessageDigest

/** One HTTP answer for the relay. */
data class Reply(val status: Int, val body: String, val contentType: String = "application/json")

/**
 * Teléfono's MCP endpoint (Streamable HTTP, JSON answers). Every request carries a token (Authorization: Bearer,
 * X-Api-Key or ?token=); it is compared in constant time against the tokens being served. 120 requests a minute per
 * token; bodies over 1 MiB are refused.
 */
class McpHandler(private val tokens: () -> List<Token>, private val data: PhoneData, private val version: String,
    private val onUsed: (Token) -> Unit = {}, private val now: () -> Long = System::currentTimeMillis) {
    private val window = HashMap<String, Pair<Long, Int>>()

    suspend fun handle(method: String, path: String, headers: Map<String, String>, query: Map<String, String>, body: String): Reply {
        if (method == "OPTIONS") return Reply(204, "")
        if (path.trimEnd('/') !in setOf("/mcp", "")) return error(404, "Not found: use /mcp.")
        val token = grant(headers, query) ?: return error(401, "Teléfono: wrong, paused, expired or missing token. Create one in Teléfono › Ajustes › Modo MCP.")
        if (limited(token.id)) return error(429, "Teléfono: too many requests. Wait a minute.")
        if (method != "POST") return error(405, "Use POST with JSON-RPC (MCP Streamable HTTP).")
        if (body.length > MAX_BODY) return error(413, "Request too large.")
        onUsed(token)
        return try {
            val text = body.trim()
            if (text.startsWith("[")) {
                val array = JSONArray(text)
                val replies = (0 until array.length()).mapNotNull { rpc(array.getJSONObject(it), token) }
                if (replies.isEmpty()) Reply(202, "") else Reply(200, JSONArray(replies).toString())
            } else rpc(JSONObject(text), token)?.let { Reply(200, it.toString()) } ?: Reply(202, "")
        } catch (_: JSONException) { Reply(400, rpcError(null, -32700, "Invalid JSON").toString()) }
    }

    private suspend fun rpc(message: JSONObject, token: Token): JSONObject? {
        val id = message.opt("id")
        if (id == null || id == JSONObject.NULL) return null // a notification: nothing to answer
        val params = message.optJSONObject("params") ?: JSONObject()
        val result = when (val method = message.optString("method")) {
            "initialize" -> JSONObject()
                .put("protocolVersion", params.optString("protocolVersion").takeIf { it in VERSIONS } ?: VERSIONS.first())
                .put("capabilities", JSONObject().put("tools", JSONObject().put("listChanged", false)))
                .put("serverInfo", JSONObject().put("name", "telefono").put("title", "Teléfono").put("version", version))
                .put("instructions", INSTRUCTIONS + "Errors carry a code (structuredContent.error.code).")
                .put("_meta", JSONObject().put(TOKEN_META, JSONObject().put("name", token.name).put("kind", token.kind.key)
                    .put("usage", token.usage).put("mode", token.access.key).put("expiresAt", token.expiresAt)))
            "ping" -> JSONObject()
            "tools/list" -> JSONObject().put("tools", JSONArray(PhoneTools.available(token.access).map {
                JSONObject().put("name", it.name).put("description", it.description).put("inputSchema", it.schema)
                    .put("annotations", JSONObject().put("readOnlyHint", !it.write).put("destructiveHint", it.name in DESTRUCTIVE))
            }))
            "tools/call" -> call(params, token)
            "resources/list" -> JSONObject().put("resources", JSONArray())
            "resources/templates/list" -> JSONObject().put("resourceTemplates", JSONArray())
            "prompts/list" -> JSONObject().put("prompts", JSONArray())
            else -> return rpcError(id, -32601, "Method not found: $method")
        }
        return JSONObject().put("jsonrpc", "2.0").put("id", id).put("result", result)
    }

    private suspend fun call(params: JSONObject, token: Token): JSONObject = try {
        val out = PhoneTools.call(params.optString("name"), params.optJSONObject("arguments") ?: JSONObject(), token.access, data, token.name)
        JSONObject().put("content", JSONArray().put(JSONObject().put("type", "text").put("text", out.toString()))).put("structuredContent", out)
    } catch (failure: ToolFailure) {
        val error = JSONObject().put("error", JSONObject().put("code", failure.code).put("message", failure.message))
        JSONObject().put("isError", true).put("content", JSONArray().put(JSONObject().put("type", "text").put("text", "${failure.code}: ${failure.message}")))
            .put("structuredContent", error)
    }

    private fun grant(headers: Map<String, String>, query: Map<String, String>): Token? {
        val lower = headers.mapKeys { it.key.lowercase() }
        val given = (lower["authorization"]?.takeIf { it.startsWith("Bearer ", true) }?.substring(7)?.trim()
            ?: lower["x-api-key"]?.trim() ?: query["token"]?.trim())?.takeIf { it.isNotEmpty() }?.toByteArray() ?: return null
        val at = now()
        return tokens().firstOrNull { MessageDigest.isEqual(given, it.secret.toByteArray()) }?.takeIf { it.usable(at) }
    }

    @Synchronized private fun limited(id: String): Boolean {
        val at = now()
        val (since, count) = window[id]?.takeIf { at - it.first < 60_000 } ?: (at to 0)
        window[id] = since to count + 1
        return count + 1 > PER_MINUTE
    }

    private fun error(status: Int, message: String) = Reply(status, rpcError(null, -32000, message).toString())
    private fun rpcError(id: Any?, code: Int, message: String) = JSONObject().put("jsonrpc", "2.0").put("id", id ?: JSONObject.NULL)
        .put("error", JSONObject().put("code", code).put("message", message))

    companion object {
        const val INSTRUCTIONS = "The phone's calls (Teléfono). phone_calls lists the recent calls, phone_missed the missed ones and " +
            "phone_call one of them. With a read-and-write token the AI can ask the user to call someone (phone_call_request): a " +
            "notification the user taps; the AI never dials, answers or hangs up. "
        val DESTRUCTIVE = emptySet<String>()
        /** Read by Gateway Hub (as Stone's): which token this is and what it is for, never the secret. */
        const val TOKEN_META = "com.zirakusa/token"
        val VERSIONS = listOf("2025-06-18", "2025-03-26", "2024-11-05")
        const val PER_MINUTE = 120
        const val MAX_BODY = 1 shl 20
    }
}
