package com.zirakusa.telefono.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zirakusa.telefono.data.Call
import com.zirakusa.telefono.data.CallType
import com.zirakusa.telefono.data.Dial
import com.zirakusa.telefono.data.KeyValues
import com.zirakusa.telefono.data.NoAccess
import com.zirakusa.telefono.data.Numbers
import com.zirakusa.telefono.data.Person
import com.zirakusa.telefono.data.PhoneBook
import com.zirakusa.telefono.data.Settings
import com.zirakusa.telefono.ui.theme.Accent
import com.zirakusa.telefono.ui.theme.ThemeMode
import java.time.LocalDate
import java.time.ZoneId

enum class Tab { RECENTS, KEYPAD, FAVORITES }
enum class Page { MAIN, SETTINGS, MCP }

/** The only owner of Teléfono's state: calls, contacts, what is typed on the keypad, the open line and the settings. */
class TelefonoState(kv: KeyValues, private val book: PhoneBook, val zone: ZoneId = ZoneId.systemDefault(), private val today: () -> LocalDate = { LocalDate.now(zone) }) {
    private val settings = Settings(kv)

    var tab by mutableStateOf(Tab.RECENTS)
    var page by mutableStateOf(Page.MAIN)
    var calls by mutableStateOf(emptyList<Call>()); private set
    var people by mutableStateOf(emptyList<Person>()); private set
    var allowed by mutableStateOf(true); private set
    var onlyMissed by mutableStateOf(false)
    /** The number typed on the keypad. */
    var typed by mutableStateOf("")
    /** The line of «Recientes» opened (its actions show), by the first call's id. */
    var open by mutableStateOf<Long?>(null)

    var tones by mutableStateOf(settings.tones); private set
    var direct by mutableStateOf(settings.direct); private set
    var themeMode by mutableStateOf(settings.theme?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.BLACK); private set
    var accent by mutableStateOf(settings.accent?.let { runCatching { Accent.valueOf(it) }.getOrNull() } ?: Accent.VERDE); private set

    val groups get() = Dial.group(if (onlyMissed) calls.filter { it.type == CallType.MISSED } else calls, zone)
    val missedToday get() = Dial.missed(calls, today(), zone).size
    val favorites get() = people.filter { it.starred }.sortedBy { it.name }
    /** Contacts that match the keypad (T9 or digits). */
    val suggestions get() = Dial.search(people, typed).take(4)
    /** The name for a number when it is saved. */
    fun nameOf(c: Call) = c.name.ifBlank { Numbers.nameOf(people, c.number).orEmpty() }

    fun load() {
        people = book.people()
        try { calls = book.calls(); allowed = true } catch (_: NoAccess) { calls = emptyList(); allowed = false }
    }

    fun press(key: Char) { typed = Dial.clean(typed + key) }
    fun backspace() { typed = typed.dropLast(1) }
    fun clearTyped() { typed = "" }
    /** Long press on 0 writes «+», for international numbers. */
    fun plus() { if (typed.isEmpty()) typed = "+" }
    fun toggle(id: Long) { open = if (open == id) null else id }

    fun changeTones(on: Boolean) { tones = on; settings.tones = on }
    fun changeDirect(on: Boolean) { direct = on; settings.direct = on }
    fun changeTheme(m: ThemeMode) { themeMode = m; settings.theme = m.name }
    fun changeAccent(a: Accent) { accent = a; settings.accent = a.name }
}
