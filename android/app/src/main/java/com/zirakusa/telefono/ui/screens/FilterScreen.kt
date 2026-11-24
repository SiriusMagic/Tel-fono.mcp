package com.zirakusa.telefono.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Close
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import com.zirakusa.telefono.data.Filter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.telefono.R
import com.zirakusa.telefono.data.Numbers
import com.zirakusa.telefono.data.Unknown
import com.zirakusa.telefono.ui.AppActions
import com.zirakusa.telefono.ui.Page
import com.zirakusa.telefono.ui.Role
import com.zirakusa.telefono.ui.TelefonoState
import com.zirakusa.telefono.ui.theme.TelefonoColors

/** A page of Ajustes: back arrow and title, then its cards, scrolling. */
@Composable fun SubPage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.fillMaxSize().background(TelefonoColors.Background).systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PageTop(title, onBack)
        content()
        Text("", Modifier.padding(bottom = 16.dp))
    }

/** A grey paragraph under the cards. */
@Composable fun Note(text: String) = Text(text, Modifier.padding(horizontal = 20.dp), color = TelefonoColors.Muted, fontSize = 14.sp)

/**
 * «Filtro de llamadas no deseadas»: the call screening role (what it is, and the button to ask Android for it), hidden
 * numbers, numbers not in contacts and the «Bloqueados» list.
 */
@Composable fun FilterScreen(state: TelefonoState, actions: AppActions) = SubPage(stringResource(R.string.filter_title), { state.page = Page.SETTINGS }) {
    Group {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.role_title), fontSize = 18.sp)
            Text(stringResource(R.string.role_text), color = TelefonoColors.Muted, fontSize = 14.sp)
            when (state.role) {
                Role.ON -> Text(stringResource(R.string.role_on), color = TelefonoColors.AccentText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Role.OFF -> MainButton(stringResource(R.string.role_ask), Modifier.align(Alignment.CenterHorizontally), onClick = actions.askScreening)
                Role.UNSUPPORTED -> Text(stringResource(R.string.role_old), color = TelefonoColors.Muted, fontSize = 14.sp)
            }
        }
    }
    Group {
        SwitchItem(stringResource(R.string.block_hidden), stringResource(R.string.block_hidden_hint), state.blockHidden, onChange = state::changeBlockHidden)
        Divider()
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.unknown_title), fontSize = 18.sp)
            Text(stringResource(R.string.unknown_hint), color = TelefonoColors.Muted, fontSize = 14.sp)
            Segmented(listOf(R.string.unknown_ring, R.string.unknown_silence, R.string.unknown_block).map { stringResource(it) }, state.unknown.ordinal,
                Modifier.fillMaxWidth()) { state.changeUnknown(Unknown.entries[it]) }
        }
        Divider()
        Item(stringResource(R.string.blocked_title), null, value = if (state.blocked.isEmpty()) stringResource(R.string.blocked_none) else "${state.blocked.size}") {
            state.page = Page.BLOCKED
        }
    }
    Note(stringResource(R.string.filter_note))
}

/** «Bloqueados»: a number to add by hand, then each number (with its contact's name when saved) and «Desbloquear». */
@Composable fun BlockedScreen(state: TelefonoState) = SubPage(stringResource(R.string.blocked_title), { state.page = Page.FILTER }) {
    var number by remember { mutableStateOf("") }
    Group {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(number, { number = it.take(40) }, Modifier.weight(1f), singleLine = true, label = { Text(stringResource(R.string.blocked_add)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            RoundButton(Icons.Outlined.Block, stringResource(R.string.block), Modifier.padding(start = 10.dp), on = Filter.canBlock(number),
                enabled = Filter.canBlock(number)) { state.block(number); number = "" }
        }
    }
    Group {
        if (state.blocked.isEmpty()) Text(stringResource(R.string.blocked_empty), Modifier.padding(20.dp), color = TelefonoColors.Muted)
        state.blocked.forEachIndexed { i, n ->
            if (i > 0) Divider()
            val name = Numbers.nameOf(state.people, n)
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(name ?: Numbers.pretty(n), fontSize = 18.sp)
                    if (name != null) Text(Numbers.pretty(n), color = TelefonoColors.Muted, fontSize = 14.sp)
                }
                RoundButton(Icons.Outlined.Close, stringResource(R.string.unblock) + " " + (name ?: n)) { state.unblock(n) }
            }
        }
    }
}
