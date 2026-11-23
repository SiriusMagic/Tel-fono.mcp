package com.zirakusa.telefono

import com.zirakusa.telefono.data.Ago
import com.zirakusa.telefono.data.Call
import com.zirakusa.telefono.data.CallType
import com.zirakusa.telefono.data.Handle
import com.zirakusa.telefono.data.Sim
import com.zirakusa.telefono.data.SimChoice
import com.zirakusa.telefono.data.Sims
import com.zirakusa.telefono.data.SubInfo
import com.zirakusa.telefono.data.When
import com.zirakusa.telefono.data.Dial
import com.zirakusa.telefono.data.MapValues
import com.zirakusa.telefono.data.MemoryBook
import com.zirakusa.telefono.data.Numbers
import com.zirakusa.telefono.ui.AppActions
import com.zirakusa.telefono.ui.Page
import com.zirakusa.telefono.ui.TelefonoState
import com.zirakusa.telefono.ui.dial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** The pure rules (groups, missed, T9, durations, codes, numbers, SIMs, times) and the keypad, lines, SIMs and page of the state. */
class PhoneTest {
    private val today = LocalDate.of(2026, 11, 21)

    @Test fun groupsAndMissed() {
        val g = Dial.group(Book.calls, ZoneOffset.UTC)
        assertEquals(listOf(listOf(2L, 1L), listOf(3L), listOf(4L), listOf(5L)), g.map { gr -> gr.calls.map { it.id } }) // 0… and +593… together; not across days
        assertTrue(g[0].missed); assertEquals(2, g[0].count)
        assertEquals(listOf(2L, 1L), Dial.missed(Book.calls, today, ZoneOffset.UTC).map { it.id })
        assertEquals(3, Dial.missed(Book.calls, today.minusDays(2), ZoneOffset.UTC).size)
        assertEquals(listOf("Ana Pérez (2)"), SummaryLines.lines(Book.calls, today, ZoneOffset.UTC, "Privado"))
        assertEquals(listOf("Privado"), SummaryLines.lines(Book.calls, today.minusDays(2), ZoneOffset.UTC, "Privado").takeLast(1))
    }

    @Test fun keypadLetters() {
        assertEquals('2', Dial.digit('a')); assertEquals('6', Dial.digit('ñ')); assertEquals('2', Dial.digit('Á')); assertEquals('9', Dial.digit('z')); assertNull(Dial.digit('-'))
        assertEquals(listOf("262", "73739"), Dial.t9("Ana Pérez"))
        assertEquals(listOf(Book.ana), Dial.search(Book.people, "262"))                       // A-n-a
        assertEquals(listOf(Book.ana), Dial.search(Book.people, "7373"))                      // any word: P-é-r-e
        assertEquals(listOf(Book.nunez), Dial.search(Book.people, "6863"))                    // N-ú-ñ-e, accents and ñ
        assertEquals(listOf(Book.pizza), Dial.search(Book.people, "6266"))                    // Ñ-a-ñ-o
        assertEquals(listOf(Book.nunez), Dial.search(Book.people, "98765"))                   // by digits too
        assertTrue(Dial.search(Book.people, "").isEmpty())
    }

    @Test fun durationsCodesAndNumbers() {
        assertEquals("0:42", Dial.duration(42)); assertEquals("2:05", Dial.duration(125)); assertEquals("1:02:03", Dial.duration(3723))
        assertTrue(Dial.isCode("*611#")); assertTrue(Dial.isCode("*#06#")); assertFalse(Dial.isCode("0991234567"))
        assertEquals("tel:*611%23", Dial.telUri("*611#")); assertEquals("+593991234567", Dial.clean("+593 99-123 4567+"))
        assertEquals("991234567", Numbers.key("+593 99 123 4567")); assertEquals("099 123 4567", Numbers.pretty("0991234567"))
        assertEquals("Ana Pérez", Numbers.nameOf(Book.people, "+593991234567")); assertNull(Numbers.nameOf(Book.people, "123"))
    }

