package com.zirakusa.telefono

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The app's launcher icons: the handset in five styles, «3D» by default (since 0.5.0). Each is an `activity-alias` in the
 * manifest; exactly one is enabled. Alias names never change (Android keeps the enabled state by name), so the old colour
 * aliases now carry the new pictures.
 */
enum class AppIcon(val alias: String, val label: Int, val preview: Int, val background: Long) {
    CLAY("Home", R.string.icon_clay, R.mipmap.ic_launcher_clay_foreground, 0xFF13EA0C),
    PIXEL("HomeClaro", R.string.icon_pixel, R.mipmap.ic_launcher_pixel_foreground, 0xFF04E509),
    OIL("HomeDorado", R.string.icon_oil, R.mipmap.ic_launcher_oil_foreground, 0xFF1DC001),
    GLASS("HomeAzul", R.string.icon_glass, R.mipmap.ic_launcher_glass_foreground, 0xFF17D80F),
    CLASSIC("HomeVerde", R.string.icon_classic, R.mipmap.ic_launcher_classic_foreground, 0xFF2AEB1E);

    private fun component(context: Context) = ComponentName(context.packageName, "com.zirakusa.telefono.$alias")
    fun isEnabled(context: Context): Boolean = when (context.packageManager.getComponentEnabledSetting(component(context))) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> this == CLAY           // as the manifest says
        else -> false
    }

    companion object {
        var current by mutableStateOf(CLAY); private set

        /** Reads the enabled one; if none is (an odd update), «3D» comes back so the app never loses its icon. */
        fun load(context: Context) { val on = entries.firstOrNull { it.isEnabled(context) }; if (on == null) apply(context, CLAY) else current = on }

        /** Enables [icon] first and then disables the others, without closing the app. Launchers refresh on their own. */
        fun apply(context: Context, icon: AppIcon) {
            val pm = context.packageManager
            pm.setComponentEnabledSetting(icon.component(context), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            entries.filter { it != icon }.forEach {
                pm.setComponentEnabledSetting(it.component(context), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            }
            current = icon
        }
    }
}

/** «Icono de la app»: the five icons; a tap changes the home-screen icon. */
@Composable fun IconCard(card: Color, text: Color, muted: Color, accent: Color) {
    val context = LocalContext.current
    IconPicker(R.string.icon_title, AppIcon.entries.map { IconOption(it.label, it.preview, it.background) }, AppIcon.entries.indexOf(AppIcon.current),
        card, text, muted, accent) { AppIcon.apply(context, AppIcon.entries[it]) }
}

/** One choice in an [IconPicker]: its name, its picture and the colour behind it. */
class IconOption(val label: Int, val preview: Int, val background: Long)

/** A row of launcher icons to pick from (Teléfono's and Contactos' own); the [current] one is framed in [accent]. */
@Composable fun IconPicker(title: Int, options: List<IconOption>, current: Int, card: Color, text: Color, muted: Color, accent: Color, onPick: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(card).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(title), color = text, fontSize = 18.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            options.forEachIndexed { i, icon ->
                val on = i == current
                val name = stringResource(icon.label)
                Column(Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClickLabel = name) { onPick(i) }.padding(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Color(icon.background))
                        .border(if (on) 3.dp else 0.dp, if (on) accent else Color.Transparent, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Image(painterResource(icon.preview), null, Modifier.size(52.dp))
                    }
                    Text(name, color = if (on) accent else muted, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        }
        Text(stringResource(R.string.icon_hint), color = muted, fontSize = 13.sp)
    }
}
