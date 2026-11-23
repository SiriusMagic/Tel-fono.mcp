package com.zirakusa.telefono.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.zirakusa.telefono.R
import com.zirakusa.telefono.ui.theme.Accent
import com.zirakusa.telefono.ui.theme.TelefonoColors
import com.zirakusa.telefono.ui.theme.ThemeMode

/** One line of a card: title, optional subtitle (or value on the right) and an arrow when it opens something. */
@Composable fun Item(title: String, subtitle: String?, value: String? = null, expand: Boolean = false, onClick: (() -> Unit)?) =
    Row(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 18.sp)
            if (subtitle != null) Text(subtitle, color = TelefonoColors.Muted, fontSize = 14.sp)
        }
        if (value != null) Text(value, Modifier.padding(start = 8.dp), color = TelefonoColors.Muted, fontSize = 15.sp)
        if (onClick != null) Icon(if (expand) Icons.Outlined.UnfoldMore else Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = TelefonoColors.Muted)
    }

/** A line with a switch. */
@Composable fun SwitchItem(title: String, subtitle: String?, on: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) =
    Row(Modifier.fillMaxWidth().clickable(enabled) { onChange(!on) }.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 18.sp, color = if (enabled) TelefonoColors.Text else TelefonoColors.Muted)
            if (subtitle != null) Text(subtitle, color = TelefonoColors.Muted, fontSize = 14.sp)
        }
        Switch(on, onChange, enabled = enabled, colors = SwitchDefaults.colors(checkedTrackColor = TelefonoColors.Accent, checkedThumbColor = TelefonoColors.OnAccent))
    }

/** A line you pick: a round mark (one choice) or a square check (several). */
@Composable fun ChoiceRow(title: String, on: Boolean, square: Boolean = false, subtitle: String? = null, onClick: () -> Unit) =
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 18.sp)
            if (subtitle != null) Text(subtitle, color = TelefonoColors.Muted, fontSize = 14.sp)
        }
        val shape = if (square) RoundedCornerShape(6.dp) else CircleShape
        Box(Modifier.size(24.dp).clip(shape).background(if (on && square) TelefonoColors.Accent else Color.Transparent)
            .border(if (on && !square) 6.dp else 2.dp, if (on) TelefonoColors.Accent else TelefonoColors.Muted, shape), contentAlignment = Alignment.Center) {
            if (on && square) Icon(Icons.Filled.Check, null, Modifier.size(18.dp), tint = TelefonoColors.OnAccent)
        }
    }

/** A small title over a card («Teléfono», «Región»…). */
@Composable fun Caption(text: String) = Text(text, Modifier.padding(start = 20.dp, top = 8.dp, bottom = 2.dp), color = TelefonoColors.Muted, fontSize = 15.sp)

/** A dialog with one text field («Etiqueta», «Nombre del temporizador»). */
@Composable fun TextDialog(title: String, initial: String, max: Int, onCancel: () -> Unit, onDone: (String) -> Unit) = Dialog(onCancel) {
    var text by remember { mutableStateOf(initial) }
    Column(Modifier.clip(RoundedCornerShape(28.dp)).background(TelefonoColors.Card).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, Modifier.align(Alignment.CenterHorizontally), fontSize = 20.sp)
        OutlinedTextField(text, { text = it.take(max) }, Modifier.fillMaxWidth(), singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onCancel, Modifier.weight(1f)) { Text(stringResource(R.string.cancel), color = TelefonoColors.Text) }
            TextButton({ onDone(text) }, Modifier.weight(1f).clip(CircleShape).background(TelefonoColors.Accent)) { Text(stringResource(R.string.accept), color = TelefonoColors.OnAccent) }
        }
    }
}

/** «Temas»: Sistema, Claro, Oscuro or Negro, and the accent colour. */
@Composable fun ThemeCard(mode: ThemeMode, accent: Accent, onMode: (ThemeMode) -> Unit, onAccent: (Accent) -> Unit) = Group {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.themes), fontSize = 18.sp)
        Segmented(listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark, R.string.theme_black).map { stringResource(it) },
            mode.ordinal, Modifier.fillMaxWidth()) { onMode(ThemeMode.entries[it]) }
        Text(stringResource(R.string.accent), fontSize = 15.sp, color = TelefonoColors.Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Accent.entries.forEach { a ->
                val name = stringResource(accentNames.getValue(a))
                Box(Modifier.size(36.dp).clip(CircleShape).background(a.color)
                    .border(3.dp, if (a == accent) TelefonoColors.Text else Color.Transparent, CircleShape)
                    .clickable(onClickLabel = name) { onAccent(a) }.semantics { contentDescription = name },
                    contentAlignment = Alignment.Center) { if (a == accent) Icon(Icons.Filled.Check, null, Modifier.size(20.dp), tint = a.ink) }
            }
        }
    }
}

private val accentNames = mapOf(
    Accent.ROJO to R.string.accent_red, Accent.NARANJA to R.string.accent_orange, Accent.LIMA to R.string.accent_lime, Accent.AMARILLO to R.string.accent_yellow,
    Accent.ROSA to R.string.accent_pink, Accent.AZUL to R.string.accent_blue, Accent.VERDE to R.string.accent_green,
)