    @Test fun state() {
        val book = MemoryBook(Book.calls, Book.people)
        val s = TelefonoState(MapValues(), book, ZoneOffset.UTC) { today }
        s.load()
        assertEquals(2, s.missedToday); assertEquals(listOf(Book.ana, Book.pizza), s.favorites); assertEquals(4, s.groups.size)
        s.onlyMissed = true; assertEquals(2, s.groups.size); assertTrue(s.groups.all { it.missed })
        assertEquals("Ana Pérez", s.nameOf(Book.calls[0]))                                     // the log had no name; the contacts do
        s.plus(); "262".forEach(s::press); assertEquals("+262", s.typed); s.backspace(); assertEquals("+26", s.typed)
        s.clearTyped(); "262".forEach(s::press); assertEquals(listOf(Book.ana), s.suggestions)
        s.toggle(2); assertEquals(2L, s.open); s.toggle(2); assertNull(s.open)
        book.allowed = false; s.load(); assertFalse(s.allowed); assertTrue(s.calls.isEmpty()); assertEquals(3, s.people.size)
        assertEquals(CallType.MISSED, Book.calls[0].type)
    }

    @Test fun sims() {
        val claro = Sim(1, "8959301", 1, "Claro"); val movistar = Sim(2, "2", 2, "Movistar")
        assertEquals(listOf(claro, movistar), Book.sims)                                                // by ICCID and by subscription id
        val subs = listOf(SubInfo(1, 7, carrier = "Movistar"), SubInfo(0, 3, carrier = "Claro"))
        assertEquals(listOf(Sim(1, "x", 3, "Claro"), Sim(2, "y", 7, "Movistar")),
            Sims.match(listOf(Handle("y", 7), Handle("x", 3), Handle("sip:ana@voip")), subs))         // Android 11+: subId; SIP left out; by slot
        assertEquals(2, Sims.slotOf("2", Book.sims)); assertEquals(1, Sims.slotOf("8959301", Book.sims)); assertEquals(1, Sims.slotOf("1", Book.sims))
        assertNull(Sims.slotOf("", Book.sims)); assertNull(Sims.slotOf("E", Book.sims)); assertNull(Sims.slotOf("2", emptyList()))
        assertNull(Sims.pick(SimChoice.ASK, Book.sims)); assertEquals(movistar, Sims.pick(SimChoice.SIM2, Book.sims)); assertEquals(claro, Sims.pick(SimChoice.SIM1, Book.sims))
        assertEquals(claro, Sims.pick(SimChoice.SIM2, listOf(claro)))                                  // one SIM: that one, whatever the setting
        assertTrue(Sims.mustAsk(SimChoice.ASK, Book.sims)); assertFalse(Sims.mustAsk(SimChoice.SIM1, Book.sims)); assertFalse(Sims.mustAsk(SimChoice.ASK, listOf(claro)))
    }

    @Test fun times() {
        val now = Book.at(21, 9, 10); val z = ZoneOffset.UTC
        assertEquals(Ago.Now, When.ago(now - 30_000, now, z)); assertEquals(Ago.Minutes(5), When.ago(Book.at(21, 9, 5), now, z))
        assertEquals(Ago.Today(LocalTime.of(7, 12)), When.ago(Book.at(21, 7, 12), now, z)); assertEquals(Ago.Yesterday, When.ago(Book.at(20, 23, 59), now, z))
        assertEquals(Ago.Day(LocalDate.of(2026, 11, 19), true), When.ago(Book.at(19, 7), now, z))
        assertEquals("08:12 a. m.", When.clock(LocalTime.of(8, 12), "a. m.", "p. m.")); assertEquals("12:00 p. m.", When.clock(LocalTime.NOON, "a. m.", "p. m."))
        assertEquals("4:08 p. m.", When.clock(LocalTime.of(16, 8), "a. m.", "p. m.", pad = false)); assertEquals("12:30 a. m.", When.clock(LocalTime.of(0, 30), "a. m.", "p. m."))
        assertEquals("22/11", When.shortDay(LocalDate.of(2026, 11, 22), true)); assertEquals("05/01/25", When.shortDay(LocalDate.of(2025, 1, 5), false))
        val months = "ene,feb,mar,abr,may,jun,jul,ago,sept,oct,nov,dic".split(',')
        assertEquals("12:30", When.stamp(Book.at(21, 12, 30), Book.at(21, 13, 0), z, months, "a. m.", "p. m."))
        assertEquals("20 nov 4:08 p. m.", When.stamp(Book.at(20, 16, 8), now, z, months, "a. m.", "p. m."))
        assertEquals("1 nov 2025 9:00 a. m.", When.stamp(java.time.LocalDateTime.of(2025, 11, 1, 9, 0).toInstant(z).toEpochMilli(), now, z, months, "a. m.", "p. m."))
    }

