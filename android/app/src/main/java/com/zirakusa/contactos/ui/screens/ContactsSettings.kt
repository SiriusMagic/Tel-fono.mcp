package com.zirakusa.contactos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.telefono.R
import com.zirakusa.contactos.data.NameFormat
import com.zirakusa.contactos.data.Order
import com.zirakusa.contactos.ui.AppActions
import com.zirakusa.contactos.ui.ContactosState
import com.zirakusa.contactos.ui.Page

/** Ajustes › Contactos: which to show, where new ones go, duplicates, order, name format, .vcf and blocked numbers. */
@Composable fun ContactsSettings(state: ContactosState, actions: AppActions) {
    var picking by remember { mutableStateOf<Int?>(null) }
    Group {
        Item(stringResource(R.string.c_show_contacts), state.showOnly?.let { accountName(it) } ?: stringResource(R.string.c_show_all), expand = true) { picking = R.string.c_show_contacts }
        Divider()
        Item(stringResource(R.string.c_default_storage), listOfNotNull(accountName(state.storage), accountDetail(state.storage)).joinToString(" · "), expand = true) {
            picking = R.string.c_default_storage
        }
    }
    Group {
        Item(stringResource(R.string.c_duplicates), state.duplicates.size.toString(), onClick = { state.dupesBack = Page.SETTINGS; state.page = Page.DUPLICATES })
        Divider()
        SwitchItem(stringResource(R.string.c_hide_dups), stringResource(R.string.c_hide_dups_hint), state.hideDuplicates, onChange = state::changeHideDuplicates)
    }
    Group {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.c_sort_by), fontSize = 18.sp)
            Segmented(listOf(stringResource(R.string.c_first_name), stringResource(R.string.c_last_name)), state.order.ordinal, Modifier.fillMaxWidth()) {
                state.changeOrder(Order.entries[it])
            }
            Text(stringResource(R.string.c_name_format), Modifier.padding(top = 6.dp), fontSize = 18.sp)
            Segmented(listOf(stringResource(R.string.c_first_last), stringResource(R.string.c_last_first)), state.format.ordinal, Modifier.fillMaxWidth()) {
                state.changeFormat(NameFormat.entries[it])
            }
        }
    }
    Group {
        Item(stringResource(R.string.c_import_cards), null, onClick = actions.importCards)
        Divider()
        Item(stringResource(R.string.c_export_cards), null, onClick = actions.exportCards)
        Divider()
        Item(stringResource(R.string.c_blocked_numbers), stringResource(R.string.c_blocked_numbers_hint), onClick = actions.blockedNumbers)
    }
    Group { Item(stringResource(R.string.c_widget_add), stringResource(R.string.c_widget_add_hint), onClick = actions.addWidget) }
    when (picking) {
        R.string.c_show_contacts -> AccountDialog(stringResource(R.string.c_show_contacts), state.accounts, state.showOnly, stringResource(R.string.c_show_all), { picking = null }) {
            picking = null; state.changeShowOnly(it)
        }
        R.string.c_default_storage -> AccountDialog(stringResource(R.string.c_default_storage), state.accounts, state.storage, onCancel = { picking = null }) {
            picking = null; if (it != null) state.changeStorage(it)
        }
    }
}
