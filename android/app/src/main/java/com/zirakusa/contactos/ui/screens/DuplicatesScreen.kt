package com.zirakusa.contactos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.telefono.R
import com.zirakusa.contactos.data.Phones
import com.zirakusa.contactos.ui.ContactosState
import com.zirakusa.contactos.ui.Page
import com.zirakusa.contactos.ui.theme.ContactosColors

/** «Contactos repetidos»: people saved twice (same number or same name). «Combinar» joins a group (nothing is deleted); tap one to open it. */
@Composable fun DuplicatesScreen(state: ContactosState) = Column(Modifier.fillMaxSize().background(ContactosColors.Background).systemBarsPadding()
    .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    PageTop(stringResource(R.string.c_duplicates)) { state.page = state.dupesBack }
    val groups = state.duplicates
    if (groups.isEmpty()) Text(stringResource(R.string.c_no_duplicates), Modifier.padding(16.dp), color = ContactosColors.Muted)
    else Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.c_merge_hint), Modifier.weight(1f).padding(start = 8.dp), color = ContactosColors.Muted, fontSize = 14.sp)
        if (groups.size > 1) TextButton({ groups.forEach(state::merge) }) { Text(stringResource(R.string.c_merge_all), color = ContactosColors.AccentText) }
    }
    groups.forEach { g ->
        Group {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(g.first().shownName, Modifier.weight(1f), color = ContactosColors.Muted, fontSize = 14.sp)
                TextButton({ state.merge(g) }) { Text(stringResource(R.string.c_merge), color = ContactosColors.AccentText) }
            }
            g.forEachIndexed { i, c ->
            if (i > 0) Divider()
            Row(Modifier.fillMaxWidth().clickable { state.open(c) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(c)
                Column(Modifier.padding(start = 14.dp)) {
                    Text(state.name(c), fontSize = 17.sp)
                    Text(c.phones.joinToString(" · ") { Phones.pretty(it.number) }, color = ContactosColors.Muted, fontSize = 13.sp)
                }
            }
        } }
    }
}
