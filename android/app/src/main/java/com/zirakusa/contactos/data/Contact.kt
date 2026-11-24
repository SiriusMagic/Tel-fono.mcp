package com.zirakusa.contactos.data

import java.text.Collator
import java.text.Normalizer
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Kinds of phone and email, as Android names them. [key] is what MCP uses. */
enum class Label(val key: String) { MOBILE("mobile"), HOME("home"), WORK("work"), OTHER("other") }

data class Phone(val number: String, val label: Label = Label.MOBILE)
data class Email(val address: String, val label: Label = Label.HOME)

/** A birthday: day and month, and the year when known. */
data class Birthday(val month: Int, val day: Int, val year: Int? = null) {
    init { require(month in 1..12 && day in 1..31) }
    fun next(today: LocalDate): LocalDate {
        fun on(y: Int) = if (month == 2 && day == 29 && !java.time.Year.isLeap(y.toLong())) LocalDate.of(y, 3, 1)
            else LocalDate.of(y, month, day.coerceAtMost(java.time.YearMonth.of(y, month).lengthOfMonth()))
        val t = on(today.year)
        return if (t.isBefore(today)) on(today.year + 1) else t
    }
    /** The age they turn on [next], when the year is known. */
    fun turns(today: LocalDate) = year?.let { next(today).year - it }
    /** «1990-12-02» or «--12-02» (vCard without year). */
    fun iso() = if (year != null) "%04d-%02d-%02d".format(year, month, day) else "--%02d-%02d".format(month, day)

    companion object {
        /** Reads «1990-12-02», «1990/12/02», «--12-02» or «12/02». */
        fun parse(text: String?): Birthday? {
            val t = text?.trim().orEmpty()
            Regex("""^(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})$""").find(t)?.destructured?.let { (y, m, d) -> return runCatching { Birthday(m.toInt(), d.toInt(), y.toInt()) }.getOrNull() }
            Regex("""^(?:--)?(\d{1,2})[-/.](\d{1,2})$""").find(t)?.destructured?.let { (m, d) -> return runCatching { Birthday(m.toInt(), d.toInt()) }.getOrNull() }
            return null
        }
    }
}

/** One contact as the app shows it (Android may keep it in several accounts; [id] is its contact id). */
data class Contact(val id: Long = 0, val name: String = "", val phones: List<Phone> = emptyList(), val emails: List<Email> = emptyList(),
    val org: String = "", val note: String = "", val birthday: Birthday? = null, val address: String = "", val starred: Boolean = false,
    val photo: String? = null, val raws: List<Raw> = emptyList(), val altName: String = "", val ringtone: String? = null) {
    val shownName get() = name.ifBlank { phones.firstOrNull()?.number ?: emails.firstOrNull()?.address ?: "" }
    /** Up to two letters for the round picture («AP» for Ana Pérez). */
    val initials get() = shownName.split(' ', '-').filter { it.isNotBlank() && it[0].isLetterOrDigit() }.take(2).joinToString("") { it.take(1).uppercase() }
        .ifEmpty { shownName.take(1).uppercase() }

    companion object { const val NAME_MAX = 100 }
}

/** Pure rules of the list: order, letters, search, duplicates and birthdays. */
object Contacts {
    private val collator = Collator.getInstance(Locale.forLanguageTag("es")).apply { strength = Collator.PRIMARY }

    /** What a contact is sorted by: the name, or «Pérez Ana» when sorting by last name. */
    fun key(c: Contact, order: Order = Order.FIRST): String = if (order == Order.LAST) lastFirst(c).replace(",", "") else c.shownName

    /** «Pérez, Ana»: Android's own alternative name when it has one, else the last word first. */
    private fun lastFirst(c: Contact): String {
        if (c.altName.isNotBlank()) return c.altName.trim()
        val words = c.name.trim().split(Regex("\\s+"))
        return if (words.size > 1) words.drop(1).joinToString(" ") + ", " + words.first() else c.shownName
    }

    /** The name as «Formato del nombre» asks: «Ana Pérez» or «Pérez, Ana». */
    fun display(c: Contact, format: NameFormat) = if (format == NameFormat.LAST && c.name.isNotBlank()) lastFirst(c) else c.shownName

    /** The letters of the bar on the right: always the whole alphabet, Ñ after N, and #. */
    val INDEX: List<Char> = ('A'..'N') + 'Ñ' + ('O'..'Z') + '#'

    /** Tapping a letter with nobody under it goes to the next one that has someone (or the last). */
    fun jump(present: List<Char>, tapped: Char): Char? {
        if (present.isEmpty()) return null
        val at = INDEX.indexOf(tapped)
        return present.filter { INDEX.indexOf(it) >= at }.minByOrNull { INDEX.indexOf(it) } ?: present.maxBy { INDEX.indexOf(it) }
    }

    /** Several parts of one person (one per account) shown as one: numbers and emails once each. */
    fun merge(parts: List<Contact>): Contact {
        val first = parts.first()
        if (parts.size == 1) return first
        fun text(f: (Contact) -> String) = parts.map(f).firstOrNull { it.isNotBlank() }.orEmpty()
        return Contact(first.id, text { it.name },
            parts.flatMap { it.phones }.distinctBy { Phones.key(it.number).ifEmpty { it.number } },
            parts.flatMap { it.emails }.distinctBy { it.address.lowercase() },
            text { it.org }, text { it.note }, parts.firstNotNullOfOrNull { it.birthday }, text { it.address }, parts.any { it.starred },
            parts.firstNotNullOfOrNull { it.photo }, parts.flatMap { it.raws }, text { it.altName }, parts.firstNotNullOfOrNull { it.ringtone })
    }

