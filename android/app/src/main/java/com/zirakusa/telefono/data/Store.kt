package com.zirakusa.telefono.data

import androidx.core.content.edit

/** Where the calls and contacts come from: Android's call log and contacts in the app, lists in the tests. */
interface PhoneBook {
    /** The last calls, newest first; throws [NoAccess] without the permission. */
    fun calls(limit: Int = 500): List<Call>
    /** The contacts with numbers; empty without the permission. */
    fun people(): List<Person>
    /** The SIMs that can call, by slot; empty when unknown (no permission, one SIM-less phone, tests). */
    fun sims(): List<Sim> = emptyList()
    /** The voicemail number of the carrier (long press on 1); null when it has none or Android doesn't say. */
    fun voicemail(): String? = null
}

/** The user hasn't allowed Teléfono to read the call log. */
class NoAccess : Exception()

class MemoryBook(var callList: List<Call> = emptyList(), var peopleList: List<Person> = emptyList(), var simList: List<Sim> = emptyList(),
    var voicemailNumber: String? = null) : PhoneBook {
    var allowed = true
    override fun calls(limit: Int): List<Call> { if (!allowed) throw NoAccess(); return callList.sortedByDescending { it.at }.take(limit) }
    override fun people() = peopleList
    override fun sims() = simList
    override fun voicemail() = voicemailNumber
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
    /** «SIM por defecto» when the phone has two: ask, SIM 1 or SIM 2. */
    var sim: SimChoice get() = kv.get("sim")?.let { runCatching { SimChoice.valueOf(it) }.getOrNull() } ?: SimChoice.ASK; set(v) = kv.put("sim", v.name)
    /** «Filtro de llamadas no deseadas»: reject hidden and private numbers. */
    var blockHidden: Boolean get() = flag("block_hidden", false); set(v) = kv.put("block_hidden", if (v) "1" else "0")
    /** Numbers not in the contacts: ring (factory setting), ring without sound or block. */
    var unknown: Unknown get() = kv.get("unknown")?.let { runCatching { Unknown.valueOf(it) }.getOrNull() } ?: Unknown.RING; set(v) = kv.put("unknown", v.name)
    /** «Bloqueados»: rejected silently, listed in «Recientes» as «Bloqueada». */
    var blocked: List<String> get() = Filter.decode(kv.get("blocked")); set(v) = kv.put("blocked", Filter.encode(v))
    /** Everything the screening service needs, in one read. */
    val filter get() = FilterSettings(blockHidden, unknown, blocked)
    /** «Anuncio de voz de llamada entrante». */
    var announce: AnnounceMode get() = kv.get("announce")?.let { runCatching { AnnounceMode.valueOf(it) }.getOrNull() } ?: AnnounceMode.NEVER; set(v) = kv.put("announce", v.name)
    /** «Marcación rápida»: keys 2–9. */
    var speed: List<Speed> get() = SpeedDial.decode(kv.get("speed")); set(v) = kv.put("speed", SpeedDial.encode(v))
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
