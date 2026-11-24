package com.zirakusa.telefono.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.telefono.R
import com.zirakusa.telefono.data.SimChoice
import com.zirakusa.telefono.ui.AppActions
import com.zirakusa.telefono.ui.Page
import com.zirakusa.telefono.ui.SystemPage
import com.zirakusa.telefono.ui.TelefonoState
import com.zirakusa.telefono.ui.theme.TelefonoColors

/**
 * «Características del operador»: «Realizar llamadas con» (Preguntar, SIM 1 or SIM 2: the «SIM por defecto»), each SIM
 * with its name (Claro EC, Movistar…) and slot, and Android's SIM settings.
 */
@Composable fun CarrierScreen(state: TelefonoState, actions: AppActions) = SubPage(stringResource(R.string.carrier_title), { state.page = Page.SETTINGS }) {
    Group {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.make_calls_with), fontSize = 18.sp)
            Text(stringResource(if (state.sims.size == 1) R.string.one_sim_hint else R.string.make_calls_hint), color = TelefonoColors.Muted, fontSize = 14.sp)
            Segmented(listOf(stringResource(R.string.ask), stringResource(R.string.sim_n, 1), stringResource(R.string.sim_n, 2)), state.defaultSim.ordinal,
                Modifier.fillMaxWidth()) { state.changeSim(SimChoice.entries[it]) }
        }
    }
    Caption(stringResource(R.string.sim_cards))
    Group {
        if (state.sims.isEmpty()) Text(stringResource(R.string.no_sims), Modifier.padding(20.dp), color = TelefonoColors.Muted)
        val chosen = state.chosenSim
        state.sims.forEachIndexed { i, sim ->
            if (i > 0) Divider()
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                SimBadge(sim.slot)
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(sim.title.ifBlank { stringResource(R.string.sim_n, sim.slot) }, fontSize = 18.sp)
                    val carrier = sim.carrier.takeIf { it.isNotBlank() && it != sim.title }
                    Text(stringResource(R.string.sim_slot, sim.slot) + (carrier?.let { " · $it" } ?: ""), color = TelefonoColors.Muted, fontSize = 14.sp)
                }
                if (chosen == sim && state.sims.size >= 2) Text(stringResource(R.string.sim_default_mark), color = TelefonoColors.AccentText, fontSize = 14.sp)
            }
        }
    }
    Group { Item(stringResource(R.string.sys_sim), stringResource(R.string.sys_sim_hint)) { actions.system(SystemPage.SIM_NETWORK) } }
}
