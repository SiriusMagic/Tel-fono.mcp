package com.zirakusa.contactos.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.telefono.R
import com.zirakusa.contactos.data.Contact
import com.zirakusa.contactos.data.Grid
import com.zirakusa.contactos.data.Phones
import com.zirakusa.contactos.data.Qr
import com.zirakusa.contactos.ui.AppActions
import com.zirakusa.contactos.ui.ContactosState
import com.zirakusa.contactos.ui.Page
import com.zirakusa.contactos.ui.theme.ContactosColors

/** «Compartir contacto»: a white card with the name, number and a QR code of the vCard; «Guardar» (image) and «Compartir». */
@Composable fun CardScreen(state: ContactosState, c: Contact, actions: AppActions) = Sheet(stringResource(R.string.c_card_title), onBack = { state.page = Page.DETAIL }) {
    val grid = remember(c) { Qr.card(c) }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color.White).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Avatar(c, 72.dp, badge = false)
            Text(state.name(c), color = Color(0xFF111111), fontSize = 24.sp, textAlign = TextAlign.Center)
            c.phones.firstOrNull()?.let { p ->
                Text(Phones.pretty(p.number), color = Color(0xFF111111), fontSize = 18.sp)
                Text(stringResource(labelName(p.label)), color = Color(0xFF6B6B70), fontSize = 14.sp)
            }
            val scan = stringResource(R.string.c_card_scan)
            if (grid != null) QrCode(grid, Modifier.padding(top = 10.dp).size(230.dp).semantics { contentDescription = scan })
            Text(if (grid != null) scan else stringResource(R.string.c_card_too_long), color = Color(0xFF6B6B70), fontSize = 14.sp, textAlign = TextAlign.Center)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            MainButton(stringResource(R.string.c_save), Modifier.weight(1f).padding(end = 6.dp), 1f) { actions.saveImage(c) }
            var menu by remember { mutableStateOf(false) }
            Box(Modifier.weight(1f).padding(start = 6.dp)) {
                MainButton(stringResource(R.string.c_share), fraction = 1f) { menu = true }
                DropdownMenu(menu, { menu = false }, containerColor = ContactosColors.Card) {
                    DropdownMenuItem({ Text(stringResource(R.string.c_share_vcf)) }, { menu = false; actions.share(c) })
                    DropdownMenuItem({ Text(stringResource(R.string.c_share_image)) }, { menu = false; actions.shareImage(c) })
                    DropdownMenuItem({ Text(stringResource(R.string.c_share_text)) }, { menu = false; actions.shareText(c) })
                }
            }
        }
    }
}

/** The code drawn module by module: black on the card's white. */
@Composable fun QrCode(grid: Grid, modifier: Modifier) = Canvas(modifier) {
    val cell = size.width / grid.size
    for (y in 0 until grid.size) for (x in 0 until grid.size) if (grid[x, y]) drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
}
