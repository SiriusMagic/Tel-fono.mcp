package com.zirakusa.contactos.ui.screens

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
import com.zirakusa.telefono.R
import com.zirakusa.contactos.ui.AppActions
import com.zirakusa.contactos.ui.Page
import com.zirakusa.contactos.ui.ContactosState
import com.zirakusa.contactos.ui.theme.ContactosColors

/** «Ajustes»: contacts (show, storage, duplicates, order, name, .vcf), Modo MCP, Temas, privacy and version. */
@Composable fun SettingsScreen(state: ContactosState, actions: AppActions) {
    var privacy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(ContactosColors.Background).systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PageTop(stringResource(R.string.c_settings)) { state.page = Page.LIST }
        ContactsSettings(state, actions)
        ThemeCard(state.themeMode, state.accent, state::changeTheme, state::changeAccent)
        com.zirakusa.contactos.ContactsIconCard(ContactosColors.Card, ContactosColors.Text, ContactosColors.Muted, ContactosColors.Accent)
        Caption(stringResource(R.string.c_about))
        Group {
            Item(stringResource(R.string.c_privacy), null) { privacy = true }
            Divider()
            Item(stringResource(R.string.c_version_title), BuildConfig.VERSION_NAME, onClick = null)
        }
        Text("", Modifier.padding(bottom = 16.dp))
    }
    if (privacy) AlertDialog({ privacy = false }, confirmButton = { TextButton({ privacy = false }) { Text(stringResource(R.string.c_done), color = ContactosColors.AccentText) } },
        title = { Text(stringResource(R.string.c_privacy)) }, text = { Text(stringResource(R.string.c_privacy_text)) })
}
