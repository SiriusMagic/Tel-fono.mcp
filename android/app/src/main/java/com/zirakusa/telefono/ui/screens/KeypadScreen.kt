package com.zirakusa.telefono.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.telefono.R
import com.zirakusa.telefono.data.Numbers
import com.zirakusa.telefono.ui.AppActions
import com.zirakusa.telefono.ui.TelefonoState
import com.zirakusa.telefono.ui.theme.TelefonoColors

private val keys = listOf('1' to "", '2' to "ABC", '3' to "DEF", '4' to "GHI", '5' to "JKL", '6' to "MNÑO", '7' to "PQRS", '8' to "TUV", '9' to "WXYZ", '*' to "", '0' to "+", '#' to "")

/** «Teclado»: the number big, contacts that match (by letters or digits), the 12 keys, WhatsApp and the call button (one per SIM). */
@OptIn(ExperimentalFoundationApi::class)
@Composable fun KeypadScreen(state: TelefonoState, actions: AppActions) = Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp).padding(bottom = 96.dp),
    horizontalAlignment = Alignment.CenterHorizontally) {
    Column(Modifier.weight(1f).fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        state.suggestions.forEach { p ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { state.typed = p.numbers.first() }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(p.name, Modifier.weight(1f), fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(Numbers.pretty(p.numbers.first()), color = TelefonoColors.Muted, fontSize = 14.sp)
            }
        }
    }
    Text(state.typed.ifEmpty { " " }, Modifier.fillMaxWidth().padding(vertical = 12.dp), fontSize = if (state.typed.length > 13) 28.sp else 38.sp,
        textAlign = TextAlign.Center, maxLines = 1, fontWeight = FontWeight.Light)
    keys.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            row.forEach { (k, letters) ->
                Column(Modifier.size(78.dp).clip(CircleShape).background(TelefonoColors.Card)
                    .combinedClickable(onClick = { state.press(k); actions.tone(k) }, onLongClick = if (k == '0') ({ state.plus() }) else null)
                    .semantics { contentDescription = "$k" }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("$k", fontSize = 30.sp)
                    if (letters.isNotEmpty()) Text(letters, fontSize = 11.sp, color = TelefonoColors.Muted)
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        val wa = Numbers.wa(state.typed)
        if (state.whatsapp && wa != null) Box(Modifier.size(64.dp).clip(CircleShape).background(TelefonoColors.Accent)
            .clickable(onClickLabel = stringResource(R.string.whatsapp)) { actions.whatsapp(wa) }, contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Outlined.Chat, stringResource(R.string.whatsapp), Modifier.size(28.dp), tint = TelefonoColors.OnAccent)
        } else Spacer(Modifier.size(64.dp))
        CallPill(state, actions, state.typed)
        Box(Modifier.size(78.dp).clip(CircleShape).combinedClickable(onClick = state::backspace, onLongClick = state::clearTyped)
            .semantics { contentDescription = "⌫" }, contentAlignment = Alignment.Center) {
            if (state.typed.isNotEmpty()) Icon(Icons.AutoMirrored.Outlined.Backspace, stringResource(R.string.delete), tint = TelefonoColors.Muted)
        }
    }
    Spacer(Modifier.height(8.dp))
}
