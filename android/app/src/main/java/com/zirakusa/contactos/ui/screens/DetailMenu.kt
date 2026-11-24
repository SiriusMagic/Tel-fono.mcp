package com.zirakusa.contactos.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zirakusa.telefono.R
import com.zirakusa.contactos.data.Contact
import com.zirakusa.contactos.ui.AppActions
import com.zirakusa.contactos.ui.ContactosState
import com.zirakusa.contactos.ui.theme.ContactosColors

/** ⋮ of a contact. «Desvincular» only when it joins several parts; «Mi perfil» only shares. */
@Composable fun DetailMenu(state: ContactosState, c: Contact, actions: AppActions, me: Boolean, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    RoundButton(Icons.Outlined.MoreVert, stringResource(R.string.c_more), size = 52.dp) { open = true }
    DropdownMenu(open, { open = false }, containerColor = ContactosColors.Card) {
        @Composable fun entry(text: Int, color: androidx.compose.ui.graphics.Color = ContactosColors.Text, onClick: () -> Unit) =
            DropdownMenuItem({ Text(stringResource(text), color = color) }, { open = false; onClick() })
        if (!me) entry(R.string.c_favorites) { state.star(c) }
        entry(R.string.c_share) { state.card(c) }
        if (!me) {
            entry(R.string.c_ringtone) { actions.ringtone(c) }
            if (c.phones.isNotEmpty()) entry(R.string.c_block) { actions.block(c) }
            if (c.raws.size > 1) entry(R.string.c_unlink) { state.unlink(c); actions.toast(R.string.c_unlinked) }
            entry(R.string.c_delete_contact, ContactosColors.Danger, onDelete)
        }
    }
}
