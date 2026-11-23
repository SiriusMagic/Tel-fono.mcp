package com.zirakusa.telefono.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.telefono.R
import com.zirakusa.telefono.data.Call
import com.zirakusa.telefono.data.CallType
import com.zirakusa.telefono.data.Dial
import com.zirakusa.telefono.data.Numbers
import com.zirakusa.telefono.ui.AppActions
import com.zirakusa.telefono.ui.TelefonoState
import com.zirakusa.telefono.ui.dial
import com.zirakusa.telefono.ui.theme.TelefonoColors

/** One number of the page: «+593 99 018 4595 · Móvil» with Llamar, Mensaje and WhatsApp (when installed). */
@Composable fun NumberRow(state: TelefonoState, actions: AppActions, number: String, label: String) =
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 6.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(Numbers.pretty(number), fontSize = 19.sp, maxLines = 1)
            if (label.isNotBlank()) Text(label, color = TelefonoColors.Muted, fontSize = 14.sp)
        }
        Action(Icons.Outlined.Call, stringResource(R.string.call)) { state.dial(number, actions) }
        Action(Icons.Outlined.Sms, stringResource(R.string.message)) { actions.sms(number) }
        val wa = Numbers.wa(number)
        if (state.whatsapp && wa != null) Action(Icons.AutoMirrored.Outlined.Chat, stringResource(R.string.whatsapp)) { actions.whatsapp(wa) }
    }

@Composable private fun Action(icon: ImageVector, label: String, onClick: () -> Unit) =
    Box(Modifier.size(48.dp).clip(CircleShape).clickable(onClickLabel = label, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = TelefonoColors.AccentText)
    }

/** «Registro de llamadas»: the last calls with this number, five at first and more with «Ver más». */
@Composable fun HistoryList(state: TelefonoState, history: List<Call>) {
    var shown by rememberSaveable(state.detail) { mutableIntStateOf(5) }
    Text(stringResource(R.string.history), Modifier.padding(start = 4.dp, top = 20.dp, bottom = 6.dp), color = TelefonoColors.Muted, fontSize = 15.sp)
    if (history.isEmpty()) Text(stringResource(R.string.no_calls), Modifier.padding(4.dp), color = TelefonoColors.Muted)
    history.take(shown).forEach { HistoryRow(state, it) }
    if (history.size > shown) Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { shown += 10 }.padding(12.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.see_more), color = TelefonoColors.Muted, fontSize = 16.sp)
        Icon(Icons.Outlined.ExpandMore, null, tint = TelefonoColors.Muted)
    }
}

/** «12:30 · ↗ +593 99 018 4595 — Llamada saliente / No conectado» or «(1 m 38 s)». */
@Composable private fun HistoryRow(state: TelefonoState, c: Call) = Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 10.dp, bottom = 10.dp)) {
    Column(Modifier.weight(1f)) {
        Text(stampText(state, c.at), fontSize = 18.sp)
        Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(typeIcon(c.type), null, Modifier.size(18.dp), tint = if (c.type == CallType.MISSED) Missed else TelefonoColors.Muted)
            state.slotOf(c)?.let { SimBadge(it, Modifier.padding(start = 6.dp)) }
            Text(Numbers.pretty(c.number).ifBlank { stringResource(R.string.private_number) }, Modifier.padding(start = 6.dp), color = TelefonoColors.Muted, fontSize = 14.sp, maxLines = 1)
        }
    }
    Column(Modifier.padding(start = 12.dp), horizontalAlignment = Alignment.End) {
        Text(stringResource(longTypeName(c.type)), color = if (c.type == CallType.MISSED) Missed else TelefonoColors.Muted, fontSize = 14.sp, textAlign = TextAlign.End)
        val outcome = when { Dial.notConnected(c) -> stringResource(R.string.not_connected); c.seconds > 0 -> "(${Dial.spoken(c.seconds)})"; else -> null }
        if (outcome != null) Text(outcome, color = TelefonoColors.Muted, fontSize = 14.sp, textAlign = TextAlign.End)
    }
}
