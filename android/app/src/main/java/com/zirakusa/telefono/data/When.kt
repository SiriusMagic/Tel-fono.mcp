package com.zirakusa.telefono.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** How long ago a call was, as «Recientes» shows it. The screens turn it into words. */
sealed class Ago {
    /** Less than a minute: «Justo ahora». */
    data object Now : Ago()
    /** Less than an hour: «hace 5 min». */
    data class Minutes(val n: Long) : Ago()
    /** Earlier today: «08:12 a. m.». */
    data class Today(val time: LocalTime) : Ago()
    /** Yesterday: «Ayer». */
    data object Yesterday : Ago()
    /** Before: «22/11» (with the year when it isn't this year). */
    data class Day(val date: LocalDate, val thisYear: Boolean) : Ago()
}

/** Pure rules of times: how long ago, the 12-hour clock («08:12 a. m.») and the day-month labels. */
object When {
    fun ago(at: Long, now: Long, zone: ZoneId): Ago {
        val diff = now - at
        val then = Instant.ofEpochMilli(at).atZone(zone)
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return when {
            diff in 0 until 60_000 -> Ago.Now
            diff in 60_000 until 3_600_000 -> Ago.Minutes(diff / 60_000)
            then.toLocalDate() == today -> Ago.Today(then.toLocalTime())
            then.toLocalDate() == today.minusDays(1) -> Ago.Yesterday
            else -> Ago.Day(then.toLocalDate(), then.year == today.year)
        }
    }

    /** «08:12 a. m.», «4:08 p. m.» with [pad] off; [am]/[pm] are the words of the language. */
    fun clock(t: LocalTime, am: String, pm: String, pad: Boolean = true): String {
        val h = (t.hour % 12).let { if (it == 0) 12 else it }
        return (if (pad) "%02d:%02d %s" else "%d:%02d %s").format(h, t.minute, if (t.hour < 12) am else pm)
    }

    /** «22/11» or «22/11/25». */
    fun shortDay(d: LocalDate, thisYear: Boolean) = if (thisYear) "%02d/%02d".format(d.dayOfMonth, d.monthValue)
        else "%02d/%02d/%02d".format(d.dayOfMonth, d.monthValue, d.year % 100)

    /** A line of a number's history: today «12:30» (24 h like the clock of the reference), before «21 nov 4:08 p. m.». */
    fun stamp(at: Long, now: Long, zone: ZoneId, months: List<String>, am: String, pm: String): String {
        val then = Instant.ofEpochMilli(at).atZone(zone)
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        if (then.toLocalDate() == today) return "%02d:%02d".format(then.hour, then.minute)
        val day = "${then.dayOfMonth} ${months[then.monthValue - 1]}" + if (then.year != today.year) " ${then.year}" else ""
        return day + " " + clock(then.toLocalTime(), am, pm, pad = false)
    }
}
