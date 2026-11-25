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
 * The Modo MCP, run by Arka (same developer, through [HubLinkProvider]): «connect» turns the public link on (the relay,
 * with Arka's own token, so the hub decides who may do what) and «disconnect» turns it off. Lucía calls the tools on the
 * phone with [local], without internet. Off by default; nothing to set up in this app.
 */
object McpMode {
    var remote by mutableStateOf(false); private set
    var state by mutableStateOf(RelayLink.State.OFFLINE); private set
    /** Called on the main thread after a tool changed the places or got a new forecast, so the screen reloads them. */
    var onChanged: () -> Unit = {}

    private var prefs: android.content.SharedPreferences? = null
    private var link: RelayLink? = null
    private var device = ""
    private var relaySecret = ""
    private var token = Token("")
    internal var relayUrl: String = BuildConfig.RELAY_URL

    fun load(context: Context) {
        if (prefs != null) return
        val app = context.applicationContext
        val p = app.getSharedPreferences("mcp", Context.MODE_PRIVATE)
        prefs = p
        remote = p.getBoolean("redirect", false)
        token = Token(SecretBox.open(p.getString("arka", "") ?: "").ifBlank {
            (Legacy.redirectSecret(SecretBox.open(p.getString("tokens", "") ?: "")) ?: Token.secret())
                .also { p.edit { putString("arka", SecretBox.seal(it)); remove("tokens"); remove("enabled") } }
        })
        device = p.getString("device", null) ?: Token.id().repeat(2).also { p.edit { putString("device", it) } }
        relaySecret = SecretBox.open(p.getString("relay", "") ?: "").ifBlank {
            Token.secret().also { p.edit { putString("relay", SecretBox.seal(it)) } }
        }
        contacts = com.zirakusa.contactos.mcp.ContactLiveData(app)
        data = LiveData(app)
        apply()
    }

    /** Arka's «Activar MCP» for this app: the public link on or off. */
    fun turnRemote(on: Boolean) { remote = on; prefs?.edit { putBoolean("redirect", on) }; apply() }

    fun hubLink() = "${relayUrl.trimEnd('/')}/d/$device/mcp?token=${token.secret}"

    private var data: PhoneData? = null
    private var contacts: com.zirakusa.contactos.mcp.ContactData? = null

    /** One JSON-RPC request from Lucía on this phone, through the same handler as the relay. Null before [load]. */
    fun local(body: String): String? {
        val d = data ?: return null
        return kotlinx.coroutines.runBlocking {
            McpHandler({ listOf(token) }, d, BuildConfig.VERSION_NAME, contacts = contacts).handle("POST", "/mcp", mapOf("Authorization" to "Bearer ${token.secret}"), emptyMap(), body).body
        }
    }

    private fun apply() {
        val d = data ?: return
        if (remote && relayUrl.isNotBlank()) {
            if (link == null) link = RelayLink(relayUrl, device, relaySecret,
                McpHandler({ listOf(token) }, d, BuildConfig.VERSION_NAME, contacts = contacts)) { state = it }.also { it.start() }
        } else { link?.stop(); link = null; state = RelayLink.State.OFFLINE }
    }
}


/** The real call log and the notification for the tools. */
class LiveData(private val context: Context) : PhoneData {
    override val book = com.zirakusa.telefono.AndroidBook(context)
    override fun askToCall(who: String, number: String, name: String) = com.zirakusa.telefono.Requests.post(context, who, number, name)
}
