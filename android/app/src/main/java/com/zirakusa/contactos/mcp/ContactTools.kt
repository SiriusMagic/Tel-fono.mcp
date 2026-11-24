package com.zirakusa.contactos.mcp

import com.zirakusa.telefono.mcp.Access
import com.zirakusa.telefono.mcp.ToolFailure

import com.zirakusa.contactos.data.Account
import com.zirakusa.contactos.data.Birthday
import com.zirakusa.contactos.data.Contact
import com.zirakusa.contactos.data.ContactStore
import com.zirakusa.contactos.data.Contacts
import com.zirakusa.contactos.data.Email
import com.zirakusa.contactos.data.Label
import com.zirakusa.contactos.data.NoAccess
import com.zirakusa.contactos.data.Phone
import com.zirakusa.contactos.data.Phones
import com.zirakusa.contactos.data.Raw
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** What the tools read and change; the app gives Android's contacts, tests a list in memory. */
interface ContactData {
    val store: ContactStore
    fun today(): LocalDate = LocalDate.now()
    /** Where contacts_create saves: «Almacenamiento predeterminado» (the phone when null). */
    fun account(): Account? = null
    /** Called after a tool changed something, so the screen reloads. */
    fun changed() {}
}


/**
 * Contactos' MCP tools: search, read one, favourites and birthdays; with a read-and-write token also create a contact and
 * mark favourites. Deleting and calling are never done by the AI: only the user, on the phone.
 */
object ContactTools {
    /** The contacts screen reloads after a tool changed something. */
    var onChanged: () -> Unit = {}

    class Tool(val name: String, val description: String, val write: Boolean, val schema: JSONObject)

    private fun schema(required: List<String>, vararg props: Pair<String, JSONObject>) = JSONObject()
        .put("type", "object").put("properties", JSONObject().apply { props.forEach { (k, v) -> put(k, v) } })
        .put("required", JSONArray(required)).put("additionalProperties", false)
    private fun prop(type: String, description: String) = JSONObject().put("type", type).put("description", description)

    val tools = listOf(
        Tool("contacts_search", "Finds contacts by name (any word, accents ignored), number (Ecuador's 0… and +593… match) or email.", false,
            schema(listOf("query"), "query" to prop("string", "1–100 characters."), "limit" to prop("integer", "1–50, default 10."))),
        Tool("contacts_get", "One contact: numbers, emails, company, birthday, address, note and the kinds of account it is saved in.", false,
            schema(listOf("id"), "id" to prop("integer", "From contacts_search."))),
        Tool("contacts_favorites", "The contacts marked as favourites.", false, schema(emptyList())),
        Tool("contacts_birthdays", "Birthdays coming up, soonest first, with the age they turn when the year is known.", false,
            schema(emptyList(), "days" to prop("integer", "1–366, default 30."))),
        Tool("contacts_create", "Creates a contact in the phone.", true,
            schema(listOf("name"), "name" to prop("string", "Up to ${Contact.NAME_MAX} characters."), "phone" to prop("string", "A number."),
                "email" to prop("string", "An email address."), "company" to prop("string", "Company."), "birthday" to prop("string", "YYYY-MM-DD or MM-DD."),
                "note" to prop("string", "Up to 500 characters."))),
        Tool("contacts_favorite", "Marks or unmarks a contact as favourite.", true,
            schema(listOf("id", "favorite"), "id" to prop("integer", "From contacts_search."), "favorite" to prop("boolean", "true to mark."))),
    )

    fun available(access: Access) = tools.filter { !it.write || access == Access.WRITE }

    fun call(name: String, args: JSONObject, access: Access, data: ContactData): JSONObject {
        val tool = tools.firstOrNull { it.name == name } ?: throw ToolFailure("UNKNOWN_TOOL", "No tool named $name.")
        if (tool.write && access != Access.WRITE) throw ToolFailure("READ_ONLY", "This token can only read. Ask for a read-and-write token.")
        val known = tool.schema.getJSONObject("properties").keys().asSequence().toSet()
        args.keys().forEach { if (it !in known) throw ToolFailure("BAD_ARGUMENT", "Unknown argument «$it».") }
        try {
            return when (name) {
                "contacts_search" -> {
                    val q = text(args, "query", 100, required = true)
                    val limit = int(args, "limit", 10, 1..50)
                    val found = Contacts.search(data.store.list(), q)
                    JSONObject().put("total", found.size).put("contacts", JSONArray(found.take(limit).map(::brief)))
                }
                "contacts_get" -> JSONObject().put("contact", full(one(args, data)))
                "contacts_favorites" -> JSONObject().put("contacts", JSONArray(Contacts.sorted(data.store.list().filter { it.starred }).map(::brief)))
                "contacts_birthdays" -> {
                    val days = int(args, "days", 30, 1..366)
                    val today = data.today()
                    JSONObject().put("birthdays", JSONArray(Contacts.upcoming(data.store.list(), today, days).map { (c, d) ->
                        brief(c).put("date", d.toString()).put("days", java.time.temporal.ChronoUnit.DAYS.between(today, d)).put("turns", c.birthday?.turns(today) ?: JSONObject.NULL)
                    }))
                }
                "contacts_create" -> create(args, data)
                "contacts_favorite" -> {
                    val c = one(args, data)
                    val on = args.opt("favorite") as? Boolean ?: throw ToolFailure("BAD_ARGUMENT", "«favorite» is true or false.")
                    data.store.star(c.id, on); data.changed()
                    JSONObject().put("id", c.id).put("favorite", on)
                }
                else -> throw ToolFailure("UNKNOWN_TOOL", "No tool named $name.")
            }
        } catch (_: NoAccess) { throw ToolFailure("NO_PERMISSION", "The user hasn't allowed Contactos to use the contacts. Ask them to open Contactos once.") }
    }

