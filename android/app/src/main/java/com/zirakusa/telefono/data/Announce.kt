package com.zirakusa.telefono.data

/** «Anuncio de voz de llamada entrante»: never (factory setting), always, or only with headphones on. */
enum class AnnounceMode { NEVER, ALWAYS, HEADPHONES }

/** The words of the announcement, from the app's texts («Llamada de», «número privado», «más»). */
data class AnnounceWords(val callFrom: String, val private: String, val plus: String)

/** Pure rules of the spoken announcement: when to speak and what. */
object Announce {
    fun should(mode: AnnounceMode, headphones: Boolean) = when (mode) {
        AnnounceMode.NEVER -> false; AnnounceMode.ALWAYS -> true; AnnounceMode.HEADPHONES -> headphones
    }

    /** «Llamada de Ana Pérez», «Llamada de 0 9 9, 1 2 3, 4 5 6 7» or «Llamada de número privado». */
    fun text(name: String?, number: String, hidden: Boolean, w: AnnounceWords): String {
        val who = when {
            !name.isNullOrBlank() -> name.trim()
            hidden || number.none { it.isDigit() } -> w.private
            else -> spoken(number, w.plus)
        }
        return "${w.callFrom} $who"
    }

    /**
     * The number in its groups, digit by digit, so the voice doesn't read «noventa y nueve millones…»:
     * «099 123 4567» → «0 9 9, 1 2 3, 4 5 6 7»; «+593 99…» → «más 5 9 3, 9 9, …». Numbers without a known shape go in
     * threes (a lone last digit joins the group before).
     */
    fun spoken(number: String, plus: String): String {
        val pretty = Numbers.pretty(number)
        val groups = if (pretty.contains(' ')) pretty.split(' ').filter { it.isNotEmpty() }.map { g -> g.filter { it.isDigit() || it == '+' } }
        else {
            val lead = if (number.trim().startsWith("+")) "+" else ""
            val chunks = number.filter { it.isDigit() }.chunked(3).toMutableList()
            if (chunks.size > 1 && chunks.last().length == 1) chunks[chunks.size - 2] = chunks[chunks.size - 2] + chunks.removeAt(chunks.size - 1)
            chunks.mapIndexed { i, g -> if (i == 0) lead + g else g }
        }
        return groups.filter { it.isNotEmpty() }.joinToString(", ") { g -> g.map { if (it == '+') plus else "$it" }.joinToString(" ") }
    }
}
