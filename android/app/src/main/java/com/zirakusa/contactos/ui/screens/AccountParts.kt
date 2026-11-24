package com.zirakusa.contactos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.zirakusa.telefono.R
import com.zirakusa.contactos.data.Account
import com.zirakusa.contactos.data.AccountKind
import com.zirakusa.contactos.ui.theme.ContactosColors

/** «Teléfono», «SIM 1», «Google»… */
@Composable fun accountName(a: Account): String = when (a.kind) {
    AccountKind.PHONE -> stringResource(R.string.c_acc_phone)
    AccountKind.SIM -> a.slot?.let { stringResource(R.string.c_acc_sim_n, it) } ?: a.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.c_acc_sim)
    AccountKind.GOOGLE -> stringResource(R.string.c_acc_google)
    else -> a.name?.takeIf { it.isNotBlank() } ?: a.type.orEmpty()
}

/** The account's address under its name (the Google email), when it has one worth showing. */
fun accountDetail(a: Account): String? = a.name?.takeIf { a.kind == AccountKind.GOOGLE && it.isNotBlank() }

/** Pick one account (or «Todos», the null option, when [all] is given). */
@Composable fun AccountDialog(title: String, accounts: List<Account>, selected: Account?, all: String? = null, onCancel: () -> Unit, onPick: (Account?) -> Unit) =
    Dialog(onCancel) {
        Column(Modifier.clip(RoundedCornerShape(28.dp)).background(ContactosColors.Card).padding(vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, Modifier.align(Alignment.CenterHorizontally).padding(horizontal = 20.dp, vertical = 8.dp), fontSize = 20.sp)
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (all != null) ChoiceRow(all, selected == null) { onPick(null) }
                accounts.forEach { a -> ChoiceRow(accountName(a), a == selected, subtitle = accountDetail(a)) { onPick(a) } }
            }
            TextButton(onCancel, Modifier.align(Alignment.End).padding(end = 12.dp)) { Text(stringResource(R.string.c_cancel), color = ContactosColors.Text) }
        }
    }
