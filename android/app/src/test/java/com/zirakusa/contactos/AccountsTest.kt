package com.zirakusa.contactos

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.zirakusa.contactos.data.Account
import com.zirakusa.contactos.data.AccountKind
import com.zirakusa.contactos.data.Accounts
import com.zirakusa.contactos.data.Badge
import com.zirakusa.contactos.data.Contact
import com.zirakusa.contactos.data.Contacts
import com.zirakusa.contactos.data.Links
import com.zirakusa.contactos.data.MapValues
import com.zirakusa.contactos.data.MemoryStore
import com.zirakusa.contactos.data.NameFormat
import com.zirakusa.contactos.data.Phone
import com.zirakusa.contactos.data.Qr
import com.zirakusa.contactos.data.Raw
import com.zirakusa.contactos.data.Transfer
import com.zirakusa.contactos.data.VCard
import com.zirakusa.contactos.ui.ContactosState
import com.zirakusa.contactos.ui.Page
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** 0.2.0: accounts and badges, joining and separating, hiding duplicates, the letter bar, name format, .vcf in and out, QR. */
class AccountsTest {
    private val google = Account("com.google", "ana@gmail.com")
    private val sim2 = Account("SIM Account", "SIM2")
    private val local = Account("Local Phone Account", "Phone")

    @Test fun accountKindsBadgesAndChoices() {
        assertEquals(AccountKind.PHONE, Account.PHONE.kind); assertEquals(AccountKind.PHONE, local.kind)
        assertEquals(AccountKind.SIM, sim2.kind); assertEquals(2, sim2.slot); assertEquals(AccountKind.SIM, Account("com.android.sim", "Movistar").kind)
        assertEquals(AccountKind.GOOGLE, google.kind); assertEquals(AccountKind.MESSENGER, Account("com.whatsapp", "WhatsApp").kind)
        assertEquals(AccountKind.MESSENGER, Account("com.google.android.apps.tachyon", "x").kind)
        assertEquals(Badge(AccountKind.SIM, 2), Accounts.badge(listOf(Raw(1, google), Raw(2, sim2))))
        assertEquals(Badge(AccountKind.GOOGLE), Accounts.badge(listOf(Raw(1, google))))
        assertNull(Accounts.badge(listOf(Raw(1, local)))); assertNull(Accounts.badge(emptyList()))
        // Each once, messengers out, the phone first; a phone with its own local account drops the account-less one.
        assertEquals(listOf(Account.PHONE, sim2, google), Accounts.choices(listOf(google, sim2, Account("com.whatsapp", "w"), google, Account(null, null))))
        assertEquals(listOf(local, google), Accounts.choices(listOf(google, Account(), local)))
        assertEquals(Account.PHONE, Account.of("")); assertEquals(google, Account.of(google.key)); assertNull(Account.of(null))
        val people = listOf(Contact(1, "A", raws = listOf(Raw(1, google))), Contact(2, "B", raws = listOf(Raw(2, sim2))), Contact(3, "C"))
        assertEquals(listOf(1L), Accounts.filter(people, google).map { it.id }); assertEquals(listOf(3L), Accounts.filter(people, Account.PHONE).map { it.id })
        assertEquals(3, Accounts.filter(people, null).size)
    }

    @Test fun linksMergeAndHiding() {
        assertEquals(listOf(10L to 11L, 10L to 20L), Links.together(listOf(listOf(10L, 11L), listOf(20L))))
        assertEquals(listOf(1L to 2L, 1L to 3L, 2L to 3L), Links.apart(listOf(1L, 2L, 3L))); assertTrue(Links.apart(listOf(1L)).isEmpty())
        val merged = Contacts.merge(listOf(People.ana.copy(raws = listOf(Raw(1))), People.ana2.copy(raws = listOf(Raw(4, google)))))
        assertEquals("Ana Pérez", merged.name); assertEquals(1, merged.phones.size); assertEquals(2, merged.raws.size); assertTrue(merged.starred)
        val twin = People.ana.copy(id = 9, phones = listOf(Phone("+593 99 123 4567")), starred = false, emails = emptyList())
        assertEquals(People.all.map { it.id }, Contacts.hideDuplicates(People.all + twin).map { it.id })      // the copy goes, Anita P. stays
        assertEquals(listOf('C'), listOf(Contacts.jump(listOf('A', 'C', 'Ñ', '#'), 'B')))
        assertEquals('#', Contacts.jump(listOf('A', '#'), 'Z')); assertEquals('Ñ', Contacts.jump(listOf('A', 'Ñ'), 'N')); assertNull(Contacts.jump(emptyList(), 'A'))
        assertEquals("Pérez, Ana", Contacts.display(People.ana, NameFormat.LAST)); assertEquals("Ana Pérez", Contacts.display(People.ana, NameFormat.FIRST))
        assertEquals("López Mora, Juan", Contacts.display(Contact(1, "Juan López Mora", altName = "López Mora, Juan"), NameFormat.LAST))
    }

