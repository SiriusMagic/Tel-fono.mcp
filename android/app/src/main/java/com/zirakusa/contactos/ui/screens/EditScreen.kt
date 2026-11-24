package com.zirakusa.contactos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zirakusa.telefono.R
import com.zirakusa.contactos.ui.ContactosState
import com.zirakusa.contactos.ui.Draft
import com.zirakusa.contactos.ui.theme.ContactosColors

/** «Nuevo contacto» / «Editar»: name, numbers and emails (a new empty line appears as you fill the last), company, birthday, address, note. */
@Composable fun EditScreen(state: ContactosState) = Sheet(stringResource(if (state.draft.id == 0L) R.string.c_new_contact else R.string.c_edit), onClose = state::cancelEdit) {
    val d = state.draft
    fun set(next: Draft) { state.draft = next }
    var picking by remember { mutableStateOf(false) }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // «Guardar en»: a new contact picks its account; an edit changes only the part it opened.
        if (d.id == 0L || state.shown?.raws.orEmpty().size > 1) Group {
            Item(stringResource(R.string.c_save_in), listOfNotNull(accountName(d.account), accountDetail(d.account)).joinToString(" · "),
                onClick = if (d.id == 0L && state.accounts.size > 1) ({ picking = true }) else null)
        }
        Field(R.string.c_name, d.name) { set(d.copy(name = it.take(100))) }
        d.phones.forEachIndexed { i, p -> Field(R.string.c_phone, p, KeyboardType.Phone) { v -> set(d.copy(phones = grow(d.phones, i, v.take(40)))) } }
        d.emails.forEachIndexed { i, e -> Field(R.string.c_email, e, KeyboardType.Email) { v -> set(d.copy(emails = grow(d.emails, i, v.take(100)))) } }
        Field(R.string.c_company, d.org) { set(d.copy(org = it.take(100))) }
        Field(R.string.c_birthday_hint, d.birthday, KeyboardType.Number) { set(d.copy(birthday = it.take(10))) }
        Field(R.string.c_address, d.address) { set(d.copy(address = it.take(200))) }
        Field(R.string.c_note, d.note, lines = 3) { set(d.copy(note = it.take(500))) }
        when (d.problem) {
            Draft.Problem.EMPTY -> Text(stringResource(R.string.c_need_name), color = ContactosColors.Muted)
            Draft.Problem.BIRTHDAY -> Text(stringResource(R.string.c_bad_birthday), color = ContactosColors.Danger)
            null -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { MainButton(stringResource(R.string.c_save)) { state.save() } }
        }
        TextButton(state::cancelEdit, Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.c_cancel), color = ContactosColors.Text) }
    }
    if (picking) AccountDialog(stringResource(R.string.c_save_in), state.accounts, d.account, onCancel = { picking = false }) { a ->
        picking = false; if (a != null) set(d.copy(account = a))
    }
}

/** Replaces line [i]; when the last line gets text, an empty one is added under it. */
fun grow(lines: List<String>, i: Int, value: String): List<String> {
    val next = lines.toMutableList().also { it[i] = value }
    return if (next.last().isNotBlank() && next.size < 6) next + "" else next
}

@Composable private fun Field(label: Int, value: String, keyboard: KeyboardType = KeyboardType.Text, lines: Int = 1, onChange: (String) -> Unit) =
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(stringResource(label)) }, singleLine = lines == 1, minLines = lines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard))
