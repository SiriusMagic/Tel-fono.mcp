package com.zirakusa.telefono.mcp

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.zirakusa.telefono.BuildConfig
import com.zirakusa.telefono.data.Settings
import com.zirakusa.telefono.data.SharedValues

/**
 * Ajustes › Modo MCP, as in the rest of the family: «Activar MCP» with the user's tokens
 * (LLM or Hub), or «Redireccionar a Hub», where only the hub's own token is served. Off by default. Runs while the app
 * is alive.
 */
object McpMode {
    var enabled by mutableStateOf(false); private set
    var hubRedirect by mutableStateOf(false); private set
    var tokens by mutableStateOf(emptyList<Token>()); private set
    var state by mutableStateOf(RelayLink.State.OFFLINE); private set
    /** Called on the main thread after a tool changed the places or got a new forecast, so the screen reloads them. */
    var onChanged: () -> Unit = {}

    private var prefs: android.content.SharedPreferences? = null
    private var link: RelayLink? = null
    private var device = ""
    private var relaySecret = ""
    internal var relayUrl: String = BuildConfig.RELAY_URL
    val active get() = enabled || hubRedirect
    /** The tokens the user manages (the redirect token is never listed). */
    val listed get() = tokens.filter { it.kind != Kind.REDIRECT }

    fun load(context: Context) {
        if (prefs != null) return
        val app = context.applicationContext
        val p = app.getSharedPreferences("mcp", Context.MODE_PRIVATE)
        prefs = p
        enabled = p.getBoolean("enabled", false); hubRedirect = p.getBoolean("redirect", false)
        tokens = Token.parse(SecretBox.open(p.getString("tokens", "") ?: "").ifBlank { "[]" })
        device = p.getString("device", null) ?: Token.id().repeat(2).also { p.edit { putString("device", it) } }
        relaySecret = SecretBox.open(p.getString("relay", "") ?: "").ifBlank {
            Token.secret().also { p.edit { putString("relay", SecretBox.seal(it)) } }
        }
        data = LiveData(app)
        apply()
    }

    fun enable(on: Boolean) { enabled = on; prefs?.edit { putBoolean("enabled", on) }; apply() }
    fun redirect(on: Boolean) { hubRedirect = on; prefs?.edit { putBoolean("redirect", on) }; if (on) redirectToken(); apply() }

    fun create(name: String, access: Access, kind: Kind, usage: String, days: Int): Token {
        val now = System.currentTimeMillis()
        return Token(Token.id(), name.trim().take(40).ifEmpty { "Agente ${listed.size + 1}" }, Token.secret(), access, kind,
            usage.trim().take(Token.USAGE_MAX), now, if (days > 0) now + days * 86_400_000L else 0).also { save(tokens + it) }
    }
    fun setPaused(id: String, paused: Boolean) = save(tokens.map { if (it.id == id) it.copy(paused = paused) else it })
    fun revoke(id: String) = save(tokens.filterNot { it.id == id })

    /** The hub's token while redirecting: full access, never expires, never listed. */
    fun redirectToken(): Token = tokens.firstOrNull { it.kind == Kind.REDIRECT }
        ?: Token(Token.id(), "Gateway Hub", Token.secret(), Access.WRITE, Kind.REDIRECT, "Redireccionar a Hub", System.currentTimeMillis())
            .also { save(tokens + it) }

    fun served(): List<Token> = tokens.filter { (it.kind == Kind.REDIRECT) == hubRedirect }
    fun linkFor(secret: String) = "${relayUrl.trimEnd('/')}/d/$device/mcp?token=$secret"
    fun hubLink() = linkFor(redirectToken().secret)

    private fun save(list: List<Token>) { tokens = list; prefs?.edit { putString("tokens", SecretBox.seal(Token.serialize(list))) } }

    private var data: PhoneData? = null

    private fun apply() {
        val d = data ?: return
        if (active && relayUrl.isNotBlank()) {
            if (link == null) link = RelayLink(relayUrl, device, relaySecret,
                McpHandler(::served, d, BuildConfig.VERSION_NAME, onUsed = ::touch)) { state = it }.also { it.start() }
        } else { link?.stop(); link = null; state = RelayLink.State.OFFLINE }
    }

    private fun touch(token: Token) {
        val now = System.currentTimeMillis()
        if (now - token.lastUsedAt > 60_000) save(tokens.map { if (it.id == token.id) it.copy(lastUsedAt = now) else it })
    }
}


/** The real call log and the notification for the tools. */
class LiveData(private val context: Context) : PhoneData {
    override val book = com.zirakusa.telefono.AndroidBook(context)
    override fun askToCall(who: String, number: String, name: String) = com.zirakusa.telefono.Requests.post(context, who, number, name)
}