    @Test fun numberPage() {
        assertEquals("38 s", Dial.spoken(38)); assertEquals("1 m 38 s", Dial.spoken(98)); assertEquals("1 h 0 m 3 s", Dial.spoken(3603)); assertEquals("0 s", Dial.spoken(0))
        assertTrue(Dial.notConnected(Call(9, "099", type = CallType.OUTGOING, at = 0))); assertFalse(Dial.notConnected(Book.calls[2])); assertFalse(Dial.notConnected(Book.calls[0]))
        assertEquals(listOf(2L, 1L, 4L), Dial.history(Book.calls, "+593 99 123 4567").map { it.id }); assertEquals(listOf(5L), Dial.history(Book.calls, "").map { it.id })
        assertEquals("Á", Dial.initial("álvaro")); assertEquals("#", Dial.initial("+593 99")); assertEquals("Ñ", Dial.initial(" ñaño"))
        assertEquals("593991234567", Numbers.wa("099 123 4567")); assertEquals("593991234567", Numbers.wa("+593 99 123 4567")); assertEquals("593991234567", Numbers.wa("991234567"))
        assertEquals("59322345678", Numbers.wa("02 234 5678")); assertEquals("34600111222", Numbers.wa("0034 600 111 222")); assertEquals("34600111222", Numbers.wa("+34600111222"))
        assertNull(Numbers.wa("*611#")); assertNull(Numbers.wa("123")); assertNull(Numbers.wa(""))
        assertEquals(Book.nunez, Numbers.personOf(Book.people, "0987654321")); assertNull(Numbers.personOf(Book.people, "0980000000"))
    }

    @Test fun stateSimsAndPage() {
        val kv = MapValues()
        val s = TelefonoState(kv, MemoryBook(Book.calls, Book.people, Book.sims), ZoneOffset.UTC) { today }
        s.load()
        val called = mutableListOf<Pair<String, Int?>>()
        val actions = AppActions(call = { n, sim -> called += n to sim?.slot })
        assertEquals(2, s.slotOf(Book.calls[1])); assertNull(s.slotOf(Book.calls[0]))
        s.dial("0991234567", actions); assertEquals("0991234567", s.asking); assertTrue(called.isEmpty())   // two SIMs, no default: ask
        s.asking = null; s.changeSim(SimChoice.SIM2); s.dial("0991234567", actions); assertEquals(listOf("0991234567" to 2), called)
        s.dial("*#", actions); assertEquals(1, called.size)
        assertEquals(SimChoice.SIM2, TelefonoState(kv, MemoryBook()).defaultSim)                             // remembered
        s.openDetail("+593991234567"); assertEquals(Page.DETAIL, s.page); assertEquals(Book.ana, s.detailPerson); assertEquals(3, s.history.size)
        s.closeDetail(); assertEquals(Page.MAIN, s.page); assertTrue(s.history.isEmpty())
        val one = TelefonoState(MapValues(), MemoryBook(Book.calls, Book.people, Book.sims.take(1)), ZoneOffset.UTC) { today }.apply { load() }
        one.dial("0991234567", actions); assertEquals("0991234567" to 1, called.last()); assertNull(one.asking)
    }
}
