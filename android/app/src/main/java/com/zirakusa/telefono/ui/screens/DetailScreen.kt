package com.zirakusa.telefono.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.telefono.R
import com.zirakusa.telefono.data.Dial
import com.zirakusa.telefono.data.Filter
import com.zirakusa.telefono.data.Numbers
import com.zirakusa.telefono.ui.AppActions
import com.zirakusa.telefono.ui.TelefonoState
import com.zirakusa.telefono.ui.dial
import com.zirakusa.telefono.ui.theme.TelefonoColors

/**
 * The page of a number (ⓘ in «Recientes»): the big letter, the name, «Llamar con SIM n (por defecto)», its numbers with
 * Llamar · Mensaje · WhatsApp, «Bloquear este número», and every call with it. Editing or saving the contact goes to Contactos (or the phone's).
 */
@Composable fun DetailScreen(state: TelefonoState, actions: AppActions) {
    val number = state.detail ?: return
    val person = state.detailPerson
    val history = state.history
    val name = person?.name ?: history.firstOrNull { it.name.isNotBlank() }?.name.orEmpty()
    val title = name.ifBlank { Numbers.pretty(number).ifBlank { stringResource(R.string.private_number) } }
    Column(Modifier.fillMaxSize().background(TelefonoColors.Background).systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), size = 52.dp, onClick = state::closeDetail)
            Spacer(Modifier.weight(1f))
            if (person != null) RoundButton(Icons.Outlined.Edit, stringResource(R.string.edit_contact), size = 52.dp) { actions.editContact(person.id) }
            else if (number.isNotBlank()) RoundButton(Icons.Outlined.PersonAdd, stringResource(R.string.new_contact), size = 52.dp) { actions.addContact(number) }
        }
        Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp).size(112.dp).clip(CircleShape).background(TelefonoColors.Accent),
            contentAlignment = Alignment.Center) {
            Text(Dial.initial(name), color = TelefonoColors.OnAccent, fontSize = 48.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(title, Modifier.fillMaxWidth().padding(top = 16.dp), fontSize = 30.sp, textAlign = TextAlign.Center, maxLines = 2)
        if (number.isNotBlank() && state.sims.size >= 2) SimLine(state, actions, number)
        Spacer(Modifier.size(20.dp))
        if (number.isNotBlank()) {
            val numbers = person?.numbers?.takeIf { it.isNotEmpty() } ?: listOf(number)
            numbers.forEach { n -> NumberRow(state, actions, n, Dial.history(state.calls, n).firstOrNull { it.label.isNotBlank() }?.label.orEmpty()) }
            Divider()
            val wa = Numbers.wa(number)
            if (state.whatsapp && wa != null) Item(stringResource(R.string.whatsapp), null, value = Numbers.pretty("+$wa")) { actions.whatsapp(wa) }
            if (Filter.canBlock(number)) {
                if (state.isBlocked(number)) Item(stringResource(R.string.unblock_number), null) { state.unblock(number) }
                else Item(stringResource(R.string.block_number), null) { state.block(number) }
            }
        }
        HistoryList(state, history)
        Spacer(Modifier.size(24.dp))
    }
}

/** «Llamar con SIM 2 (por defecto) ›», or «Elegir SIM al llamar ›» when there is no default. */
@Composable private fun SimLine(state: TelefonoState, actions: AppActions, number: String) {
    val sim = state.chosenSim
    Row(Modifier.fillMaxWidth().padding(top = 6.dp).clip(CircleShape).clickable { state.dial(number, actions) }.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        if (sim != null) SimBadge(sim.slot, Modifier.padding(end = 6.dp))
        Text(if (sim != null) stringResource(R.string.call_with_default, sim.slot) else stringResource(R.string.call_ask_sim), color = TelefonoColors.Muted, fontSize = 15.sp)
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(18.dp), tint = TelefonoColors.Muted)
    }
}
