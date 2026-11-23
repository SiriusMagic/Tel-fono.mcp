package com.zirakusa.telefono.mcp

import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.util.Base64

/** As in the sibling apps: READ reads the weather and places; WRITE may also add and remove places. */
enum class Access(val key: String) {
    READ("read"), WRITE("write");
    companion object { fun from(key: String?) = entries.firstOrNull { it.key == key } ?: READ }
}

/** As in Stone: LLM (link pasted in the AI), HUB (pasted in Gateway Hub, a bridge), REDIRECT (the hub manages all). */
enum class Kind(val key: String) {
    LLM("llm"), HUB("hub"), REDIRECT("redirect");
    companion object { fun from(key: String?) = entries.firstOrNull { it.key == key } ?: LLM }
}

data class Token(val id: String, val name: String, val secret: String, val access: Access, val kind: Kind = Kind.LLM,
    val usage: String = "", val createdAt: Long = 0, val expiresAt: Long = 0, val paused: Boolean = false, val lastUsedAt: Long = 0) {
    fun usable(now: Long) = !paused && (expiresAt == 0L || expiresAt > now)
    val expired get() = expiresAt in 1..System.currentTimeMillis()

    fun json(): JSONObject = JSONObject().put("id", id).put("name", name).put("secret", secret).put("access", access.key)
        .put("kind", kind.key).put("usage", usage).put("createdAt", createdAt).put("expiresAt", expiresAt).put("paused", paused)
        .put("lastUsedAt", lastUsedAt)

    companion object {
        const val USAGE_MAX = 300
        fun parse(text: String): List<Token> = runCatching {
            val array = JSONArray(text)
            (0 until array.length()).map { array.getJSONObject(it) }.map {
                Token(it.getString("id"), it.optString("name"), it.getString("secret"), Access.from(it.optString("access")),
                    Kind.from(it.optString("kind")), it.optString("usage"), it.optLong("createdAt"), it.optLong("expiresAt"),
                    it.optBoolean("paused"), it.optLong("lastUsedAt"))
            }.filter { it.secret.length >= 32 }
        }.getOrDefault(emptyList())
        fun serialize(tokens: List<Token>) = JSONArray(tokens.map { it.json() }).toString()

        /** "tel_" + 256 random bits: the prefix lets secret scanners recognise a leaked Teléfono token. */
        fun secret(): String = "tel_" + Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also(SecureRandom()::nextBytes))
        fun id(): String = ByteArray(6).also(SecureRandom()::nextBytes).joinToString("") { "%02x".format(it) }
    }
}
