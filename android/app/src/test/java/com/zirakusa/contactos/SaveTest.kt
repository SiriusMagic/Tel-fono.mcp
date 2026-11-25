package com.zirakusa.contactos

import com.zirakusa.contactos.data.Contact
import com.zirakusa.contactos.data.ContactStore
import com.zirakusa.contactos.data.Contacts
import com.zirakusa.contactos.data.MapValues
import com.zirakusa.contactos.data.MemoryStore
import com.zirakusa.contactos.data.Order
import com.zirakusa.contactos.data.Phone
import com.zirakusa.contactos.data.SaveFailed
import com.zirakusa.contactos.data.WidgetPeople
import com.zirakusa.contactos.ui.ContactosState
import com.zirakusa.contactos.ui.Page
import com.zirakusa.contactos.ui.SaveError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** «Nuevo contacto» always saves or says why (it never closes silently), and what the widget shows. */
class SaveTest {
    private val today = LocalDate.of(2026, 11, 21)

    @Test fun newContactWithoutPermissionAsksAndThenSaves() {
        val store = MemoryStore(People.all)
        val state = ContactosState(MapValues(), store) { today }.apply { load() }
        state.newContact("099 777 8888", "Rosa Vera")
        assertEquals(Page.EDIT, state.page); assertEquals("Rosa Vera", state.draft.name); assertEquals("0997778888", state.draft.phones.single())
        store.allowed = false                                   // reading was allowed by Teléfono, changing wasn't
        assertNull(state.save())
        assertEquals(SaveError.PERMISSION, state.saveError); assertEquals(Page.EDIT, state.page); assertEquals("Rosa Vera", state.draft.name)
        store.allowed = true; state.allowedNow()                // «Permitir»: saved with what was typed
        assertNull(state.saveError); assertEquals(Page.DETAIL, state.page); assertEquals("Rosa Vera", state.shown?.name)
        assertEquals(7, state.items.size)
    }

    @Test fun refusedAccountSaysSoAndKeepsTheDraft() {
        val memory = MemoryStore(People.all)
        val store = object : ContactStore by memory { override fun save(c: Contact): Contact = throw SaveFailed() }
        val state = ContactosState(MapValues(), store) { today }.apply { load() }
        state.newContact("0991112222")
        assertNull(state.save()); assertEquals(SaveError.FAILED, state.saveError); assertEquals(Page.EDIT, state.page)
        state.cancelEdit(); assertNull(state.saveError); assertEquals(Page.LIST, state.page)
        state.newContact(null); assertEquals("", state.draft.name); assertEquals(listOf(""), state.draft.phones)
    }

    @Test fun widgetPeople() {
        val people = People.all.map { it.copy(starred = it.id in setOf(1L, 2L, 3L, 5L, 6L)) } + Contact(9, "Zoe", starred = true)
        val picked = WidgetPeople.pick(people)
        assertEquals(WidgetPeople.MAX, picked.size)
        assertEquals(listOf("Álvaro Núñez", "Ana Pérez", "Carlos Mora", "Ñusta Quispe"), picked.map { it.name })   // same order as the list
        assertEquals(Contacts.sorted(people.filter { it.starred }, Order.LAST).take(2), WidgetPeople.pick(people, Order.LAST, 2))
        assertEquals("Ana", WidgetPeople.short(People.ana)); assertEquals("0991234567", WidgetPeople.short(Contact(phones = listOf(Phone("0991234567")))))
        assertEquals("0991234567", WidgetPeople.number(People.ana)); assertNull(WidgetPeople.number(People.carlos))
        assertEquals(emptyList<Contact>(), WidgetPeople.pick(People.all.map { it.copy(starred = false) }))
    }
}
