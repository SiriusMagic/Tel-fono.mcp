package com.zirakusa.contactos.data

/** «Importar/exportar contactos» as .vcf. Pure: the activity only reads and writes the file the user picked. */
object Transfer {
    /** What an import did: added, and already in the phone (skipped). */
    data class Result(val added: Int, val skipped: Int)

    const val MAX_CARDS = 5000

    fun export(items: List<Contact>) = VCard.buildAll(Contacts.sorted(items))

    /** Adds each card of [text] into [into] (the phone when null), skipping the ones already there. Never deletes. */
    fun import(store: ContactStore, text: String, into: Account?): Result {
        val cards = VCard.parseAll(text).take(MAX_CARDS)
        val have = store.list().toMutableList()
        var added = 0
        for (c in cards) {
            if (Contacts.known(have, c)) continue
            have += store.save(c.copy(id = 0, starred = false, raws = listOf(Raw(0, into ?: Account.PHONE))))
            added++
        }
        return Result(added, cards.size - added)
    }
}
