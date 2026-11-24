package com.zirakusa.telefono.data

/** What happens to an incoming call: it rings, it rings without sound (shows as missed), or it is rejected silently. */
enum class Verdict { ALLOW, SILENCE, REJECT }

/** «Números que no están en tus contactos»: they ring (factory setting), ring without sound, or are blocked. */
enum class Unknown { RING, SILENCE, BLOCK }

/** The options of «Filtro de llamadas no deseadas». [blocked] is the «Bloqueados» list, as the user saved the numbers. */
data class FilterSettings(val hidden: Boolean = false, val unknown: Unknown = Unknown.RING, val blocked: List<String> = emptyList())

/** Pure rules of the call filter (the screening service asks them) and of the «Bloqueados» list. */
object Filter {
    /**
     * The verdict for an incoming call. [inContacts] is null when Teléfono can't read the contacts: then «not in
     * contacts» never blocks anyone (it would block everybody). A number of the «Bloqueados» list is rejected even if
     * it is a contact; 099… and +593 99… are the same number.
     */
    fun decide(number: String?, hidden: Boolean, inContacts: Boolean?, s: FilterSettings): Verdict {
        val key = Numbers.key(number.orEmpty())
        val private = hidden || key.isEmpty()
        if (!private && isBlocked(s.blocked, number.orEmpty())) return Verdict.REJECT
        if (private) return if (s.hidden) Verdict.REJECT else Verdict.ALLOW
        if (inContacts != false) return Verdict.ALLOW
        return when (s.unknown) { Unknown.RING -> Verdict.ALLOW; Unknown.SILENCE -> Verdict.SILENCE; Unknown.BLOCK -> Verdict.REJECT }
    }

    /** A number can go to «Bloqueados»: it has digits and it is not a code like *611#. */
    fun canBlock(number: String) = Numbers.key(number).isNotEmpty() && !Dial.isCode(number)

    fun isBlocked(list: List<String>, number: String): Boolean {
        val k = Numbers.key(number)
        return k.isNotEmpty() && list.any { Numbers.key(it) == k }
    }

    /** The list with this number first (once: 099… and +593 99… are one), cleaned like the keypad does. */
    fun block(list: List<String>, number: String): List<String> {
        if (!canBlock(number)) return list
        return listOf(Dial.clean(number.trim())) + list.filter { Numbers.key(it) != Numbers.key(number) }
    }

    fun unblock(list: List<String>, number: String) = list.filter { Numbers.key(it) != Numbers.key(number) }

    /** One number per line in the settings. */
    fun encode(list: List<String>) = list.joinToString("\n")
    fun decode(text: String?) = text.orEmpty().split('\n').map { it.trim() }.filter { canBlock(it) }
}
