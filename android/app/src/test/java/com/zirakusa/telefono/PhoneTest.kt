package com.zirakusa.telefono

import com.zirakusa.telefono.data.CallType
import com.zirakusa.telefono.data.Dial
import com.zirakusa.telefono.data.MapValues
import com.zirakusa.telefono.data.MemoryBook
import com.zirakusa.telefono.data.Numbers
import com.zirakusa.telefono.ui.TelefonoState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/** The pure rules (groups, missed, T9, durations, codes, numbers) and the keypad and lines of the state. */
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
}
