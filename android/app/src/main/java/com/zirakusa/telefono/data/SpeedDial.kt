package com.zirakusa.telefono.data

/** A key of «Marcación rápida» (2–9) with its number and, when it came from a contact, the name. */
data class Speed(val key: Char, val number: String, val name: String = "")

/** What a long press on a key of the keypad does. */
sealed interface LongPress {
    /** Call this number (the same way as the call button). */
    data class Call(val number: String) : LongPress
    /** The key has nothing yet: open «Marcación rápida» to give it one. */
    data class Assign(val key: Char) : LongPress
    /** Just type the key, as a short press would. */
    data object Type : LongPress
}

/** A number of a contact to pick for a key. */
data class Choice(val name: String, val number: String)

/** Pure rules of «Marcación rápida»: keys 2–9 (1 is the voicemail), saved one per line. */
object SpeedDial {
    val keys = ('2'..'9').toList()

    fun canSet(key: Char, number: String) = key in keys && Numbers.key(number).isNotEmpty() && !Dial.isCode(number)

    /** The list with [key] set to this number (replacing what it had), by key. */
    fun set(list: List<Speed>, key: Char, number: String, name: String = ""): List<Speed> {
        if (!canSet(key, number)) return list
        return (list.filter { it.key != key } + Speed(key, Dial.clean(number.trim()), name.trim())).sortedBy { it.key }
    }

    fun remove(list: List<Speed>, key: Char) = list.filter { it.key != key }
    fun of(list: List<Speed>, key: Char) = list.firstOrNull { it.key == key }

    /**
     * A long press on [key]: only with nothing typed (otherwise it types, like a short press). 1 calls the voicemail
     * when the carrier has one; 2–9 call their number, or ask to assign one.
     */
    fun longPress(typed: String, key: Char, list: List<Speed>, voicemail: String?): LongPress = when {
        typed.isNotEmpty() -> LongPress.Type
        key == '1' -> voicemail?.takeIf { v -> v.any { it.isDigit() } }?.let { LongPress.Call(it) } ?: LongPress.Type
        key in keys -> of(list, key)?.let { LongPress.Call(it.number) } ?: LongPress.Assign(key)
        else -> LongPress.Type
    }

    /** Contacts' numbers matching what was written (name without accents, or digits), and the written number itself. */
    fun choices(people: List<Person>, query: String): List<Choice> {
        val q = Dial.plain(query.trim())
        val digits = query.filter { it.isDigit() }
        val all = people.sortedBy { Dial.plain(it.name) }.flatMap { p -> p.numbers.map { Choice(p.name, it) } }
        val found = if (q.isEmpty()) all else all.filter { Dial.plain(it.name).contains(q) || (digits.isNotEmpty() && it.number.filter(Char::isDigit).contains(digits)) }
        val typed = Dial.clean(query.trim())
        val own = if (digits.length >= 3 && !Dial.isCode(typed) && found.none { Numbers.key(it.number) == Numbers.key(typed) }) listOf(Choice("", typed)) else emptyList()
        return own + found
    }

    /** «2\t0991234567\tAna Pérez», one line per key. */
    fun encode(list: List<Speed>) = list.joinToString("\n") { s -> "${s.key}\t${s.number}\t${s.name.replace(Regex("[\\t\\n]"), " ")}" }

    fun decode(text: String?): List<Speed> = text.orEmpty().split('\n').mapNotNull { line ->
        val p = line.split('\t')
        val key = p.getOrNull(0)?.singleOrNull() ?: return@mapNotNull null
        val number = p.getOrNull(1).orEmpty()
        if (canSet(key, number)) Speed(key, number, p.getOrNull(2).orEmpty()) else null
    }.distinctBy { it.key }.sortedBy { it.key }
}
