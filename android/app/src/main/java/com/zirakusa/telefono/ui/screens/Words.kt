package com.zirakusa.telefono.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zirakusa.telefono.R
import com.zirakusa.telefono.data.Ago
import com.zirakusa.telefono.data.CallType
import com.zirakusa.telefono.data.When
import com.zirakusa.telefono.ui.TelefonoState

/** «Justo ahora», «hace 5 min», «08:12 a. m.», «Ayer», «22/11». */
@Composable fun agoText(state: TelefonoState, at: Long): String = when (val a = When.ago(at, state.now(), state.zone)) {
    Ago.Now -> stringResource(R.string.just_now)
    is Ago.Minutes -> stringResource(R.string.minutes_ago, a.n.toInt())
    is Ago.Today -> When.clock(a.time, stringResource(R.string.am), stringResource(R.string.pm))
    Ago.Yesterday -> stringResource(R.string.yesterday)
    is Ago.Day -> When.shortDay(a.date, a.thisYear)
}

/** «12:30» today, «21 nov 4:08 p. m.» before. */
@Composable fun stampText(state: TelefonoState, at: Long): String = When.stamp(at, state.now(), state.zone,
    stringResource(R.string.months_short).split(','), stringResource(R.string.am), stringResource(R.string.pm))

/** «Llamada entrante», «Llamada saliente»… for a number's history. */
fun longTypeName(t: CallType) = when (t) {
    CallType.INCOMING -> R.string.h_incoming; CallType.OUTGOING -> R.string.h_outgoing; CallType.MISSED -> R.string.h_missed
    CallType.REJECTED -> R.string.h_rejected; CallType.BLOCKED -> R.string.h_blocked; CallType.VOICEMAIL -> R.string.t_voicemail
}
