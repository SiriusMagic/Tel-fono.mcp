package com.zirakusa.telefono

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.zirakusa.telefono.data.Call
import com.zirakusa.telefono.data.Dial
import com.zirakusa.telefono.data.Numbers
import java.time.LocalDate
import java.time.ZoneId

/** The lines of «Hoy» for the family summary: today's missed calls, by person («Ana Pérez (2)»). Pure. */
object SummaryLines {
    const val MAX = 5
    fun lines(calls: List<Call>, today: LocalDate, zone: ZoneId, unknown: String): List<String> =
        Dial.group(Dial.missed(calls, today, zone), zone).take(MAX).map { g ->
            g.first.name.ifBlank { Numbers.pretty(g.first.number).ifBlank { unknown } } + if (g.count > 1) " (${g.count})" else ""
        }
}

/** The family summary (see Inicio's FAMILIA.md): missed calls and birthdays for Inicio's «Hoy». Only apps signed by the same developer may ask. */
class SummaryProvider : ContentProvider() {
    override fun onCreate() = true
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != "summary") return null
        val context = context ?: return null
        val caller = callingPackage ?: return null
        if (context.packageManager.checkSignatures(context.packageName, caller) != PackageManager.SIGNATURE_MATCH) return null
        val calls = runCatching { AndroidBook(context).calls(200) }.getOrDefault(emptyList())
        val lines = SummaryLines.lines(calls, LocalDate.now(), ZoneId.systemDefault(), context.getString(R.string.private_number))
        return Bundle().apply {
            putString("title", context.getString(R.string.summary_title))
            // Missed calls first, then the birthdays of the week (Contactos is inside Teléfono).
            putStringArrayList("lines", ArrayList((lines + com.zirakusa.contactos.Birthdays.lines(context)).take(SummaryLines.MAX)))
            putString("empty", context.getString(R.string.summary_empty))
        }
    }
    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0
}
