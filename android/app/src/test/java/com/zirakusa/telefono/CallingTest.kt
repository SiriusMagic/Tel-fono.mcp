package com.zirakusa.telefono

import com.zirakusa.telefono.data.Announce
import com.zirakusa.telefono.data.AnnounceMode
import com.zirakusa.telefono.data.AnnounceWords
import com.zirakusa.telefono.data.Choice
import com.zirakusa.telefono.data.Filter
import com.zirakusa.telefono.data.FilterSettings
import com.zirakusa.telefono.data.LongPress
import com.zirakusa.telefono.data.MapValues
import com.zirakusa.telefono.data.MemoryBook
import com.zirakusa.telefono.data.Settings
import com.zirakusa.telefono.data.Sim
import com.zirakusa.telefono.data.SimChoice
import com.zirakusa.telefono.data.Speed
import com.zirakusa.telefono.data.SpeedDial
import com.zirakusa.telefono.data.Unknown
import com.zirakusa.telefono.data.Verdict
import com.zirakusa.telefono.ui.AppActions
import com.zirakusa.telefono.ui.Page
import com.zirakusa.telefono.ui.TelefonoState
import com.zirakusa.telefono.ui.longPress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/** The call filter, the voice announcement and «Marcación rápida»: pure rules, settings and the state. */
class CallingTest {
    private val words = AnnounceWords("Llamada de", "número privado", "más")

    @Test fun filterDecides() {
        val off = FilterSettings()
        assertEquals(Verdict.ALLOW, Filter.decide("0991234567", false, true, off))
        assertEquals(Verdict.ALLOW, Filter.decide("0980000000", false, false, off))                     // factory: strangers ring
        assertEquals(Verdict.ALLOW, Filter.decide(null, true, null, off))                               // hidden rings unless asked
        val hidden = FilterSettings(hidden = true)
        assertEquals(Verdict.REJECT, Filter.decide(null, true, null, hidden))
        assertEquals(Verdict.REJECT, Filter.decide("", false, false, hidden))                            // no number = private
        assertEquals(Verdict.REJECT, Filter.decide("0991234567", true, true, hidden))                    // restricted presentation
        assertEquals(Verdict.ALLOW, Filter.decide("0991234567", false, true, hidden))
        val strangers = FilterSettings(unknown = Unknown.BLOCK)
        assertEquals(Verdict.REJECT, Filter.decide("0980000000", false, false, strangers))
        assertEquals(Verdict.ALLOW, Filter.decide("0991234567", false, true, strangers))
        assertEquals(Verdict.ALLOW, Filter.decide("0980000000", false, null, strangers))                 // contacts unreadable: never block everyone
        assertEquals(Verdict.ALLOW, Filter.decide(null, true, false, strangers))                         // hidden follows its own switch
        assertEquals(Verdict.SILENCE, Filter.decide("0980000000", false, false, FilterSettings(unknown = Unknown.SILENCE)))
        val list = FilterSettings(blocked = listOf("+593 99 123 4567"))
        assertEquals(Verdict.REJECT, Filter.decide("0991234567", false, true, list))                     // blocked wins over contact; 0… = +593…
        assertEquals(Verdict.REJECT, Filter.decide("593991234567", false, null, list))
        assertEquals(Verdict.ALLOW, Filter.decide("0991234568", false, false, list))
    }

    @Test fun blockedList() {
        var l = Filter.block(emptyList(), "099 123 4567")
        assertEquals(listOf("0991234567"), l)
        l = Filter.block(l, "+593991234567"); assertEquals(listOf("+593991234567"), l)                   // same number once
        l = Filter.block(l, "022345678"); assertEquals(listOf("022345678", "+593991234567"), l)
        assertEquals(l, Filter.block(l, "*611#")); assertEquals(l, Filter.block(l, "")); assertFalse(Filter.canBlock("abc"))
        assertTrue(Filter.isBlocked(l, "0991234567")); assertFalse(Filter.isBlocked(l, "")); assertFalse(Filter.isBlocked(l, "0987654321"))
        assertEquals(listOf("022345678"), Filter.unblock(l, "099 123 4567"))
        assertEquals(l, Filter.decode(Filter.encode(l))); assertEquals(emptyList<String>(), Filter.decode(null))
        assertEquals(listOf("0991234567"), Filter.decode("0991234567\n\n*611#\n  "))
    }

