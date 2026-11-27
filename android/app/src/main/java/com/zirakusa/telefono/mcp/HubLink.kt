package com.zirakusa.telefono.mcp

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle

/**
 * How Arka runs this app's Modo MCP. Two locks: Android's signature permission on [HubLinkProvider] and the caller's
 * signature checked again here. «link» gives the hub link (only with the public link on), «connect» / «disconnect» turn
 * it on or off (Arka's «Activar MCP», the user's tap there is the consent), «status» says how it is, and «tool» runs one
 * JSON-RPC request on the phone for Lucía (Arka's «Permitir MCP local» decides whether she asks).
 */
object HubLink {
    fun answer(sameDeveloper: Boolean): Bundle = when {
        !sameDeveloper -> error("NOT_SAME_DEVELOPER", "Solo apps firmadas por el mismo desarrollador que Teléfono.")
        !McpMode.remote -> error("REDIRECT_OFF", "Activa «Redireccionar a Hub» en los ajustes de la app.")
        McpMode.relayUrl.isBlank() -> error("NO_RELAY", "Esta versión de Teléfono no tiene enlace público (relay).")
        else -> Bundle().apply { putString("link", McpMode.hubLink()) }
    }
    fun connect(sameDeveloper: Boolean): Bundle { if (sameDeveloper) McpMode.turnRemote(true); return answer(sameDeveloper) }
    fun disconnect(sameDeveloper: Boolean): Bundle =
        if (!sameDeveloper) answer(false) else { McpMode.turnRemote(false); status(true) }
    fun status(sameDeveloper: Boolean): Bundle = if (!sameDeveloper) answer(false) else Bundle().apply {
        putBoolean("remote", McpMode.remote)
        putBoolean("enabled", McpMode.enabled)
        putBoolean("localMcp", McpMode.localMcp)
        putString("state", McpMode.state.name)
    }

    fun tool(sameDeveloper: Boolean, request: String?): Bundle = when {
        !sameDeveloper -> answer(false)
        request.isNullOrBlank() -> error("BAD_REQUEST", "Falta la petición.")
        !McpMode.localMcp -> error("LOCAL_DISABLED", "Activa «Permitir MCP local» en los ajustes de la app.")
        else -> McpMode.local(request)?.let { r -> Bundle().apply { putString("response", r) } } ?: error("NOT_READY", "La app aún no está lista.")
    }

    private fun error(code: String, message: String) = Bundle().apply { putString("error", code); putString("message", message) }
}

/** The hub token of versions before Arka took over («Redireccionar a Hub»): kept, so the hub's link keeps working. */
internal object Legacy {
    fun redirectSecret(tokens: String): String? = runCatching {
        val array = org.json.JSONArray(tokens)
        (0 until array.length()).map { array.getJSONObject(it) }.firstOrNull { it.optString("kind") == "redirect" }
            ?.optString("secret")?.takeIf { it.length >= 32 }
    }.getOrNull()
}

class HubLinkProvider : ContentProvider() {
    override fun onCreate() = true
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method !in METHODS) return null
        val context = context ?: return null
        val caller = callingPackage
        val same = caller != null && context.packageManager.checkSignatures(context.packageName, caller) == PackageManager.SIGNATURE_MATCH
        if (same) McpMode.load(context)
        return when (method) {
            "connect" -> HubLink.connect(same)
            "disconnect" -> HubLink.disconnect(same)
            "status" -> HubLink.status(same)
            "tool" -> HubLink.tool(same, extras?.getString("request"))
            else -> HubLink.answer(same)
        }
    }
    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0

    private companion object { val METHODS = setOf("link", "connect", "disconnect", "status", "tool") }
}
