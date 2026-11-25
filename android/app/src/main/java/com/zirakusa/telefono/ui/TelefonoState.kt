package com.zirakusa.telefono.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zirakusa.telefono.data.AnnounceMode
import com.zirakusa.telefono.data.Call
import com.zirakusa.telefono.data.CallType
import com.zirakusa.telefono.data.Dial
import com.zirakusa.telefono.data.Filter
import com.zirakusa.telefono.data.KeyValues
import com.zirakusa.telefono.data.LongPress
import com.zirakusa.telefono.data.NoAccess
import com.zirakusa.telefono.data.Numbers
import com.zirakusa.telefono.data.Person
import com.zirakusa.telefono.data.PhoneBook
import com.zirakusa.telefono.data.Settings
import com.zirakusa.telefono.data.Sim
import com.zirakusa.telefono.data.SimChoice
import com.zirakusa.telefono.data.Sims
import com.zirakusa.telefono.data.SpeedDial
import com.zirakusa.telefono.data.Unknown
import com.zirakusa.telefono.ui.theme.Accent
import com.zirakusa.telefono.ui.theme.ThemeMode
import java.time.LocalDate
import java.time.ZoneId

enum class Tab { RECENTS, KEYPAD, FAVORITES }
enum class Page { MAIN, SETTINGS, DETAIL, FILTER, BLOCKED, SPEED, CARRIER }

/** The call screening role of Android («Filtro de llamadas»): not on this Android (before 10), not granted, or granted. */
enum class Role { UNSUPPORTED, OFF, ON }

/** The only owner of Teléfono's state: calls, contacts, what is typed on the keypad, the open line and the settings. */
class TelefonoState(kv: KeyValues, private val book: PhoneBook, val zone: ZoneId = ZoneId.systemDefault(),
    val now: () -> Long = System::currentTimeMillis, private val today: () -> LocalDate = { LocalDate.now(zone) }) {
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
    /** The number whose page (ⓘ) is open. */
    var detail by mutableStateOf<String?>(null); private set
    /** A number waiting for the user to pick SIM 1 or SIM 2. */
    var asking by mutableStateOf<String?>(null)
    var sims by mutableStateOf(emptyList<Sim>()); private set
    /** WhatsApp is installed (the activity tells); its buttons only show then. */
    var whatsapp by mutableStateOf(false)

    var tones by mutableStateOf(settings.tones); private set
    var direct by mutableStateOf(settings.direct); private set
    var defaultSim by mutableStateOf(settings.sim); private set
    var themeMode by mutableStateOf(settings.theme?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.BLACK); private set
    var accent by mutableStateOf(settings.accent?.let { runCatching { Accent.valueOf(it) }.getOrNull() } ?: Accent.VERDE); private set
    /** Whether Teléfono holds the call screening role (the activity tells, on every return to the app). */
    var role by mutableStateOf(Role.UNSUPPORTED)
    var blockHidden by mutableStateOf(settings.blockHidden); private set
    var unknown by mutableStateOf(settings.unknown); private set
    var blocked by mutableStateOf(settings.blocked); private set
    var announce by mutableStateOf(settings.announce); private set
    var speed by mutableStateOf(settings.speed); private set
    /** The carrier's voicemail number (key 1), when it has one. */
    var voicemail by mutableStateOf<String?>(null); private set
    /** The key of «Marcación rápida» being assigned (its picker is open). */
    var assigning by mutableStateOf<Char?>(null)

    val groups get() = Dial.group(if (onlyMissed) calls.filter { it.type == CallType.MISSED } else calls, zone)
    val missedToday get() = Dial.missed(calls, today(), zone).size
    val favorites get() = people.filter { it.starred }.sortedBy { it.name }
    /** Contacts that match the keypad (T9 or digits). */
    val suggestions get() = Dial.search(people, typed).take(4)
    /** The name for a number when it is saved. */
    fun nameOf(c: Call) = c.name.ifBlank { Numbers.nameOf(people, c.number).orEmpty() }

    /** The SIM a call goes out with, without asking; null = ask (two SIMs, no default) or let Android choose. */
    val chosenSim get() = Sims.pick(defaultSim, sims)
    val mustAsk get() = Sims.mustAsk(defaultSim, sims)
    fun slotOf(c: Call) = Sims.slotOf(c.account, sims)
    /** The calls with the open number, newest first. */
    val history get() = detail?.let { Dial.history(calls, it) }.orEmpty()
    val detailPerson get() = detail?.let { Numbers.personOf(people, it) }

    fun load() {
        people = book.people()
        sims = book.sims()
        voicemail = book.voicemail()
        try { calls = book.calls(); allowed = true } catch (_: NoAccess) { calls = emptyList(); allowed = false }
    }

    fun press(key: Char) { typed = Dial.clean(typed + key) }
    fun backspace() { typed = typed.dropLast(1) }
    fun clearTyped() { typed = "" }
    /** Long press on 0 writes «+», for international numbers. */
    fun plus() { if (typed.isEmpty()) typed = "+" }
    fun toggle(id: Long) { open = if (open == id) null else id }
    fun openDetail(number: String) { detail = number; page = Page.DETAIL }
    fun closeDetail() { page = Page.MAIN; detail = null }

    fun changeTones(on: Boolean) { tones = on; settings.tones = on }
    fun changeDirect(on: Boolean) { direct = on; settings.direct = on }
    fun changeSim(c: SimChoice) { defaultSim = c; settings.sim = c }
    fun isBlocked(number: String) = Filter.isBlocked(blocked, number)
    fun block(number: String) { blocked = Filter.block(blocked, number); settings.blocked = blocked }
    fun unblock(number: String) { blocked = Filter.unblock(blocked, number); settings.blocked = blocked }
    fun changeBlockHidden(on: Boolean) { blockHidden = on; settings.blockHidden = on }
    fun changeUnknown(u: Unknown) { unknown = u; settings.unknown = u }
    fun changeAnnounce(m: AnnounceMode) { announce = m; settings.announce = m }
    fun setSpeed(key: Char, number: String, name: String = "") { speed = SpeedDial.set(speed, key, number, name); settings.speed = speed; assigning = null }
    fun clearSpeed(key: Char) { speed = SpeedDial.remove(speed, key); settings.speed = speed }

    /** A long press on a key: «Marcación rápida» (2–9), the voicemail (1), «+» (0); otherwise it types the key. */
    fun longPress(key: Char): LongPress {
        if (key == '0') { if (typed.isEmpty()) plus() else press(key); return LongPress.Type }
        val what = SpeedDial.longPress(typed, key, speed, voicemail)
        when (what) {
            LongPress.Type -> press(key)
            is LongPress.Assign -> { page = Page.SPEED; assigning = what.key }
            is LongPress.Call -> Unit
        }
        return what
    }

    fun changeTheme(m: ThemeMode) { themeMode = m; settings.theme = m.name }
    fun changeAccent(a: Accent) { accent = a; settings.accent = a.name }
}