    @Test fun vcardFilesAndQr() {
        val android21 = "BEGIN:VCARD\r\nVERSION:2.1\r\nN;CHARSET=UTF-8;ENCODING=QUOTED-PRINTABLE:=50=C3=A9rez;Mar=C3=\r\n=ADa;;;\r\n" +
            "FN;CHARSET=UTF-8;ENCODING=QUOTED-PRINTABLE:Mar=C3=ADa P=C3=A9rez\r\nTEL;CELL:0987654321\r\nEND:VCARD\r\nBEGIN:VCARD\r\nVERSION:3.0\r\n" +
            "FN:Luis\r\nitem1.TEL;type=CELL:099 111 2222\r\nEND:VCARD\r\n"
        val cards = VCard.parseAll(android21)
        assertEquals(listOf("María Pérez", "Luis"), cards.map { it.name }); assertEquals("099 111 2222", cards[1].phones.single().number)
        assertEquals(2, VCard.parseAll(VCard.buildAll(People.all.take(2))).size)
        assertEquals("Ana Pérez\n099 123 4567\nana@correo.ec", VCard.plain(People.ana))
        val store = MemoryStore(People.all)
        val text = android21 + VCard.build(People.ana)                                             // Ana is already there
        assertEquals(Transfer.Result(2, 1), Transfer.import(store, text, google))
        assertEquals(google, store.list().first { it.name == "Luis" }.raws.single().account); assertEquals(8, store.list().size)
        assertEquals(Transfer.Result(0, 3), Transfer.import(store, text, google))                  // twice adds nothing
        assertTrue(Transfer.export(People.all).startsWith("BEGIN:VCARD"))
        // The card's QR code reads back as the vCard.
        val grid = Qr.card(People.ana)!!
        val px = IntArray(grid.size * grid.size * 16).also { a -> for (i in a.indices) { val x = i % (grid.size * 4) / 4; val y = i / (grid.size * 4) / 4; a[i] = if (grid[x, y]) -0x1000000 else -1 } }
        val read = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(grid.size * 4, grid.size * 4, px)))).text
        assertEquals(VCard.parse(VCard.forQr(People.ana))!!.name, VCard.parse(read)!!.name); assertTrue("TEL;TYPE=CELL:0991234567" in read)
        assertNull(Qr.encode("x".repeat(5000)))
    }

    @Test fun stateJoinsSeparatesEditsPartsAndSettings() {
        val split = Contact(10, "Rosa Mora", listOf(Phone("0981112222")), raws = listOf(Raw(100, google), Raw(101, sim2)))
        val store = MemoryStore(People.all + split, listOf(Account.PHONE, google, sim2), me = Contact(99, "Yo", listOf(Phone("0990000000"))))
        val kv = MapValues()
        val state = ContactosState(kv, store) { LocalDate.of(2026, 11, 21) }
        state.load()
        assertEquals("Yo", state.profile!!.name); assertTrue(state.isProfile(state.profile!!)); assertEquals(3, state.accounts.size)
        // Banner until closed; back only with new duplicates.
        assertTrue(state.dupesBanner); state.closeBanner(); assertFalse(state.dupesBanner)
        assertFalse(ContactosState(kv, store).apply { load() }.dupesBanner)
        // «Combinar» joins Ana and Anita (nothing deleted), «Desvincular» separates them again.
        state.merge(state.duplicates.first())
        assertEquals(6, state.items.size); assertEquals(2, state.items.first { it.id == 1L }.raws.size)
        state.unlink(state.items.first { it.id == 1L }); assertEquals(7, state.items.size)
        // A person in two accounts asks which part to edit; only that part changes.
        val rosa = state.items.first { it.id == 10L }
        state.edit(rosa); assertEquals(Page.PICK, state.page); assertEquals(2, state.parts(rosa).size)
        state.editPart(rosa, 101); assertEquals(sim2, state.draft.account)
        state.draft = state.draft.copy(name = "Rosa M."); state.save()
        assertEquals("Rosa M.", store.part(101)!!.name); assertEquals("Rosa Mora", store.part(100)!!.name)
        // New contacts go to «Almacenamiento predeterminado»; settings are kept.
        state.changeStorage(google); state.page = Page.LIST; state.edit()
        assertEquals(google, state.draft.account); state.draft = state.draft.copy(name = "Nuevo"); state.save()
        assertEquals(google, state.shown!!.raws.single().account)
        state.changeFormat(NameFormat.LAST); state.changeHideDuplicates(true); state.changeShowOnly(google)
        assertEquals(setOf("Nuevo", "Mora, Rosa"), state.visible.map(state::name).toSet())
        val again = ContactosState(kv, store).apply { load() }
        assertEquals(NameFormat.LAST, again.format); assertTrue(again.hideDuplicates); assertEquals(google, again.storage); assertEquals(google, again.showOnly)
        state.setRingtone(rosa, "content://media/1"); assertEquals("content://media/1", store.list().first { it.id == 10L }.ringtone)
    }
}