    @Test fun announcement() {
        assertFalse(Announce.should(AnnounceMode.NEVER, true)); assertTrue(Announce.should(AnnounceMode.ALWAYS, false))
        assertTrue(Announce.should(AnnounceMode.HEADPHONES, true)); assertFalse(Announce.should(AnnounceMode.HEADPHONES, false))
        assertEquals("Llamada de Ana Pérez", Announce.text(" Ana Pérez ", "0991234567", false, words))
        assertEquals("Llamada de 0 9 9, 1 2 3, 4 5 6 7", Announce.text(null, "0991234567", false, words))
        assertEquals("Llamada de más 5 9 3, 9 9, 1 2 3, 4 5 6 7", Announce.text("", "+593991234567", false, words))
        assertEquals("Llamada de 0 2, 2 3 4, 5 6 7 8", Announce.text(null, "022345678", false, words))
        assertEquals("Llamada de número privado", Announce.text(null, "", false, words))
        assertEquals("Llamada de número privado", Announce.text(null, "0991234567", true, words))
        assertEquals("1 2 3, 4 5 6 7", Announce.spoken("1234567", "más"))                                    // a lone last digit joins
        assertEquals("1 7 1", Announce.spoken("171", "más")); assertEquals("más 3 4 6, 0 0 1, 1 1 2, 2 2", Announce.spoken("+34600111222", "más"))
    }

    @Test fun speedDial() {
        var l = SpeedDial.set(emptyList(), '5', "099 123 4567", "Ana Pérez")
        l = SpeedDial.set(l, '2', "022345678")
        assertEquals(listOf(Speed('2', "022345678"), Speed('5', "0991234567", "Ana Pérez")), l)
        assertEquals(l, SpeedDial.set(l, '1', "0991234567")); assertEquals(l, SpeedDial.set(l, '0', "0991234567"))   // only 2–9
        assertEquals(l, SpeedDial.set(l, '3', "*611#")); assertEquals(l, SpeedDial.set(l, '3', ""))
        assertEquals(listOf(Speed('2', "0987654321"), l[1]), SpeedDial.set(l, '2', "0987654321"))               // replaces
        assertEquals(listOf(l[1]), SpeedDial.remove(l, '2'))
        assertEquals(l, SpeedDial.decode(SpeedDial.encode(l)))
        assertEquals(listOf(Speed('3', "0991", "A B")), SpeedDial.decode(SpeedDial.encode(listOf(Speed('3', "0991", "A\tB"))) + "\nx\t1\n9\t*1#\n3\t0222"))
        assertEquals(LongPress.Call("0991234567"), SpeedDial.longPress("", '5', l, null))
        assertEquals(LongPress.Assign('7'), SpeedDial.longPress("", '7', l, null))
        assertEquals(LongPress.Type, SpeedDial.longPress("09", '5', l, null))                                    // typing: just a digit
        assertEquals(LongPress.Call("*100"), SpeedDial.longPress("", '1', l, "*100"))
        assertEquals(LongPress.Type, SpeedDial.longPress("", '1', l, null)); assertEquals(LongPress.Type, SpeedDial.longPress("", '1', l, ""))
        assertEquals(LongPress.Type, SpeedDial.longPress("", '*', l, null))
    }

    @Test fun speedChoices() {
        assertEquals(listOf(Choice("Álvaro Núñez", "+593 98 765 4321"), Choice("Ana Pérez", "0991234567"), Choice("Pizzería Ñaño", "022345678")),
            SpeedDial.choices(Book.people, ""))
        assertEquals(listOf(Choice("Álvaro Núñez", "+593 98 765 4321")), SpeedDial.choices(Book.people, "nunez"))   // without accents
        assertEquals(listOf(Choice("Ana Pérez", "0991234567")), SpeedDial.choices(Book.people, "0991234567"))        // a contact's: not twice
        assertEquals(listOf(Choice("", "0980000000")), SpeedDial.choices(Book.people, "098 000 0000"))
        assertEquals(listOf(Choice("", "123"), Choice("Ana Pérez", "0991234567")), SpeedDial.choices(Book.people, "123"))
        assertTrue(SpeedDial.choices(Book.people, "*611#").isEmpty())
    }

