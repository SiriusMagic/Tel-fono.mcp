package com.zirakusa.contactos.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zirakusa.contactos.data.Account
import com.zirakusa.contactos.data.Accounts
import com.zirakusa.contactos.data.Birthday
import com.zirakusa.contactos.data.Contact
import com.zirakusa.contactos.data.ContactStore
import com.zirakusa.contactos.data.Contacts
import com.zirakusa.contactos.data.Email
import com.zirakusa.contactos.data.KeyValues
import com.zirakusa.contactos.data.Label
import com.zirakusa.contactos.data.NameFormat
import com.zirakusa.contactos.data.NoAccess
import com.zirakusa.contactos.data.Order
import com.zirakusa.contactos.data.Phone
import com.zirakusa.contactos.data.Phones
import com.zirakusa.contactos.data.Raw
import com.zirakusa.contactos.data.SaveFailed
import com.zirakusa.contactos.data.Settings
import com.zirakusa.contactos.ui.theme.Accent
import com.zirakusa.contactos.ui.theme.ThemeMode
import java.time.LocalDate

/** Why «Guardar» didn't save: Android hasn't let Contactos change contacts yet, or it refused this one. */
enum class SaveError { PERMISSION, FAILED }

/** The list, or a page over it. */
enum class Page { LIST, DETAIL, EDIT, PICK, CARD, DUPLICATES, SETTINGS }

/** What «Editar» holds while typing (phones and emails as lines, the birthday as text). */
data class Draft(val id: Long = 0, val name: String = "", val phones: List<String> = listOf(""), val emails: List<String> = listOf(""),
    val org: String = "", val birthday: String = "", val address: String = "", val note: String = "", val starred: Boolean = false,
    /** The part being edited (0: a new contact, or the contact's first part). */
    val raw: Long = 0, val account: Account = Account.PHONE) {
    fun toContact() = Contact(id, name.trim(), phones.map(Phones::clean).filter { it.any(Char::isDigit) }.map { Phone(it, Label.MOBILE) },
        emails.map { it.trim() }.filter { it.isNotEmpty() }.map { Email(it, Label.HOME) }, org.trim(), note.trim(), Birthday.parse(birthday), address.trim(), starred,
        raws = if (id == 0L) listOf(Raw(0, account)) else listOfNotNull(raw.takeIf { it > 0 }?.let { Raw(it, account) }))
    /** Why it can't be saved yet: no name nor number; a birthday that isn't a date. */
    val problem get() = when {
        name.isBlank() && phones.none { it.any(Char::isDigit) } -> Problem.EMPTY
        birthday.isNotBlank() && Birthday.parse(birthday) == null -> Problem.BIRTHDAY
        else -> null
    }
    enum class Problem { EMPTY, BIRTHDAY }

    companion object {
        fun of(c: Contact) = Draft(c.id, c.name, c.phones.map { it.number }.ifEmpty { listOf("") }, c.emails.map { it.address }.ifEmpty { listOf("") },
            c.org, c.birthday?.iso()?.removePrefix("--").orEmpty(), c.address, c.note, c.starred,
            c.raws.firstOrNull()?.id ?: 0, c.raws.firstOrNull()?.account ?: Account.PHONE)
    }
}

/** The only owner of Contactos' state: the contacts, search, the one open, the one being edited and the settings. Screens only paint. */
class ContactosState(kv: KeyValues, private val store: ContactStore, private val today: () -> LocalDate = LocalDate::now) {
    private val settings = Settings(kv)

    var page by mutableStateOf(Page.LIST)
    /** Where «Contactos repetidos» goes back to: the list (from the banner) or Ajustes. */
    var dupesBack by mutableStateOf(Page.SETTINGS)
    var items by mutableStateOf(emptyList<Contact>()); private set
    /** Android lets Contactos read the contacts. */
    var allowed by mutableStateOf(true); private set
    var query by mutableStateOf("")
    var shown by mutableStateOf<Contact?>(null); private set
    var draft by mutableStateOf(Draft())
    /** The last «Guardar» failed (the editor says why and stays open with what was typed). */
    var saveError by mutableStateOf<SaveError?>(null); private set
    var order by mutableStateOf(settings.order); private set
    var themeMode by mutableStateOf(settings.theme?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.BLACK); private set
    var accent by mutableStateOf(settings.accent?.let { runCatching { Accent.valueOf(it) }.getOrNull() } ?: Accent.ROSA); private set

    var format by mutableStateOf(settings.format); private set
    var hideDuplicates by mutableStateOf(settings.hideDuplicates); private set
    var storage by mutableStateOf(settings.storage ?: Account.PHONE); private set
    var showOnly by mutableStateOf(settings.show); private set
    private var dupesClosed by mutableStateOf(settings.dupesClosed)
    /** «Mi perfil», when the phone has one. */
    var profile by mutableStateOf<Contact?>(null); private set
    /** Where a new contact can be saved («Guardar en»). */
    var accounts by mutableStateOf(listOf(Account.PHONE)); private set

