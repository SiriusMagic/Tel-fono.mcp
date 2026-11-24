package com.zirakusa.contactos

import com.zirakusa.contactos.data.Birthday
import com.zirakusa.contactos.data.Contact
import com.zirakusa.contactos.data.Contacts
import com.zirakusa.contactos.data.MapValues
import com.zirakusa.contactos.data.MemoryStore
import com.zirakusa.contactos.data.Order
import com.zirakusa.contactos.data.Phones
import com.zirakusa.contactos.data.VCard
import com.zirakusa.contactos.ui.ContactosState
import com.zirakusa.contactos.ui.Draft
import com.zirakusa.contactos.ui.Page
import com.zirakusa.contactos.ui.screens.grow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** The pure rules: order and letters, search, phones, duplicates, birthdays, vCard, the editor and the state. */
class ContactsTest {
    private val today = LocalDate.of(2026, 11, 21)

    @Test fun orderLettersAndSearch() {
        assertEquals(listOf("Álvaro Núñez", "Ana Pérez", "Anita P.", "Carlos Mora", "Ñusta Quispe", "9 de Octubre Pizzería"), Contacts.sorted(People.all).map { it.name })
        assertEquals(listOf('A', 'C', 'Ñ', '#'), Contacts.groups(People.all).map { it.first })
        assertEquals('N', Contacts.letter(People.alvaro, Order.LAST)); assertEquals("Mora Carlos", Contacts.key(People.carlos, Order.LAST))
        assertEquals(listOf(People.ana), Contacts.search(People.all, "ana")); assertEquals(listOf(People.ana, People.ana2), Contacts.search(People.all, "an").filter { it.id in setOf(1L, 4L) })
        assertEquals(listOf(People.alvaro), Contacts.search(People.all, "nunez"))                 // any word, accents ignored
        assertEquals(listOf(People.ana, People.ana2), Contacts.search(People.all, "099 123"))     // 0… and +593… are the same
        assertEquals(listOf(People.ana, People.ana2), Contacts.search(People.all, "+593 99 123 4567"))
        assertEquals(listOf(People.carlos), Contacts.search(People.all, "trabajo.com"))
        assertTrue(Contacts.search(People.all, "  ").isEmpty())
        assertEquals("AP", People.ana.initials); assertEquals("9", People.nine.initials.take(1))
        assertEquals("042000000", Contact(phones = People.nine.phones).shownName)
    }

    @Test fun phones() {
        assertEquals("991234567", Phones.key("099 123 4567")); assertEquals("991234567", Phones.key("+593 99 123 4567"))
        assertEquals("099 123 4567", Phones.pretty("0991234567")); assertEquals("02 234 5678", Phones.pretty("022345678"))
        assertEquals("+593 99 123 4567", Phones.pretty("+593991234567")); assertEquals("*123#", Phones.pretty("*123#"))
        assertEquals("+593991234567", Phones.clean(" +593 (99) 123-4567 ")); assertEquals("*611#", Phones.clean("*611#"))
    }

    @Test fun duplicatesAndBirthdays() {
        val dups = Contacts.duplicates(People.all + Contact(7, "carlos mora"))
        assertEquals(listOf(setOf(1L, 4L), setOf(6L, 7L)), dups.map { g -> g.map { it.id }.toSet() })
        val soon = Contacts.upcoming(People.all, today, 7)
        assertEquals(listOf(People.alvaro, People.ana), soon.map { it.first })                     // today first
        assertEquals(LocalDate.of(2026, 11, 23), soon[1].second); assertEquals(35, People.ana.birthday!!.turns(today))
        assertNull(People.alvaro.birthday!!.turns(today))
        assertEquals(LocalDate.of(2027, 3, 1), Birthday(2, 29).next(today))                       // 29 Feb on a common year
        assertEquals(LocalDate.of(2028, 2, 29), Birthday(2, 29).next(LocalDate.of(2028, 1, 1)))
        assertEquals(Birthday(12, 2, 1990), Birthday.parse("1990-12-02")); assertEquals(Birthday(12, 2), Birthday.parse("--12-02"))
        assertEquals(Birthday(12, 2), Birthday.parse("12-2")); assertEquals(Birthday(12, 2, 1990), Birthday.parse("1990/12/02")); assertNull(Birthday.parse("13-40")); assertNull(Birthday.parse("mañana"))
        assertEquals("--11-21", People.alvaro.birthday!!.iso())
    }

    @Test fun vcard() {
        val text = VCard.build(People.ana.copy(note = "Llamar; tarde\ndespués", address = "Av. Amazonas, Quito"))
        assertTrue(text.contains("FN:Ana Pérez")); assertTrue(text.contains("TEL;TYPE=CELL:0991234567")); assertTrue(text.contains("BDAY:1991-11-23"))
        val back = VCard.parse(text)!!
        assertEquals("Ana Pérez", back.name); assertEquals(People.ana.phones, back.phones); assertEquals("Llamar; tarde\ndespués", back.note)
        assertEquals(People.ana.birthday, back.birthday); assertEquals("Zirakusa", back.org); assertEquals("Av. Amazonas, Quito", back.address)
        assertEquals("Luis Mora", VCard.parse("BEGIN:VCARD\nN:Mora;Luis;;;\nTEL;TYPE=WORK:022\nEND:VCARD")!!.name)
        assertNull(VCard.parse("hola")); assertNull(VCard.parse("BEGIN:VCARD\nEND:VCARD"))
    }

    @Test fun editorAndState() {
        assertEquals(listOf("099", ""), grow(listOf(""), 0, "099")); assertEquals(listOf("", ""), grow(listOf("", ""), 0, ""))
        assertEquals(Draft.Problem.EMPTY, Draft().problem); assertEquals(Draft.Problem.BIRTHDAY, Draft(name = "X", birthday = "ayer").problem)
        val draft = Draft(name = " Luis ", phones = listOf("099 111 2222", ""), emails = listOf(""), birthday = "05-14")
        val c = draft.toContact()
        assertEquals("Luis", c.name); assertEquals("0991112222", c.phones.single().number); assertTrue(c.emails.isEmpty()); assertEquals(Birthday(5, 14), c.birthday)
        assertEquals(Draft.of(People.ana).toContact().copy(photo = null), People.ana.copy(photo = null))

        val store = MemoryStore(People.all)
        val state = ContactosState(MapValues(), store) { today }
        state.load()
        assertEquals(listOf(People.ana.id), state.favorites.map { it.id }); assertEquals(2, state.birthdays.size); assertEquals(1, state.duplicates.size)
        state.query = "0995556666"; state.edit()
        assertEquals(listOf("0995556666"), state.draft.phones)
        state.draft = state.draft.copy(name = "Nuevo")
        val saved = state.save()!!
        assertEquals(Page.DETAIL, state.page); assertEquals(7L, saved.id); assertEquals(7, state.items.size)
        state.star(saved); assertTrue(store.list().first { it.id == 7L }.starred)
        state.delete(state.shown!!); assertEquals(Page.LIST, state.page); assertEquals(6, state.items.size)
        store.allowed = false; state.load(); assertFalse(state.allowed); assertTrue(state.items.isEmpty())
        val lines = SummaryLines.lines(People.all, today) { listOf("Hoy", "Mañana", "En 2 días")[it] }
        assertEquals(listOf("Hoy · Álvaro Núñez", "En 2 días · Ana Pérez (35)"), lines)
    }
}
