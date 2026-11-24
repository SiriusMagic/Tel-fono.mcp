package com.zirakusa.contactos.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.telefono.R
import com.zirakusa.contactos.data.Contact
import com.zirakusa.contactos.data.Label
import com.zirakusa.contactos.data.Phones
import com.zirakusa.contactos.ui.AppActions
import com.zirakusa.contactos.ui.ContactosState
import com.zirakusa.contactos.ui.Page
import com.zirakusa.contactos.ui.theme.ContactosColors
import java.time.format.DateTimeFormatter

/**
 * One contact: picture and name, Llamar · Mensaje · Correo · Compartir, then each number, email, birthday, address, note
 * and where it is saved. ⋮ has Favoritos, Compartir, Tono de llamada, Bloquear, Desvincular and Eliminar (the user only).
 */
@Composable fun DetailScreen(state: ContactosState, c: Contact, actions: AppActions) {
    var deleting by remember { mutableStateOf(false) }
    val me = state.isProfile(c)
    Sheet(state.name(c), onBack = { state.page = Page.LIST }, menu = { DetailMenu(state, c, actions, me) { deleting = true } }) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar(c, 96.dp)
            Text(state.name(c), fontSize = 26.sp, textAlign = TextAlign.Center)
            if (c.org.isNotBlank()) Text(c.org, color = ContactosColors.Muted)
            val phone = c.phones.firstOrNull()?.number; val mail = c.emails.firstOrNull()?.address
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Action(Icons.Outlined.Call, R.string.c_call, phone != null) { phone?.let(actions.call) }
                Action(Icons.Outlined.Sms, R.string.c_message, phone != null) { phone?.let(actions.sms) }
                Action(Icons.Outlined.Email, R.string.c_email, mail != null) { mail?.let(actions.email) }
                Action(Icons.Outlined.Share, R.string.c_share, true) { state.card(c) }
            }
            if (c.phones.isNotEmpty()) Group { c.phones.forEachIndexed { i, p -> if (i > 0) Divider(); Line(Phones.pretty(p.number), stringResource(labelName(p.label)), { actions.call(p.number) }) { actions.copy(p.number) } } }
            if (c.emails.isNotEmpty()) Group { c.emails.forEachIndexed { i, e -> if (i > 0) Divider(); Line(e.address, stringResource(labelName(e.label)), { actions.email(e.address) }) { actions.copy(e.address) } } }
            c.birthday?.let { b ->
                val next = b.next(state.today())
                Group { Line(DateTimeFormatter.ofPattern("d 'de' MMMM", locale()).format(next) + (b.year?.let { " de $it" } ?: ""),
                    stringResource(R.string.c_birthday) + (b.turns(state.today())?.let { " · " + stringResource(R.string.c_turns, it) } ?: ""), null, null) }
            }
            if (c.address.isNotBlank()) Group { Line(c.address, stringResource(R.string.c_address), { actions.map(c.address) }) { actions.copy(c.address) } }
            if (c.note.isNotBlank()) Group { Line(c.note, stringResource(R.string.c_note), null) { actions.copy(c.note) } }
            val where = c.raws.map { it.account }.distinct()
            if (where.isNotEmpty()) Group { Line(where.map { accountName(it) }.distinct().joinToString(" · "), stringResource(R.string.c_saved_in), null, null) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                if (!me) Action(if (c.starred) Icons.Filled.Star else Icons.Outlined.StarBorder, R.string.c_favorite, true, c.starred) { state.star(c) }
                Action(Icons.Outlined.Edit, R.string.c_edit, true) { if (me) actions.editProfile(c) else state.edit(c) }
            }
        }
    }
    if (deleting) AlertDialog({ deleting = false }, title = { Text(stringResource(R.string.c_delete_title, c.shownName)) }, text = { Text(stringResource(R.string.c_delete_text)) },
        confirmButton = { TextButton({ deleting = false; state.delete(c) }) { Text(stringResource(R.string.c_delete), color = ContactosColors.Danger) } },
        dismissButton = { TextButton({ deleting = false }) { Text(stringResource(R.string.c_cancel), color = ContactosColors.Text) } })
}

@Composable private fun Action(icon: ImageVector, label: Int, enabled: Boolean, on: Boolean = false, onClick: () -> Unit) =
    Column(Modifier.clickable(enabled, onClick = onClick).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        RoundButton(icon, stringResource(label), size = 54.dp, on = on, enabled = enabled, onClick = onClick)
        Text(stringResource(label), Modifier.padding(top = 4.dp), fontSize = 13.sp, color = if (enabled) ContactosColors.Text else ContactosColors.Muted)
    }

/** A line of the contact: the value big, what it is small; tap does the obvious thing, hold copies. */
@Composable private fun Line(value: String, what: String, onClick: (() -> Unit)?, onCopy: (() -> Unit)?) =
    Column(Modifier.fillMaxWidth().then(if (onClick != null || onCopy != null) Modifier.combinedClickableCompat(onClick, onCopy) else Modifier)
        .padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(value, fontSize = 17.sp)
        Text(what, color = ContactosColors.Muted, fontSize = 13.sp)
    }

private fun Modifier.combinedClickableCompat(onClick: (() -> Unit)?, onLong: (() -> Unit)?) =
    this.combinedClickable(onClick = { (onClick ?: onLong)?.invoke() }, onLongClick = onLong)

fun labelName(l: Label) = when (l) { Label.MOBILE -> R.string.c_l_mobile; Label.HOME -> R.string.c_l_home; Label.WORK -> R.string.c_l_work; Label.OTHER -> R.string.c_l_other }

@Composable fun locale(): java.util.Locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
