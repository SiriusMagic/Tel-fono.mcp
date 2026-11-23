package com.zirakusa.telefono

import com.zirakusa.telefono.data.Call
import com.zirakusa.telefono.data.CallType
import com.zirakusa.telefono.data.Handle
import com.zirakusa.telefono.data.Person
import com.zirakusa.telefono.data.Sims
import com.zirakusa.telefono.data.SubInfo
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
        Call(1, "+593991234567", "", CallType.MISSED, at(21, 9, 0), label = "Ecuador"),
        Call(2, "0991234567", "Ana Pérez", CallType.MISSED, at(21, 9, 5), account = "2", label = "Móvil"),
        Call(3, "0987654321", "", CallType.OUTGOING, at(21, 8, 0), 125, account = "8959301", label = "Ecuador"),
        Call(4, "0991234567", "Ana Pérez", CallType.INCOMING, at(20, 18, 0), 3723),
        Call(5, "", "", CallType.MISSED, at(19, 7, 0)),
    )
    /** Dual SIM like the owner's phone: SIM 1 known by its ICCID (older phones), SIM 2 by its subscription id. */
    val sims = Sims.match(listOf(Handle("8959301"), Handle("2")), listOf(SubInfo(0, 1, "8959301", "Claro"), SubInfo(1, 2, "", "Movistar")))
}
