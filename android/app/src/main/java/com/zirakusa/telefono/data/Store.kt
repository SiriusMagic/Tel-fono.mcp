package com.zirakusa.telefono.data

import androidx.core.content.edit

/** Where the calls and contacts come from: Android's call log and contacts in the app, lists in the tests. */
interface PhoneBook {
    /** The last calls, newest first; throws [NoAccess] without the permission. */
    fun calls(limit: Int = 500): List<Call>
    /** The contacts with numbers; empty without the permission. */
    fun people(): List<Person>
}

/** The user hasn't allowed Teléfono to read the call log. */
class NoAccess : Exception()

class MemoryBook(var callList: List<Call> = emptyList(), var peopleList: List<Person> = emptyList()) : PhoneBook {
    var allowed = true
    override fun calls(limit: Int): List<Call> { if (!allowed) throw NoAccess(); return callList.sortedByDescending { it.at }.take(limit) }
    override fun people() = peopleList
}

/** Where the settings live: the phone's preferences in the app, a map in the tests. */
interface KeyValues {
    fun get(key: String): String?
    fun put(key: String, value: String?)
}

class MapValues(private val map: MutableMap<String, String> = mutableMapOf()) : KeyValues {
    override fun get(key: String) = map[key]
    override fun put(key: String, value: String?) { if (value == null) map.remove(key) else map[key] = value }
}

/** Every setting of Teléfono, typed. */
class Settings(private val kv: KeyValues) {
    private fun flag(key: String, default: Boolean) = kv.get(key)?.let { it == "1" } ?: default
    /** Keypad tones when pressing keys. */
    var tones: Boolean get() = flag("tones", true); set(v) = kv.put("tones", if (v) "1" else "0")
    /** Call at once from Teléfono (needs the phone permission); off: the number opens in the phone's dialer. */
    var direct: Boolean get() = flag("direct", true); set(v) = kv.put("direct", if (v) "1" else "0")
    var theme: String? get() = kv.get("theme"); set(v) = kv.put("theme", v)
    var accent: String? get() = kv.get("accent"); set(v) = kv.put("accent", v)
}

/** The phone's preferences file «telefono» (only settings and tokens: the calls stay in Android's own log). */
class SharedValues(context: android.content.Context) : KeyValues {
    private val prefs = context.applicationContext.getSharedPreferences("telefono", android.content.Context.MODE_PRIVATE)
    override fun get(key: String): String? = prefs.getString(key, null)
    override fun put(key: String, value: String?) {
        prefs.edit(commit = true) { if (value == null) remove(key) else putString(key, value) }
    }
}
