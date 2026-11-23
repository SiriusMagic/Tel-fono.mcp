package com.zirakusa.telefono.data

import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** How a call went. [key] is what MCP uses. */
enum class CallType(val key: String) { INCOMING("incoming"), OUTGOING("outgoing"), MISSED("missed"), REJECTED("rejected"), BLOCKED("blocked"), VOICEMAIL("voicemail") }

/**
 * One call of the phone's log. [name] is the contact's, when the number is saved; [account] is the phone account (SIM) it
 * used, as the log keeps it; [label] what the phone says of the number («Móvil» for a contact, «Ecuador» for the rest).
 */
data class Call(val id: Long, val number: String, val name: String = "", val type: CallType, val at: Long, val seconds: Long = 0,
    val account: String = "", val label: String = "")

/** A person of the phone's contacts with their numbers (for the keypad search and favourites). */
data class Person(val id: Long, val name: String, val numbers: List<String>, val starred: Boolean = false)

/** Several calls with the same number in a row, shown as one line («Ana Pérez (3)»). */
data class Group(val calls: List<Call>) {
    val first get() = calls.first()
    val count get() = calls.size
    val missed get() = calls.any { it.type == CallType.MISSED }
}

/** Pure rules of the phone: groups of calls, the keypad's letters (T9), durations and codes. */
object Dial {
    /** Calls in a row with the same number and the same day are one line. Newest first. */
    fun group(calls: List<Call>, zone: ZoneId = ZoneId.systemDefault()): List<Group> {
        val out = ArrayList<MutableList<Call>>()
        for (c in calls.sortedByDescending { it.at }) {
            val last = out.lastOrNull()?.last()
            if (last != null && Numbers.key(last.number) == Numbers.key(c.number) && day(last.at, zone) == day(c.at, zone)) out.last() += c
            else out += mutableListOf(c)
        }
        return out.map(::Group)
    }
    private fun day(at: Long, zone: ZoneId) = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()

    /** Missed calls since [since] (local day start), newest first. */
    fun missed(calls: List<Call>, since: LocalDate, zone: ZoneId = ZoneId.systemDefault()) =
        calls.filter { it.type == CallType.MISSED && !day(it.at, zone).isBefore(since) }.sortedByDescending { it.at }

    /** The keypad digit of a letter, like on every phone (ñ is 6, accents ignored). */
    fun digit(ch: Char): Char? {
        val c = plain(ch.toString()).firstOrNull() ?: return null
        if (c.isDigit()) return c
        return when (c) {
            in "abc" -> '2'; in "def" -> '3'; in "ghi" -> '4'; in "jkl" -> '5'; in "mnoñ" -> '6'; in "pqrs" -> '7'; in "tuv" -> '8'; in "wxyz" -> '9'
            else -> if (ch == 'ñ' || ch == 'Ñ') '6' else null
        }
    }

