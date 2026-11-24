package com.zirakusa.contactos

import com.zirakusa.contactos.data.Birthday
import com.zirakusa.contactos.data.Contact
import com.zirakusa.contactos.data.Email
import com.zirakusa.contactos.data.Label
import com.zirakusa.contactos.data.Phone

/** The same few people for every test. Today is Saturday 21 November 2026. */
object People {
    val ana = Contact(1, "Ana Pérez", listOf(Phone("0991234567")), listOf(Email("ana@correo.ec")), "Zirakusa", birthday = Birthday(11, 23, 1991), starred = true)
    val alvaro = Contact(2, "Álvaro Núñez", listOf(Phone("+593 98 765 4321", Label.WORK)), birthday = Birthday(11, 21))
    val nina = Contact(3, "Ñusta Quispe", listOf(Phone("022345678", Label.HOME)))
    val ana2 = Contact(4, "Anita P.", listOf(Phone("+593991234567")))
    val nine = Contact(5, "9 de Octubre Pizzería", listOf(Phone("042000000")))
    val carlos = Contact(6, "Carlos Mora", emails = listOf(Email("carlos@trabajo.com", Label.WORK)), birthday = Birthday(2, 29, 2000))
    val all = listOf(ana, alvaro, nina, ana2, nine, carlos)
}