    private fun create(args: JSONObject, data: ContactData): JSONObject {
        val name = text(args, "name", Contact.NAME_MAX, required = true).trim()
        val phone = text(args, "phone", 40).let(Phones::clean)
        if (args.has("phone") && phone.count { it.isDigit() } < 3) throw ToolFailure("BAD_ARGUMENT", "«phone» needs at least 3 digits.")
        val email = text(args, "email", 100).trim()
        if (email.isNotEmpty() && !email.matches(Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))) throw ToolFailure("BAD_ARGUMENT", "«email» is not an address.")
        val birthday = if (args.has("birthday")) Birthday.parse(text(args, "birthday", 10)) ?: throw ToolFailure("BAD_ARGUMENT", "«birthday» is YYYY-MM-DD or MM-DD.") else null
        val c = data.store.save(Contact(0, name, listOfNotNull(phone.takeIf { it.isNotEmpty() }?.let { Phone(it, Label.MOBILE) }),
            listOfNotNull(email.takeIf { it.isNotEmpty() }?.let { Email(it, Label.HOME) }), text(args, "company", 100), text(args, "note", 500), birthday,
            raws = listOf(Raw(0, data.account() ?: Account.PHONE))))
        data.changed()
        return JSONObject().put("contact", full(c))
    }

    private fun one(args: JSONObject, data: ContactData): Contact {
        val id = (args.opt("id") as? Int)?.toLong() ?: (args.opt("id") as? Long) ?: throw ToolFailure("BAD_ARGUMENT", "«id» is a whole number.")
        return data.store.list().firstOrNull { it.id == id } ?: throw ToolFailure("NOT_FOUND", "No contact with id $id.")
    }

    private fun brief(c: Contact) = JSONObject().put("id", c.id).put("name", c.shownName).put("phone", c.phones.firstOrNull()?.number ?: JSONObject.NULL)
        .put("favorite", c.starred)

    private fun full(c: Contact) = brief(c).put("phones", JSONArray(c.phones.map { JSONObject().put("number", it.number).put("label", it.label.key) }))
        .put("emails", JSONArray(c.emails.map { JSONObject().put("address", it.address).put("label", it.label.key) }))
        .put("company", c.org).put("birthday", c.birthday?.iso() ?: JSONObject.NULL).put("address", c.address).put("note", c.note)
        // Where it is saved, as kinds only («google», «sim», «phone»…): never account names or emails.
        .put("accounts", JSONArray(c.raws.map { it.account.kind.name.lowercase() }.distinct()))

    private fun int(args: JSONObject, key: String, default: Int, range: IntRange): Int {
        if (!args.has(key)) return default
        val n = (args.opt(key) as? Int) ?: (args.opt(key) as? Long)?.toInt() ?: throw ToolFailure("BAD_ARGUMENT", "«$key» is a whole number.")
        if (n !in range) throw ToolFailure("BAD_ARGUMENT", "«$key» is ${range.first}–${range.last}.")
        return n
    }

    private fun text(args: JSONObject, key: String, max: Int, required: Boolean = false): String {
        if (!args.has(key)) { if (required) throw ToolFailure("BAD_ARGUMENT", "«$key» is missing."); return "" }
        val v = args.opt(key) as? String ?: throw ToolFailure("BAD_ARGUMENT", "«$key» is text.")
        if (v.length > max) throw ToolFailure("BAD_ARGUMENT", "«$key» is longer than $max characters.")
        if (required && v.isBlank()) throw ToolFailure("BAD_ARGUMENT", "«$key» is empty.")
        return v
    }
}

/**
 * The real contacts for the tools (Contactos lives inside Teléfono now; Teléfono's Modo MCP serves these tools too). Every
 * change is shown on the contacts screen through [ContactTools.onChanged].
 */
class ContactLiveData(context: android.content.Context) : ContactData {
    override val store = com.zirakusa.contactos.AndroidStore(context)
    private val settings = com.zirakusa.contactos.data.Settings(com.zirakusa.contactos.data.SharedValues(context))
    override fun account() = settings.storage
    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    override fun changed() { main.post { ContactTools.onChanged() } }
}
