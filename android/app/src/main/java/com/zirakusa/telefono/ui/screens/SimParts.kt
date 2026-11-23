package com.zirakusa.telefono.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.zirakusa.telefono.R
import com.zirakusa.telefono.data.Sim
import com.zirakusa.telefono.ui.AppActions
import com.zirakusa.telefono.ui.TelefonoState
import com.zirakusa.telefono.ui.theme.TelefonoColors

/** The little SIM card with its slot («2»), next to a call. */
@Composable fun SimBadge(slot: Int, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.sim_n, slot)
    Box(modifier.size(width = 13.dp, height = 16.dp).border(1.dp, TelefonoColors.Muted, RoundedCornerShape(topStart = 2.dp, topEnd = 5.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
        .clearAndSetSemantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Text("$slot", color = TelefonoColors.Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** «SIM 1 · Claro». */
@Composable fun simName(sim: Sim) = stringResource(R.string.sim_n, sim.slot) + if (sim.carrier.isNotBlank()) " · ${sim.carrier}" else ""

/** «¿Con qué SIM llamas?»: shows while [TelefonoState.asking] holds a number (two SIMs and no default). */
@Composable fun SimDialog(state: TelefonoState, actions: AppActions) {
    val number = state.asking ?: return
    Dialog({ state.asking = null }) {
        Column(Modifier.clip(RoundedCornerShape(28.dp)).background(TelefonoColors.Card).padding(vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.pick_sim), Modifier.padding(horizontal = 24.dp, vertical = 8.dp), fontSize = 20.sp)
            state.sims.forEach { sim ->
                Row(Modifier.fillMaxWidth().clickable { state.asking = null; actions.call(number, sim) }.padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Call, null, tint = TelefonoColors.AccentText)
                    Text(simName(sim), Modifier.padding(start = 16.dp), fontSize = 17.sp)
                }
            }
        }
    }
}

/** The green call button of the keypad: one, or split in «1 | 2» when the phone has two SIMs. */
@Composable fun CallPill(state: TelefonoState, actions: AppActions, number: String) {
    val enabled = number.any { it.isDigit() }
    if (state.sims.size < 2) return Box(Modifier.size(72.dp).clip(CircleShape).background(TelefonoColors.Accent)
        .clickable(enabled = enabled, onClickLabel = stringResource(R.string.call)) { actions.call(number, state.chosenSim) }, contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.Call, stringResource(R.string.call), Modifier.size(32.dp), tint = TelefonoColors.OnAccent)
    }
    Row(Modifier.height(64.dp).clip(CircleShape).background(TelefonoColors.Accent)) {
        state.sims.take(2).forEachIndexed { i, sim ->
            if (i > 0) Box(Modifier.width(1.dp).height(64.dp).background(TelefonoColors.OnAccent.copy(alpha = 0.25f)))
            val label = stringResource(R.string.call_with, sim.slot)
            Row(Modifier.width(76.dp).height(64.dp).clickable(enabled = enabled, onClickLabel = label) { actions.call(number, sim) }
                .clearAndSetSemantics { contentDescription = label }, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Call, null, Modifier.size(28.dp), tint = TelefonoColors.OnAccent)
                Text("${sim.slot}", Modifier.padding(start = 2.dp, bottom = 14.dp), color = TelefonoColors.OnAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