    /** «Ana Pérez» → «262 73739»: the digits each word would be typed with. */
    fun t9(name: String) = name.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }.map { w -> w.mapNotNull(::digit).joinToString("") }

    /**
     * People matching what was typed on the keypad: a word of the name starting with those keys (T9), or a number
     * containing those digits. Names that match first.
     */
    fun search(people: List<Person>, typed: String): List<Person> {
        val d = typed.filter { it.isDigit() }
        if (d.isEmpty()) return emptyList()
        val byName = people.filter { p -> t9(p.name).any { it.startsWith(d) } || t9(p.name).joinToString("").startsWith(d) }
        val byNumber = people.filter { p -> p !in byName && p.numbers.any { it.filter(Char::isDigit).contains(d) } }
        return (byName.sortedBy { it.name } + byNumber.sortedBy { it.name })
    }

    /** «0:42», «12:05», «1:02:03». */
    fun duration(seconds: Long): String {
        val h = seconds / 3600; val m = seconds / 60 % 60; val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    /** «38 s», «1 m 38 s», «1 h 2 m 3 s»: the length of a call in a number's history. */
    fun spoken(seconds: Long): String {
        val h = seconds / 3600; val m = seconds / 60 % 60; val s = seconds % 60
        return listOfNotNull(if (h > 0) "$h h" else null, if (h > 0 || m > 0) "$m m" else null, "$s s").joinToString(" ")
    }

    /** An outgoing call of 0 seconds: nobody answered («No conectado»). */
    fun notConnected(c: Call) = c.type == CallType.OUTGOING && c.seconds == 0L

    /** Every call with this number (0… and +593… alike), newest first; private calls go with the private ones. */
    fun history(calls: List<Call>, number: String): List<Call> {
        val k = Numbers.key(number)
        return calls.filter { if (k.isEmpty()) it.number.isBlank() else Numbers.key(it.number) == k }.sortedByDescending { it.at }
    }

    /** The big letter of the avatar: the name's first letter, or «#» for a number. */
    fun initial(name: String) = name.trim().firstOrNull { it.isLetter() }?.uppercaseChar()?.toString() ?: "#"

    /** A code like *611# or *#06#: it goes to the phone, not to a person. */
    fun isCode(number: String) = number.trim().let { it.startsWith("*") || it.startsWith("#") } && number.trim().endsWith("#")

    /** What the keypad keeps: digits, + (only first), * and #. */
    fun clean(typed: String) = typed.filterIndexed { i, c -> c.isDigit() || c in "*#" || (c == '+' && i == 0) }.take(40)

    /** The number for a tel: link; «#» must be written %23 or Android cuts the code. */
    fun telUri(number: String) = "tel:" + clean(number).replace("#", "%23")

    fun plain(s: String) = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
}

/** Numbers the Ecuadorian way: «099 123 4567» and «+593 99 123 4567» are the same. */
object Numbers {
    fun key(number: String): String {
        var d = number.filter { it.isDigit() }
        if (d.startsWith("593") && d.length >= 11) d = d.drop(3)
        return d.trimStart('0').takeLast(9)
    }

    fun pretty(number: String): String {
        val plus = number.trim().startsWith("+")
        val d = number.filter { it.isDigit() }
        return when {
            !plus && d.length == 10 && d.startsWith("09") -> "${d.take(3)} ${d.substring(3, 6)} ${d.drop(6)}"
            !plus && d.length == 9 && d.startsWith("0") -> "${d.take(2)} ${d.substring(2, 5)} ${d.drop(5)}"
            plus && d.startsWith("5939") && d.length == 12 -> "+593 ${d.substring(3, 5)} ${d.substring(5, 8)} ${d.drop(8)}"
            else -> number.trim()
        }
    }

    /**
     * The digits for WhatsApp (wa.me): international without «+», Ecuador's by default. «099 123 4567» → «593991234567»,
     * «02 234 5678» → «59322345678», «+34 600…» → «34600…». Null when it can't be a phone number (codes, too short).
     */
    fun wa(number: String): String? {
        if (Dial.isCode(number)) return null
        val d = number.filter { it.isDigit() }
        val out = when {
            number.trim().startsWith("+") -> d
            d.startsWith("00") -> d.drop(2)
            d.startsWith("593") && d.length >= 11 -> d
            d.startsWith("0") -> "593" + d.drop(1)
            d.length == 9 && d.startsWith("9") -> "593$d"
            else -> d
        }
        return out.takeIf { it.length in 8..15 }
    }

    /** The contact that has this number, if any. */
    fun personOf(people: List<Person>, number: String): Person? =
        key(number).takeIf { it.length >= 7 }?.let { k -> people.firstOrNull { p -> p.numbers.any { key(it) == k } } }

    /** The saved name of a number, if any contact has it. */
    fun nameOf(people: List<Person>, number: String): String? = key(number).takeIf { it.length >= 7 }?.let { k -> people.firstOrNull { p -> p.numbers.any { key(it) == k } }?.name }
}
