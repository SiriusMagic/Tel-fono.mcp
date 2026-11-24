package com.zirakusa.telefono.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.telefono.R
import com.zirakusa.telefono.data.Numbers
import com.zirakusa.telefono.data.SpeedDial
import com.zirakusa.telefono.ui.Page
import com.zirakusa.telefono.ui.TelefonoState
import com.zirakusa.telefono.ui.theme.TelefonoColors

/** «Marcación rápida»: 1 is the voicemail; 2–9 show their contact or number, tap to assign, ✕ to remove. */
@Composable fun SpeedScreen(state: TelefonoState) {
    state.assigning?.let { return SpeedPicker(state, it) }
    SubPage(stringResource(R.string.speed_title), { state.assigning = null; state.page = Page.SETTINGS }) {
        Note(stringResource(R.string.speed_hint))
        Group {
            KeyRow('1', stringResource(R.string.speed_voicemail), state.voicemail?.let(Numbers::pretty) ?: stringResource(R.string.speed_no_voicemail), null, null)
            SpeedDial.keys.forEach { k ->
                Divider()
                val s = SpeedDial.of(state.speed, k)
                val title = when { s == null -> stringResource(R.string.speed_empty); s.name.isNotBlank() -> s.name; else -> Numbers.pretty(s.number) }
                KeyRow(k, title, s?.takeIf { it.name.isNotBlank() }?.let { Numbers.pretty(it.number) }, { state.assigning = k },
                    if (s != null) ({ state.clearSpeed(k) }) else null)
            }
        }
    }
}

@Composable private fun KeyRow(key: Char, title: String, subtitle: String?, onClick: (() -> Unit)?, onRemove: (() -> Unit)?) =
    Row(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(TelefonoColors.Chip), contentAlignment = Alignment.Center) { Text("$key", fontSize = 20.sp) }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (onRemove == null && onClick != null) TelefonoColors.Muted else TelefonoColors.Text)
            if (subtitle != null) Text(subtitle, color = TelefonoColors.Muted, fontSize = 14.sp)
        }
        if (onRemove != null) RoundButton(Icons.Outlined.Close, stringResource(R.string.speed_remove) + " $key", onClick = onRemove)
    }

/** «Tecla 2»: search a contact by name or number, or use the number written. */
@Composable private fun SpeedPicker(state: TelefonoState, key: Char) = SubPage(stringResource(R.string.speed_key, "$key"), { state.assigning = null }) {
    var query by remember { mutableStateOf("") }
    val choices = SpeedDial.choices(state.people, query)
    OutlinedTextField(query, { query = it.take(40) }, Modifier.fillMaxWidth(), singleLine = true, shape = CircleShape,
        placeholder = { Text(stringResource(R.string.speed_search)) })
    if (choices.isEmpty()) Note(stringResource(R.string.speed_no_match))
    if (choices.isNotEmpty()) Group {
        choices.take(60).forEachIndexed { i, c ->
            if (i > 0) Divider()
            Column(Modifier.fillMaxWidth().clickable { state.setSpeed(key, c.number, c.name) }.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(c.name.ifBlank { stringResource(R.string.speed_use) }, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(Numbers.pretty(c.number), color = TelefonoColors.Muted, fontSize = 14.sp)
            }
        }
    }
}
