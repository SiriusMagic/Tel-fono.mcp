package com.zirakusa.telefono

import com.zirakusa.telefono.data.Call
import com.zirakusa.telefono.data.CallType
import com.zirakusa.telefono.data.Person
import java.time.LocalDateTime
import java.time.ZoneOffset

/** The same calls and people for every test. Today is Saturday 21 November 2026, in UTC. */
object Book {
    fun at(day: Int, h: Int, m: Int = 0) = LocalDateTime.of(2026, 11, day, h, m).toInstant(ZoneOffset.UTC).toEpochMilli()
    val ana = Person(1, "Ana Pérez", listOf("0991234567"), starred = true)
    val nunez = Person(2, "Álvaro Núñez", listOf("+593 98 765 4321"))
    val pizza = Person(3, "Pizzería Ñaño", listOf("022345678"), starred = true)
    val people = listOf(ana, nunez, pizza)
    val calls = listOf(
        Call(1, "+593991234567", "", CallType.MISSED, at(21, 9, 0)),
        Call(2, "0991234567", "Ana Pérez", CallType.MISSED, at(21, 9, 5)),
        Call(3, "0987654321", "", CallType.OUTGOING, at(21, 8, 0), 125),
        Call(4, "0991234567", "Ana Pérez", CallType.INCOMING, at(20, 18, 0), 3723),
        Call(5, "", "", CallType.MISSED, at(19, 7, 0)),
    )
}
