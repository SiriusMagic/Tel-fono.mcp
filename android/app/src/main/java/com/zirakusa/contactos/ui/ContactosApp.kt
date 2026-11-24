package com.zirakusa.contactos.ui

import androidx.compose.runtime.Composable
import com.zirakusa.contactos.data.Contact
import com.zirakusa.contactos.ui.screens.CardScreen
import com.zirakusa.contactos.ui.screens.DetailScreen
import com.zirakusa.contactos.ui.screens.DuplicatesScreen
import com.zirakusa.contactos.ui.screens.EditScreen
import com.zirakusa.contactos.ui.screens.ListScreen
import com.zirakusa.contactos.ui.screens.PickScreen
import com.zirakusa.contactos.ui.screens.SettingsScreen

/** What the screens ask of the phone (the activity gives the real ones; tests leave the defaults). Each only after a tap. */
class AppActions(
    val allow: () -> Unit = {},
    /** Opens the dialer with the number (the call starts only when the user presses call there or in Teléfono). */
    val call: (String) -> Unit = {},
    val sms: (String) -> Unit = {},
    val email: (String) -> Unit = {},
    val map: (String) -> Unit = {},
    /** «Compartir» › Tarjeta (.vcf), Imagen, Texto; «Guardar» puts the card's image in Pictures. */
    val share: (Contact) -> Unit = {},
    val shareImage: (Contact) -> Unit = {},
    val shareText: (Contact) -> Unit = {},
    val saveImage: (Contact) -> Unit = {},
    /** Android's sound picker; the choice goes to [ContactosState.setRingtone]. */
    val ringtone: (Contact) -> Unit = {},
    /** Blocks the numbers if Android lets Contactos, else opens the phone's blocked list. */
    val block: (Contact) -> Unit = {},
    val blockedNumbers: () -> Unit = {},
    /** «Mi perfil» is edited in Android's own editor. */
    val editProfile: (Contact) -> Unit = {},
    val importCards: () -> Unit = {},
    val exportCards: () -> Unit = {},
    val toast: (Int) -> Unit = {},
    val copy: (String) -> Unit = {},
)

@Composable fun ContactosApp(state: ContactosState, actions: AppActions = AppActions()) = when (state.page) {
    Page.LIST -> ListScreen(state, actions)
    Page.DETAIL -> state.shown?.let { DetailScreen(state, it, actions) } ?: ListScreen(state, actions)
    Page.EDIT -> EditScreen(state)
    Page.PICK -> state.shown?.let { PickScreen(state, it) } ?: ListScreen(state, actions)
    Page.CARD -> state.shown?.let { CardScreen(state, it, actions) } ?: ListScreen(state, actions)
    Page.DUPLICATES -> DuplicatesScreen(state)
    Page.SETTINGS -> SettingsScreen(state, actions)
}
