package com.zirakusa.telefono.mcp

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle

/**
 * «Redireccionar a Hub», as in Stone: the Gateway Hub app on this phone gets Teléfono's hub link by itself. Three locks:
 * Android's signature permission on this provider, the caller's signature checked again here, and the user's switch.
 */
object HubLink {
    fun answer(sameDeveloper: Boolean): Bundle = when {
        !sameDeveloper -> error("NOT_SAME_DEVELOPER", "Solo apps firmadas por el mismo desarrollador que Teléfono.")
        !McpMode.hubRedirect -> error("REDIRECT_OFF", "Activa «Redireccionar a Hub» en Teléfono › Ajustes › Modo MCP.")
        McpMode.relayUrl.isBlank() -> error("NO_RELAY", "Esta versión de Teléfono no tiene enlace público (relay).")
        else -> Bundle().apply { putString("link", McpMode.hubLink()) }
    }
    private fun error(code: String, message: String) = Bundle().apply { putString("error", code); putString("message", message) }
}

class HubLinkProvider : ContentProvider() {
    override fun onCreate() = true
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != "link") return null
        val context = context ?: return null
        val caller = callingPackage
        val same = caller != null && context.packageManager.checkSignatures(context.packageName, caller) == PackageManager.SIGNATURE_MATCH
        if (same) McpMode.load(context)
        return HubLink.answer(same)
    }
    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0
}
