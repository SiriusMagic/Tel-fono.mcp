package com.zirakusa.contactos.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.telefono.R
import com.zirakusa.contactos.data.Contact
import com.zirakusa.contactos.data.Phones
import com.zirakusa.contactos.ui.ContactosState
import com.zirakusa.contactos.ui.Page
import com.zirakusa.contactos.ui.theme.ContactosColors

/** «Selecciona un contacto para editar»: the same person in several accounts; only the part picked changes. */
@Composable fun PickScreen(state: ContactosState, c: Contact) = Sheet(stringResource(R.string.c_pick_title), onBack = { state.page = Page.DETAIL }) {
    val parts = remember(c) { state.parts(c) }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.c_pick_text), color = ContactosColors.Muted, fontSize = 15.sp)
        Group {
            parts.forEachIndexed { i, (raw, part) ->
                if (i > 0) Divider()
                Row(Modifier.fillMaxWidth().clickable { state.editPart(c, raw.id) }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(part.copy(raws = listOf(raw)))
                    Column(Modifier.padding(start = 14.dp).weight(1f)) {
                        Text(part.shownName.ifBlank { state.name(c) }, fontSize = 17.sp)
                        Text(listOfNotNull(accountName(raw.account), accountDetail(raw.account), part.phones.firstOrNull()?.let { Phones.pretty(it.number) })
                            .joinToString(" · "), color = ContactosColors.Muted, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
