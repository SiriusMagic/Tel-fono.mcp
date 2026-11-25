package com.zirakusa.contactos.data

/** What the «Contactos» widget shows (pure, with tests): up to [MAX] favourites, how each is named and what a tap does. */
object WidgetPeople {
    const val MAX = 4

    /** The starred contacts in the list's order, at most [max]. */
    fun pick(items: List<Contact>, order: Order = Order.FIRST, max: Int = MAX): List<Contact> =
        Contacts.sorted(items.filter { it.starred }, order).take(max)

    /** The short name under the circle: the first word («Ana» for Ana Pérez), or the number. */
    fun short(c: Contact): String = c.name.trim().split(' ').firstOrNull { it.isNotBlank() } ?: c.shownName

    /** The number a tap gets ready in Teléfono (the first one), or null: then the tap opens the contact in Contactos. */
    fun number(c: Contact): String? = c.phones.firstOrNull { it.number.any(Char::isDigit) }?.number
}
