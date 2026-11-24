package com.zirakusa.contactos.data

import androidx.core.content.edit

/** Where contacts live: Android's contacts in the app, a list in memory in the tests. */
interface ContactStore {
    /** Every contact, each with its parts ([Contact.raws]); throws [NoAccess] without the permission. */
    fun list(): List<Contact>
    /**
     * Adds ([Contact.id] 0, into the account of its first [Raw]) or replaces a contact's data in its first [Raw] (the part
     * being edited); returns it with its id.
     */
    fun save(c: Contact): Contact
    fun delete(id: Long): Boolean
    fun star(id: Long, on: Boolean): Boolean
    /** Only the data of one part (one account) of a contact, to edit just that part. */
    fun part(raw: Long): Contact?
    /** The accounts a new contact can be saved in («Guardar en»). */
    fun accounts(): List<Account>
    /** «Mi perfil», when the phone has one. */
    fun profile(): Contact?
    /** «Combinar»: joins the contacts into one (Android keeps every part; nothing is deleted). */
    fun link(ids: List<Long>): Boolean
    /** «Desvincular»: separates a contact into its parts again. */
    fun unlink(id: Long): Boolean
    /** «Tono de llamada»: a sound's address, or null for the phone's default. */
    fun ringtone(id: Long, uri: String?): Boolean
}

/** The user hasn't allowed Contactos to read the contacts. */
class NoAccess : Exception()

/** Contacts in memory, kept by part like Android does: each part has an account and belongs to one contact. */
class MemoryStore(items: List<Contact> = emptyList(), private val found: List<Account> = listOf(Account.PHONE), var me: Contact? = null) : ContactStore {
    private class Part(var contact: Long, var data: Contact)
    private val parts = LinkedHashMap<Long, Part>()
    private var nextRaw = 1000L
    var allowed = true

    init { items.forEach { c -> c.raws.ifEmpty { listOf(Raw(c.id)) }.forEach { r -> parts[r.id] = Part(c.id, c.copy(raws = listOf(r))) } } }

    private fun check() { if (!allowed) throw NoAccess() }
    private fun of(id: Long) = parts.values.filter { it.contact == id }
    private fun nextId() = (parts.values.maxOfOrNull { it.contact } ?: 0) + 1

    override fun list(): List<Contact> { check(); return parts.values.groupBy { it.contact }.map { (id, p) -> Contacts.merge(p.map { it.data }).copy(id = id) } }

    override fun save(c: Contact): Contact {
        check()
        if (c.id == 0L) {
            val id = nextId(); val raw = Raw(nextRaw++, c.raws.firstOrNull()?.account ?: Account.PHONE)
            parts[raw.id] = Part(id, c.copy(id = id, raws = listOf(raw)))
            return list().first { it.id == id }
        }
        val part = c.raws.firstOrNull()?.let { parts[it.id] }?.takeIf { it.contact == c.id } ?: of(c.id).firstOrNull() ?: return c
        part.data = c.copy(raws = part.data.raws)
        return list().first { it.id == c.id }
    }

    override fun delete(id: Long): Boolean { check(); return parts.values.removeAll { it.contact == id } }
    override fun star(id: Long, on: Boolean) = of(id).onEach { it.data = it.data.copy(starred = on) }.isNotEmpty()
    override fun part(raw: Long) = parts[raw]?.let { it.data.copy(id = it.contact) }
    override fun accounts() = Accounts.choices(found)
    override fun profile() = me
    override fun ringtone(id: Long, uri: String?) = of(id).onEach { it.data = it.data.copy(ringtone = uri) }.isNotEmpty()

    override fun link(ids: List<Long>): Boolean {
        val all = ids.distinct().flatMap(::of)
        if (ids.distinct().size < 2 || all.map { it.contact }.distinct().size < 2) return false
        all.forEach { it.contact = ids.first() }
        return true
    }

    override fun unlink(id: Long): Boolean {
        val mine = of(id)
        if (mine.size < 2) return false
        mine.drop(1).forEach { it.contact = nextId() }
        return true
    }
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

/** «Ordenar por»: first name or last name. */
enum class Order { FIRST, LAST }

/** «Formato del nombre»: «Ana Pérez» or «Pérez, Ana». */
enum class NameFormat { FIRST, LAST }

/** Every setting of Contactos, typed. */
class Settings(private val kv: KeyValues) {
    var order: Order get() = kv.get("order")?.let { runCatching { Order.valueOf(it) }.getOrNull() } ?: Order.FIRST; set(v) = kv.put("order", v.name)
    var format: NameFormat get() = kv.get("format")?.let { runCatching { NameFormat.valueOf(it) }.getOrNull() } ?: NameFormat.FIRST; set(v) = kv.put("format", v.name)
    /** «Ocultar los contactos repetidos». */
    var hideDuplicates: Boolean get() = kv.get("hide_dups") == "1"; set(v) = kv.put("hide_dups", if (v) "1" else null)
    /** «Almacenamiento predeterminado»: where new contacts go (null: the phone). */
    var storage: Account? get() = Account.of(kv.get("storage")); set(v) = kv.put("storage", v?.key)
    /** «Contactos para mostrar»: one account, or null for everyone. */
    var show: Account? get() = Account.of(kv.get("show")); set(v) = kv.put("show", v?.key)
    /** The duplicates the user closed the banner for. */
    var dupesClosed: String? get() = kv.get("dupes_closed"); set(v) = kv.put("dupes_closed", v)
    var theme: String? get() = kv.get("theme"); set(v) = kv.put("theme", v)
    var accent: String? get() = kv.get("accent"); set(v) = kv.put("accent", v)
}

/** The phone's preferences file «contactos» (only settings and tokens: the contacts stay in Android's own store). */
class SharedValues(context: android.content.Context) : KeyValues {
    private val prefs = context.applicationContext.getSharedPreferences("contactos", android.content.Context.MODE_PRIVATE)
    override fun get(key: String): String? = prefs.getString(key, null)
    override fun put(key: String, value: String?) {
        prefs.edit(commit = true) { if (value == null) remove(key) else putString(key, value) }
    }
}
