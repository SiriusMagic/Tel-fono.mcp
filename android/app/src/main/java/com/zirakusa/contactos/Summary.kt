package com.zirakusa.contactos

import android.content.Context
import com.zirakusa.telefono.R

import com.zirakusa.contactos.data.Contact
import com.zirakusa.contactos.data.Contacts
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** The lines of «Hoy» for the family summary: birthdays of the next 7 days («Hoy · Ana Pérez (35)»). Pure. */
object SummaryLines {
    const val MAX = 5
    fun lines(items: List<Contact>, today: LocalDate, day: (Int) -> String): List<String> =
        Contacts.upcoming(items, today, 7).take(MAX).map { (c, d) ->
            "${day(ChronoUnit.DAYS.between(today, d).toInt())} · ${c.shownName}" + (c.birthday?.turns(today)?.let { " ($it)" } ?: "")
        }
}

/** Upcoming birthdays for Teléfono's «Hoy» summary (Contactos lives inside Teléfono): only names and days, never numbers. */
object Birthdays {
    fun lines(context: Context): List<String> {
        val items = runCatching { AndroidStore(context).list() }.getOrDefault(emptyList())
        val r = context.resources
        return SummaryLines.lines(items, LocalDate.now()) { d ->
            when (d) { 0 -> r.getString(R.string.c_today); 1 -> r.getString(R.string.c_tomorrow); else -> r.getQuantityString(R.plurals.c_in_days, d, d) }
        }
    }
}
