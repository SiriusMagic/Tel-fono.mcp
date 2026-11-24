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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zirakusa.telefono.BuildConfig
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.sp
import com.zirakusa.telefono.R
import com.zirakusa.telefono.data.SimChoice
import com.zirakusa.telefono.mcp.McpMode
import com.zirakusa.telefono.mcp.RelayLink
import com.zirakusa.telefono.ui.Page
import com.zirakusa.telefono.ui.TelefonoState
import com.zirakusa.telefono.ui.theme.TelefonoColors

/** «Ajustes»: direct calls, keypad tones, «SIM por defecto», Modo MCP, Temas, privacy and version. */
@Composable fun SettingsScreen(state: TelefonoState) {
    var privacy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(TelefonoColors.Background).systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PageTop(stringResource(R.string.settings)) { state.page = Page.MAIN }
        Group {
            SwitchItem(stringResource(R.string.direct), stringResource(R.string.direct_hint), state.direct, onChange = state::changeDirect)
            Divider()
            SwitchItem(stringResource(R.string.tones), null, state.tones, onChange = state::changeTones)
            Divider()
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.default_sim), fontSize = 18.sp)
                val sims = state.sims.map { simName(it) }
                Text(if (sims.size >= 2) sims.joinToString(", ") else stringResource(R.string.default_sim_hint), color = TelefonoColors.Muted, fontSize = 14.sp)
                Segmented(listOf(stringResource(R.string.ask), stringResource(R.string.sim_n, 1), stringResource(R.string.sim_n, 2)), state.defaultSim.ordinal,
                    Modifier.fillMaxWidth()) { state.changeSim(SimChoice.entries[it]) }
            }
        }
        Group {
            val status = when {
                !McpMode.active -> stringResource(R.string.mcp_status_off)
                McpMode.hubRedirect -> stringResource(R.string.mcp_redirect)
                McpMode.state == RelayLink.State.ONLINE -> stringResource(R.string.mcp_online)
                McpMode.state == RelayLink.State.CONNECTING -> stringResource(R.string.mcp_connecting)
                else -> stringResource(R.string.mcp_offline)
            }
            Item(stringResource(R.string.mcp_title), "$status · ${stringResource(R.string.mcp_settings_hint)}") { state.page = Page.MCP }
        }
        IconCard(TelefonoColors.Card, TelefonoColors.Text, TelefonoColors.Muted, TelefonoColors.Accent, androidx.compose.ui.graphics.Color(0xFF07160D))
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
}