    /** The contacts the list shows: of one account if «Contactos para mostrar» says so, repeated ones once if asked. */
    val visible get() = Accounts.filter(items, showOnly).let { if (hideDuplicates) Contacts.hideDuplicates(it) else it }
    val groups get() = Contacts.groups(visible, order)
    val results get() = Contacts.search(visible, query)
    val favorites get() = Contacts.sorted(visible.filter { it.starred }, order)
    val birthdays get() = Contacts.upcoming(visible, today(), 14)
    val duplicates get() = Contacts.duplicates(items)
    /** «Se encontraron contactos duplicados»: until closed, and again when there are new ones. */
    val dupesBanner get() = duplicates.let { it.isNotEmpty() && Contacts.signature(it) != dupesClosed }
    fun today() = today.invoke()
    fun name(c: Contact) = Contacts.display(c, format)
    fun isProfile(c: Contact) = profile?.id == c.id

    fun load() {
        try {
            items = store.list(); allowed = true
            profile = runCatching { store.profile() }.getOrNull()
            accounts = runCatching { store.accounts() }.getOrNull().orEmpty().ifEmpty { listOf(Account.PHONE) }
        } catch (_: NoAccess) { items = emptyList(); profile = null; allowed = false }
        shown = shown?.let { s -> items.firstOrNull { it.id == s.id } ?: profile?.takeIf { it.id == s.id } }
        if (shown == null && page in setOf(Page.DETAIL, Page.CARD, Page.PICK)) page = Page.LIST
    }

    fun open(c: Contact) { shown = c; page = Page.DETAIL }
    fun card(c: Contact) { shown = c; page = Page.CARD }

    /** «Editar»: a contact in several accounts asks first which part («Selecciona un contacto para editar»). */
    fun edit(c: Contact? = null) {
        if (c == null) {
            draft = Draft(name = query.takeIf { q -> q.none(Char::isDigit) }.orEmpty(),
                phones = listOf(query.takeIf { q -> q.isNotBlank() && q.all { it.isDigit() || it in "+ " } }.orEmpty()),
                account = storage.takeIf { it in accounts } ?: accounts.first())
            saveError = null; page = Page.EDIT
        } else if (c.raws.size > 1) { shown = c; page = Page.PICK } else editPart(c, c.raws.firstOrNull()?.id)
    }

    /** Each part of a contact with its own data, for «Selecciona un contacto para editar». */
    fun parts(c: Contact): List<Pair<Raw, Contact>> = c.raws.map { r -> r to (runCatching { store.part(r.id) }.getOrNull() ?: c) }

    /** Edits only [raw]'s data (one account), so the other parts stay as they are. */
    fun editPart(c: Contact, raw: Long?) {
        val part = raw?.let { r -> runCatching { store.part(r) }.getOrNull() }?.copy(id = c.id, starred = c.starred)
        draft = Draft.of(part ?: c); saveError = null; page = Page.EDIT
    }

    /** «Guardar»: saves and shows it; null while there is a [Draft.problem]. */
    fun save(): Contact? {
        if (draft.problem != null) return null
        val saved = try { store.save(draft.toContact()) } catch (_: NoAccess) { saveError = SaveError.PERMISSION; return null }
            catch (_: SaveFailed) { saveError = SaveError.FAILED; return null }
        saveError = null
        load(); shown = items.firstOrNull { it.id == saved.id } ?: saved; page = Page.DETAIL; query = ""
        return saved
    }

    /**
     * «Nuevo contacto» with a number (and maybe a name) from Teléfono, the widget or another app («Añadir a contactos»).
     * Only fills the editor: nothing is saved until the user taps «Guardar».
     */
    fun newContact(phone: String?, name: String? = null) {
        query = ""; edit()
        draft = draft.copy(name = name?.trim()?.take(Contact.NAME_MAX).orEmpty(), phones = listOf(phone?.let(com.zirakusa.contactos.data.Phones::clean).orEmpty()))
    }

    /** Android answered the permission request: if «Guardar» was waiting for it, try again with what was typed. */
    fun allowedNow() {
        load()
        if (page == Page.EDIT && saveError == SaveError.PERMISSION) save()
    }

    fun cancelEdit() { saveError = null; page = if (draft.id != 0L && shown != null) Page.DETAIL else Page.LIST }
    fun star(c: Contact) { runCatching { store.star(c.id, !c.starred) }; load() }
    /** Only the user deletes, after confirming; the AI never can. */
    fun delete(c: Contact) { runCatching { store.delete(c.id) }; shown = null; page = Page.LIST; load() }

    /** «Combinar»: Android joins them into one; every part stays (nothing is deleted) and «Desvincular» undoes it. */
    fun merge(group: List<Contact>) { runCatching { store.link(group.map { it.id }) }; load() }
    fun unlink(c: Contact) { runCatching { store.unlink(c.id) }; load() }
    fun setRingtone(c: Contact, uri: String?) { runCatching { store.ringtone(c.id, uri) }; load() }
    fun closeBanner() { Contacts.signature(duplicates).let { dupesClosed = it; settings.dupesClosed = it } }

    fun changeOrder(o: Order) { order = o; settings.order = o }
    fun changeFormat(f: NameFormat) { format = f; settings.format = f }
    fun changeHideDuplicates(on: Boolean) { hideDuplicates = on; settings.hideDuplicates = on }
    fun changeStorage(a: Account) { storage = a; settings.storage = a }
    fun changeShowOnly(a: Account?) { showOnly = a; settings.show = a }
    fun changeTheme(mode: ThemeMode) { themeMode = mode; settings.theme = mode.name }
    fun changeAccent(next: Accent) { accent = next; settings.accent = next.name }
}
