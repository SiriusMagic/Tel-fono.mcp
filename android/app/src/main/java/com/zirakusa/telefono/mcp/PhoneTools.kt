package com.zirakusa.telefono.mcp

import com.zirakusa.telefono.data.Call
import com.zirakusa.telefono.data.CallType
import com.zirakusa.telefono.data.Dial
import com.zirakusa.telefono.data.NoAccess
import com.zirakusa.telefono.data.Numbers
import com.zirakusa.telefono.data.PhoneBook
import com.zirakusa.telefono.data.Sim
import com.zirakusa.telefono.data.Sims
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** What the tools read and ask; the app gives Android's call log and a notification, tests fakes. */
interface PhoneData {
    val book: PhoneBook
    fun zone(): ZoneId = ZoneId.systemDefault()
    fun today(): LocalDate = LocalDate.now(zone())
    /** Shows «[who] quiere que llames a …» on the phone; false when notifications are off. */
    fun askToCall(who: String, number: String, name: String): Boolean
}

/** An expected failure the AI can read (structuredContent.error.code), never a crash. */
class ToolFailure(val code: String, message: String) : Exception(message)

/**
 * Teléfono's MCP tools: the recent and missed calls; with a read-and-write token the AI may also *ask* the user to call
 * someone (a notification the user taps). The AI never dials, hangs up or answers.
 */
object PhoneTools {
    class Tool(val name: String, val description: String, val write: Boolean, val schema: JSONObject)

    private fun schema(required: List<String>, vararg props: Pair<String, JSONObject>) = JSONObject()
        .put("type", "object").put("properties", JSONObject().apply { props.forEach { (k, v) -> put(k, v) } })
        .put("required", JSONArray(required)).put("additionalProperties", false)
    private fun prop(type: String, description: String) = JSONObject().put("type", type).put("description", description)

    val tools = listOf(
        Tool("phone_calls", "The phone's recent calls, newest first (number, saved name, incoming/outgoing/missed, time, duration, and sim 1/2 on dual-SIM phones).", false,
            schema(emptyList(), "type" to JSONObject().put("type", "string").put("enum", JSONArray(CallType.entries.map { it.key })).put("description", "Only this kind."),
                "limit" to prop("integer", "1–100, default 20."))),
        Tool("phone_missed", "Missed calls since a day (default today).", false, schema(emptyList(), "since" to prop("string", "YYYY-MM-DD, within the last 60 days."))),
        Tool("phone_call", "One call by its id.", false, schema(listOf("id"), "id" to prop("integer", "From phone_calls."))),
        Tool("phone_call_request", "Asks the user to call a number: a notification on the phone that opens the dialer. The AI never dials.", true,
            schema(listOf("number"), "number" to prop("string", "The number."), "reason" to prop("string", "Why (up to 120 characters), shown to the user."))),
    )

    fun available(access: Access) = tools.filter { !it.write || access == Access.WRITE }

    fun call(name: String, args: JSONObject, access: Access, data: PhoneData, who: String): JSONObject {
        val tool = tools.firstOrNull { it.name == name } ?: throw ToolFailure("UNKNOWN_TOOL", "No tool named $name. (Teléfono never dials for the AI.)")
        if (tool.write && access != Access.WRITE) throw ToolFailure("READ_ONLY", "This token can only read. Ask for a read-and-write token.")
        val known = tool.schema.getJSONObject("properties").keys().asSequence().toSet()
        args.keys().forEach { if (it !in known) throw ToolFailure("BAD_ARGUMENT", "Unknown argument «$it».") }
        try {
            val sims by lazy { data.book.sims() }
            return when (name) {
                "phone_calls" -> {
                    val type = if (args.has("type")) CallType.entries.firstOrNull { it.key == args.opt("type") } ?: throw ToolFailure("BAD_ARGUMENT", "«type» is one of ${CallType.entries.joinToString { it.key }}.") else null
                    val limit = if (args.has("limit")) ((args.opt("limit") as? Int)?.takeIf { it in 1..100 } ?: throw ToolFailure("BAD_ARGUMENT", "«limit» is 1–100.")) else 20
                    val all = data.book.calls().filter { type == null || it.type == type }
                    JSONObject().put("calls", JSONArray(all.take(limit).map { call(it, data, sims) }))
                }
                "phone_missed" -> {
                    val today = data.today()
                    val since = if (args.has("since")) (args.opt("since") as? String)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                        ?.takeIf { !it.isAfter(today) && !it.isBefore(today.minusDays(60)) } ?: throw ToolFailure("BAD_ARGUMENT", "«since» is YYYY-MM-DD within the last 60 days.") else today
                    val missed = Dial.missed(data.book.calls(), since, data.zone())
                    JSONObject().put("since", since.toString()).put("total", missed.size).put("calls", JSONArray(missed.take(50).map { call(it, data, sims) }))
                }
                "phone_call" -> {
                    val id = (args.opt("id") as? Int)?.toLong() ?: (args.opt("id") as? Long) ?: throw ToolFailure("BAD_ARGUMENT", "«id» is a whole number.")
                    JSONObject().put("call", call(data.book.calls().firstOrNull { it.id == id } ?: throw ToolFailure("NOT_FOUND", "No call with id $id."), data, sims))
                }
                "phone_call_request" -> {
                    val number = (args.opt("number") as? String)?.let(Dial::clean) ?: throw ToolFailure("BAD_ARGUMENT", "«number» is text.")
                    if (number.count { it.isDigit() } < 3) throw ToolFailure("BAD_ARGUMENT", "«number» needs at least 3 digits.")
                    if (Dial.isCode(number)) throw ToolFailure("BAD_ARGUMENT", "Phone codes (*…#) can't be requested by the AI.")
                    val reason = if (args.has("reason")) (args.opt("reason") as? String)?.take(120) ?: throw ToolFailure("BAD_ARGUMENT", "«reason» is text.") else ""
                    val saved = Numbers.nameOf(data.book.people(), number).orEmpty()
                    if (!data.askToCall(who, number, listOf(saved, reason).filter { it.isNotBlank() }.joinToString(" · ")))
                        throw ToolFailure("NOTIFICATIONS_OFF", "Teléfono's notifications are off on the phone; the user can't see the request.")
                    JSONObject().put("asked", true).put("number", number).put("name", saved).put("note", "The user decides whether to call.")
                }
                else -> throw ToolFailure("UNKNOWN_TOOL", "No tool named $name.")
            }
        } catch (_: NoAccess) { throw ToolFailure("NO_PERMISSION", "The user hasn't allowed Teléfono to read the calls. Ask them to open Teléfono once.") }
    }

    /** One call for the AI; «sim» (1, 2) only when the phone has SIMs Teléfono can tell apart. */
    private fun call(c: Call, data: PhoneData, sims: List<Sim>) = JSONObject().put("id", c.id).put("number", c.number).put("name", c.name).put("type", c.type.key)
        .put("at", Instant.ofEpochMilli(c.at).atZone(data.zone()).toLocalDateTime().withNano(0).toString()).put("seconds", c.seconds)
        .apply { Sims.slotOf(c.account, sims)?.let { put("sim", it) } }
}