    /**
     * «Ocultar los contactos repetidos»: the same name with the same numbers (usually one copy per account) is shown once,
     * the one with the most to show. People who only share a name or a number stay.
     */
    fun hideDuplicates(items: List<Contact>): List<Contact> {
        val seen = HashSet<String>()
        val kept = items.sortedByDescending { it.phones.size + it.emails.size + (if (it.starred) 10 else 0) + (if (it.birthday != null) 1 else 0) }
            .filter { c -> seen.add(plain(c.shownName).trim() + "|" + c.phones.map { Phones.key(it.number) }.toSortedSet().joinToString(",")) }
            .map { it.id }.toHashSet()
        return items.filter { it.id in kept }
    }

    /** What the duplicates banner remembers when closed: it comes back only when there are new duplicates. */
    fun signature(groups: List<List<Contact>>) = groups.flatten().map { it.id }.sorted().joinToString(",")

    /** Already in the phone: the same name and a number in common (or neither has a number). For «Importar». */
    fun known(items: List<Contact>, c: Contact): Boolean {
        val keys = c.phones.map { Phones.key(it.number) }.toSet()
        return items.any { o -> plain(o.shownName).trim() == plain(c.shownName).trim() &&
            (keys.isEmpty() && o.phones.isEmpty() || o.phones.any { Phones.key(it.number) in keys }) }
    }

    /** Letters then «#» for names that start with something else. */
    fun sorted(items: List<Contact>, order: Order = Order.FIRST): List<Contact> =
        items.sortedWith(compareBy<Contact> { letter(it, order) == '#' }.thenComparator { a, b -> collator.compare(key(a, order), key(b, order)) })

    /** The letter a name is listed under; Ñ stays Ñ, accents go (Álvaro → A). */
    fun letter(c: Contact, order: Order = Order.FIRST): Char {
        val first = key(c, order).trim().firstOrNull() ?: return '#'
        if (first.uppercaseChar() == 'Ñ') return 'Ñ'
        val plain = plain(first.toString()).firstOrNull()?.uppercaseChar() ?: return '#'
        return if (plain in 'A'..'Z') plain else '#'
    }

    fun groups(items: List<Contact>, order: Order = Order.FIRST): List<Pair<Char, List<Contact>>> =
        sorted(items, order).groupBy { letter(it, order) }.toList()

    /** Names (any word's start, accents ignored), numbers (digits anywhere, Ecuador's 0/+593 both) and emails. */
    fun search(items: List<Contact>, query: String): List<Contact> {
        val q = plain(query.trim())
        if (q.isEmpty()) return emptyList()
        val digits = query.filter { it.isDigit() }
        val key = if (digits.length >= 6) Phones.key(digits) else null
        val found = items.filter { c ->
            val words = plain(c.shownName + " " + c.org).split(' ', '-', '.')
            words.any { it.startsWith(q) } || plain(c.shownName).startsWith(q) ||
                (digits.length >= 3 && c.phones.any { p -> p.number.filter(Char::isDigit).contains(digits) || (key != null && Phones.key(p.number).contains(key)) }) ||
                c.emails.any { plain(it.address).contains(q) }
        }
        return sorted(found).sortedBy { if (plain(it.shownName).startsWith(q)) 0 else 1 }
    }

    /** Contacts that look like the same person: the same number, or the same name. Groups of two or more. */
    fun duplicates(items: List<Contact>): List<List<Contact>> {
        val parent = items.indices.toMutableList()
        fun find(i: Int): Int { var x = i; while (parent[x] != x) x = parent[x]; return x }
        val seen = HashMap<String, Int>()
        items.forEachIndexed { i, c ->
            val keys = c.phones.map { "p:" + Phones.key(it.number) }.filter { it.length > 8 } + listOfNotNull(plain(c.name).takeIf { it.length > 2 }?.let { "n:$it" })
            keys.forEach { k -> seen[k]?.let { j -> parent[find(i)] = find(j) } ?: run { seen[k] = i } }
        }
        return items.indices.groupBy(::find).values.filter { it.size > 1 }.map { g -> g.map { items[it] } }
    }

    /** Birthdays in the next [days] days (today first). */
    fun upcoming(items: List<Contact>, today: LocalDate, days: Int): List<Pair<Contact, LocalDate>> =
        items.mapNotNull { c -> c.birthday?.next(today)?.let { c to it } }.filter { ChronoUnit.DAYS.between(today, it.second) in 0..days.toLong() }
            .sortedWith(compareBy({ it.second }, { it.first.shownName }))

    fun plain(s: String) = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
}

/** Phone numbers the Ecuadorian way: the same person with «099 123 4567» and «+593 99 123 4567». */
object Phones {
    /** A key to compare numbers: the digits without the country or the leading 0 (the last 9 digits at most). */
    fun key(number: String): String {
        var d = number.filter { it.isDigit() }
        if (d.startsWith("593") && d.length >= 11) d = d.drop(3)
        d = d.trimStart('0')
        return d.takeLast(9)
    }

    /** «099 123 4567» for Ecuadorian mobiles, «02 234 5678» for landlines; others as typed. */
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

    /** What a phone keeps: digits, «+» first, and * # for codes. */
    fun clean(number: String) = number.trim().let { t -> (if (t.startsWith("+")) "+" else "") + t.filter { it.isDigit() || it in "*#" } }
}
