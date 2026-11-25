package com.zirakusa.telefono.ui.screens

import com.zirakusa.telefono.IconCard
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zirakusa.telefono.BuildConfig
import androidx.compose.ui.unit.sp
import com.zirakusa.telefono.R
import com.zirakusa.telefono.data.AnnounceMode
import com.zirakusa.telefono.data.SimChoice
import com.zirakusa.telefono.mcp.McpMode
import com.zirakusa.telefono.mcp.RelayLink
import com.zirakusa.telefono.ui.AppActions
import com.zirakusa.telefono.ui.Page
import com.zirakusa.telefono.ui.Role
import com.zirakusa.telefono.ui.SystemPage
import com.zirakusa.telefono.ui.TelefonoState
import com.zirakusa.telefono.ui.theme.TelefonoColors

/**
 * «Ajustes»: direct calls and keypad tones; «Llamadas» (filter, voice announcement, speed dial, carrier, why there is no
 * recording); Android's own pages; Modo MCP, icon, Temas, privacy and version.
 */
@Composable fun SettingsScreen(state: TelefonoState, actions: AppActions) {
    var privacy by remember { mutableStateOf(false) }
    var announce by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(TelefonoColors.Background).systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PageTop(stringResource(R.string.settings)) { state.page = Page.MAIN }
        Group {
            SwitchItem(stringResource(R.string.direct), stringResource(R.string.direct_hint), state.direct, onChange = state::changeDirect)
            Divider()
            SwitchItem(stringResource(R.string.tones), null, state.tones, onChange = state::changeTones)
        }
        Caption(stringResource(R.string.calls_caption))
        Group {
            Item(stringResource(R.string.filter_title), stringResource(when (state.role) {
                Role.ON -> R.string.filter_on; Role.OFF -> R.string.filter_off; Role.UNSUPPORTED -> R.string.filter_unsupported
            })) { state.page = Page.FILTER }
            Divider()
            Item(stringResource(R.string.announce_title), stringResource(announceName(state.announce)), expand = true) { announce = true }
            Divider()
            Item(stringResource(R.string.speed_title), if (state.speed.isEmpty()) stringResource(R.string.speed_none)
                else stringResource(R.string.speed_count, state.speed.size)) { state.page = Page.SPEED }
            Divider()
            Item(stringResource(R.string.carrier_title), stringResource(R.string.make_calls_with) + ": " + choiceName(state.defaultSim)) { state.page = Page.CARRIER }
            Divider()
            Item(stringResource(R.string.recording_title), stringResource(R.string.recording_text), onClick = null)
        }
        Caption(stringResource(R.string.android_caption))
        Group {
            Item(stringResource(R.string.sys_sim), stringResource(R.string.sys_sim_hint)) { actions.system(SystemPage.SIM_NETWORK) }
            Divider()
            Item(stringResource(R.string.sys_sounds), stringResource(R.string.sys_sounds_hint)) { actions.system(SystemPage.SOUNDS) }
            Divider()
            Item(stringResource(R.string.sys_calls), stringResource(R.string.sys_calls_hint)) { actions.system(SystemPage.CALLS) }
        }
        Group {
            val status = when {
                !McpMode.remote -> stringResource(R.string.mcp_status_off)
                McpMode.state == RelayLink.State.ONLINE -> stringResource(R.string.mcp_online)
                McpMode.state == RelayLink.State.CONNECTING -> stringResource(R.string.mcp_connecting)
                else -> stringResource(R.string.mcp_offline)
            }
            Item(stringResource(R.string.mcp_title), "$status · ${stringResource(R.string.mcp_settings_hint)}", onClick = actions.openArka)
        }
        IconCard(TelefonoColors.Card, TelefonoColors.Text, TelefonoColors.Muted, TelefonoColors.Accent)
        ThemeCard(state.themeMode, state.accent, state::changeTheme, state::changeAccent)
        Caption(stringResource(R.string.about))
        Group {
            Item(stringResource(R.string.privacy), null) { privacy = true }
            Divider()
            Item(stringResource(R.string.version_title), BuildConfig.VERSION_NAME, onClick = null)
        }
        Text("", Modifier.padding(bottom = 16.dp))
    }
    if (privacy) AlertDialog({ privacy = false }, confirmButton = { TextButton({ privacy = false }) { Text(stringResource(R.string.done), color = TelefonoColors.AccentText) } },
        title = { Text(stringResource(R.string.privacy)) }, text = { Text(stringResource(R.string.privacy_text)) })
    if (announce) AnnounceDialog(state.announce, { announce = false }) { state.changeAnnounce(it); announce = false }
}

@Composable private fun choiceName(c: SimChoice) = when (c) {
    SimChoice.ASK -> stringResource(R.string.ask); SimChoice.SIM1 -> stringResource(R.string.sim_n, 1); SimChoice.SIM2 -> stringResource(R.string.sim_n, 2)
}

fun announceName(m: AnnounceMode) = when (m) {
    AnnounceMode.NEVER -> R.string.announce_never; AnnounceMode.ALWAYS -> R.string.announce_always; AnnounceMode.HEADPHONES -> R.string.announce_headphones
}

/** «Anuncio de voz de llamada entrante»: Nunca · Siempre · Solo con auriculares, with what it does. */
@Composable private fun AnnounceDialog(mode: AnnounceMode, onClose: () -> Unit, onPick: (AnnounceMode) -> Unit) = Dialog(onClose) {
    Column(Modifier.clip(RoundedCornerShape(28.dp)).background(TelefonoColors.Card).padding(vertical = 20.dp)) {
        Text(stringResource(R.string.announce_title), Modifier.padding(horizontal = 24.dp, vertical = 8.dp), fontSize = 20.sp)
        AnnounceMode.entries.forEach { m -> ChoiceRow(stringResource(announceName(m)), m == mode) { onPick(m) } }
        Text(stringResource(R.string.announce_hint), Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = TelefonoColors.Muted, fontSize = 14.sp)
    }
}