    @Test fun settingsRemember() {
        val kv = MapValues(); val s = Settings(kv)
        assertEquals(FilterSettings(), s.filter); assertEquals(AnnounceMode.NEVER, s.announce); assertTrue(s.speed.isEmpty())
        s.blockHidden = true; s.unknown = Unknown.SILENCE; s.blocked = listOf("0991234567"); s.announce = AnnounceMode.HEADPHONES
        s.speed = listOf(Speed('2', "0991234567", "Ana"))
        val again = Settings(kv)
        assertEquals(FilterSettings(true, Unknown.SILENCE, listOf("0991234567")), again.filter)
        assertEquals(AnnounceMode.HEADPHONES, again.announce); assertEquals(listOf(Speed('2', "0991234567", "Ana")), again.speed)
        kv.put("unknown", "NADA"); kv.put("announce", "X"); assertEquals(Unknown.RING, again.unknown); assertEquals(AnnounceMode.NEVER, again.announce)
        assertEquals("Claro EC", Sim(1, "x", 1, "Claro", "Claro EC").title); assertEquals("Claro", Sim(1, "x", 1, "Claro").title)
    }

    @Test fun stateBlocksAndSpeedDials() {
        val kv = MapValues()
        val s = TelefonoState(kv, MemoryBook(Book.calls, Book.people, Book.sims.take(1), voicemailNumber = "*100"), ZoneOffset.UTC) { LocalDate.of(2026, 11, 21) }
        s.load()
        val called = mutableListOf<Pair<String, Int?>>(); val tones = mutableListOf<Char>()
        val actions = AppActions(call = { n, sim -> called += n to sim?.slot }, tone = { tones += it })
        s.block("+593991234567"); assertTrue(s.isBlocked("0991234567")); assertEquals(listOf("+593991234567"), Settings(kv).blocked)
        s.unblock("0991234567"); assertFalse(s.isBlocked("0991234567")); assertTrue(Settings(kv).blocked.isEmpty())
        s.changeBlockHidden(true); s.changeUnknown(Unknown.BLOCK); s.changeAnnounce(AnnounceMode.ALWAYS)
        assertEquals(FilterSettings(true, Unknown.BLOCK), Settings(kv).filter); assertEquals(AnnounceMode.ALWAYS, Settings(kv).announce)
        assertEquals("*100", s.voicemail)
        s.longPress('1', actions); assertEquals(listOf("*100" to 1), called)                                    // voicemail, with the only SIM
        s.longPress('4', actions); assertEquals(Page.SPEED, s.page); assertEquals('4', s.assigning)             // nothing on 4: assign it
        s.setSpeed('4', "0991234567", "Ana Pérez"); assertNull(s.assigning); assertEquals(listOf(Speed('4', "0991234567", "Ana Pérez")), Settings(kv).speed)
        s.page = Page.MAIN; s.longPress('4', actions); assertEquals("0991234567" to 1, called.last()); assertEquals("", s.typed)
        s.press('0'); s.longPress('4', actions); assertEquals("04", s.typed); assertEquals(listOf('4'), tones)  // while typing: a digit
        s.clearTyped(); s.longPress('0', actions); assertEquals("+", s.typed)
        s.clearSpeed('4'); assertTrue(Settings(kv).speed.isEmpty())
        val two = TelefonoState(MapValues(), MemoryBook(Book.calls, Book.people, Book.sims), ZoneOffset.UTC).apply { load(); setSpeed('2', "022345678") }
        two.longPress('2', actions); assertEquals("022345678", two.asking)                                        // two SIMs, no default: ask, like «Llamar»
        two.changeSim(SimChoice.SIM2); two.asking = null; two.longPress('2', actions); assertEquals("022345678" to 2, called.last())
    }
}
