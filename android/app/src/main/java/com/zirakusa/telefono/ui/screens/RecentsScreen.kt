package com.zirakusa.telefono.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallMade
import androidx.compose.material.icons.automirrored.outlined.CallMissed
import androidx.compose.material.icons.automirrored.outlined.CallReceived
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Voicemail
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.telefono.R
import com.zirakusa.telefono.data.CallType
import com.zirakusa.telefono.data.Group
import com.zirakusa.telefono.data.Numbers
import com.zirakusa.telefono.ui.AppActions
import com.zirakusa.telefono.ui.Page
import com.zirakusa.telefono.ui.TelefonoState
import com.zirakusa.telefono.ui.dial
import com.zirakusa.telefono.ui.theme.TelefonoColors

val Missed = Color(0xFFFF6B6B)

/** «Recientes»: big title, Todo · Llamadas perdidas, each line (calls in a row with the same number together) with its SIM and ⓘ. */
@Composable fun RecentsScreen(state: TelefonoState, actions: AppActions) = Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp)) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
        RoundButton(Icons.Outlined.Settings, stringResource(R.string.settings)) { state.page = Page.SETTINGS }
    }
    Text(stringResource(R.string.tab_recents), Modifier.padding(start = 8.dp, bottom = 8.dp), fontSize = 40.sp)
    if (!state.allowed) return@Column Group { Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Outlined.Call, null, Modifier.size(48.dp), tint = TelefonoColors.AccentText)
        Text(stringResource(R.string.allow_text), color = TelefonoColors.Muted, textAlign = TextAlign.Center)
        MainButton(stringResource(R.string.allow), onClick = actions.allow)
    } }
    UnderTabs(listOf(stringResource(R.string.all), stringResource(R.string.missed_tab)), if (state.onlyMissed) 1 else 0, if (state.missedToday > 0) 1 else -1) {
        state.onlyMissed = it == 1
    }
    val groups = state.groups
    if (groups.isEmpty()) Text(stringResource(R.string.no_calls), Modifier.padding(24.dp), color = TelefonoColors.Muted)
    LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)) { items(groups, key = { it.first.id }) { GroupRow(it, state, actions) } }
}

/** Tabs with a short accent line under the chosen one; [dot] marks one with something new (missed calls today). */
@Composable private fun UnderTabs(options: List<String>, selected: Int, dot: Int, onSelect: (Int) -> Unit) = Row(Modifier.fillMaxWidth()) {
    options.forEachIndexed { i, label ->
        Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable { onSelect(i) }.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontSize = 17.sp, maxLines = 1, color = if (i == selected) TelefonoColors.Text else TelefonoColors.Muted,
                    fontWeight = if (i == selected) FontWeight.Medium else FontWeight.Normal)
                if (i == dot) Box(Modifier.padding(start = 6.dp).size(7.dp).clip(CircleShape).background(Missed))
            }
            Box(Modifier.padding(top = 8.dp).size(width = 36.dp, height = 3.dp).clip(CircleShape).background(if (i == selected) TelefonoColors.Accent else Color.Transparent))
        }
    }
}

@Composable private fun GroupRow(g: Group, state: TelefonoState, actions: AppActions) = Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
    .background(if (state.open == g.first.id) TelefonoColors.Card else Color.Transparent)) {
    val c = g.first
    val name = state.nameOf(c)
    Row(Modifier.fillMaxWidth().clickable { state.toggle(c.id) }.padding(start = 8.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(typeIcon(c.type), stringResource(typeName(c.type)), Modifier.size(22.dp), tint = if (c.type == CallType.MISSED) Missed else TelefonoColors.Muted)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text((name.ifBlank { Numbers.pretty(c.number).ifBlank { stringResource(R.string.private_number) } }) + if (g.count > 1) " (${g.count})" else "",
                fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (g.missed) Missed else TelefonoColors.Text)
            val sub = c.label.ifBlank { if (name.isNotBlank()) Numbers.pretty(c.number) else "" }
            val slot = state.slotOf(c)
            if (sub.isNotBlank() || slot != null) Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                if (slot != null) SimBadge(slot, Modifier.padding(end = 5.dp))
                Text(sub, color = TelefonoColors.Muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(agoText(state, c.at), Modifier.padding(start = 8.dp), color = TelefonoColors.Muted, fontSize = 14.sp, maxLines = 1)
        Box(Modifier.size(48.dp).clip(CircleShape).clickable(onClickLabel = stringResource(R.string.info)) { state.openDetail(c.number) },
            contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Info, stringResource(R.string.info), tint = TelefonoColors.Text) }
    }
    if (state.open == c.id && c.number.isNotBlank()) Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        Small(Icons.Outlined.Call, R.string.call) { state.dial(c.number, actions) }
        Small(Icons.Outlined.Sms, R.string.message) { actions.sms(c.number) }
        Small(Icons.Outlined.ContentCopy, R.string.copy) { actions.copy(c.number) }
        if (name.isBlank()) Small(Icons.Outlined.PersonAdd, R.string.add_contact) { actions.addContact(c.number) }
    }
}

@Composable private fun Small(icon: ImageVector, label: Int, onClick: () -> Unit) =
    Column(Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, stringResource(label), tint = TelefonoColors.Text)
        Text(stringResource(label), fontSize = 12.sp, color = TelefonoColors.Muted)
    }

fun typeIcon(t: CallType): ImageVector = when (t) {
    CallType.INCOMING -> Icons.AutoMirrored.Outlined.CallReceived; CallType.OUTGOING -> Icons.AutoMirrored.Outlined.CallMade
    CallType.MISSED -> Icons.AutoMirrored.Outlined.CallMissed; CallType.REJECTED -> Icons.Outlined.CallEnd
    CallType.BLOCKED -> Icons.Outlined.Block; CallType.VOICEMAIL -> Icons.Outlined.Voicemail
}

fun typeName(t: CallType) = when (t) {
    CallType.INCOMING -> R.string.t_incoming; CallType.OUTGOING -> R.string.t_outgoing; CallType.MISSED -> R.string.t_missed
    CallType.REJECTED -> R.string.t_rejected; CallType.BLOCKED -> R.string.t_blocked; CallType.VOICEMAIL -> R.string.t_voicemail
}
