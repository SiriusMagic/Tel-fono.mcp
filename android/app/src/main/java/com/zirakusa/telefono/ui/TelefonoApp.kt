package com.zirakusa.telefono.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.telefono.R
import com.zirakusa.telefono.ui.screens.FavoritesScreen
import com.zirakusa.telefono.ui.screens.KeypadScreen
import com.zirakusa.telefono.ui.screens.McpScreen
import com.zirakusa.telefono.ui.screens.RecentsScreen
import com.zirakusa.telefono.ui.screens.SettingsScreen
import com.zirakusa.telefono.ui.theme.TelefonoColors

/** What the screens ask of the phone (the activity gives the real ones; tests leave the defaults). Each only after a tap. */
class AppActions(
    val allow: () -> Unit = {},
    val call: (String) -> Unit = {},
    val sms: (String) -> Unit = {},
    val addContact: (String) -> Unit = {},
    val copy: (String) -> Unit = {},
    val tone: (Char) -> Unit = {},
    val copyLink: (String) -> Unit = {},
    val goToHub: () -> Unit = {},
)

@Composable fun TelefonoApp(state: TelefonoState, actions: AppActions = AppActions()) {
    when (state.page) {
        Page.SETTINGS -> return SettingsScreen(state)
        Page.MCP -> return McpScreen(actions.copyLink, actions.goToHub) { state.page = Page.SETTINGS }
        Page.MAIN -> Unit
    }
    Box(Modifier.fillMaxSize().background(TelefonoColors.Background)) {
        when (state.tab) {
            Tab.RECENTS -> RecentsScreen(state, actions)
            Tab.KEYPAD -> KeypadScreen(state, actions)
            Tab.FAVORITES -> FavoritesScreen(state, actions)
        }
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 12.dp).clip(RoundedCornerShape(32.dp))
            .background(TelefonoColors.Card.copy(alpha = 0.96f)).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TabItem(Icons.Outlined.StarBorder, R.string.tab_favorites, state.tab == Tab.FAVORITES) { state.tab = Tab.FAVORITES }
            TabItem(Icons.Outlined.History, R.string.tab_recents, state.tab == Tab.RECENTS) { state.tab = Tab.RECENTS }
            TabItem(Icons.Outlined.Dialpad, R.string.tab_keypad, state.tab == Tab.KEYPAD) { state.tab = Tab.KEYPAD }
        }
    }
}

@Composable private fun TabItem(icon: ImageVector, label: Int, on: Boolean, onClick: () -> Unit) =
    Row(Modifier.clip(CircleShape).background(if (on) TelefonoColors.Accent else Color.Transparent).clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, if (on) null else stringResource(label), tint = if (on) TelefonoColors.OnAccent else TelefonoColors.Text)
        if (on) Text(stringResource(label), Modifier.padding(start = 8.dp), color = TelefonoColors.OnAccent, fontSize = 15.sp)
    }
