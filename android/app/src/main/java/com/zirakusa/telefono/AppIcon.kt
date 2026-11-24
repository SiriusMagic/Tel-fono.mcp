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
 * The app's launcher icons: its own drawing in five colour styles. Each is an `activity-alias` in the manifest; exactly
 * one is enabled. Alias names never change (Android keeps the enabled state by name).
 */
enum class AppIcon(val alias: String, val label: Int, val preview: Int, val background: Long) {
    ORIGINAL("Home", R.string.icon_original, R.drawable.ic_launcher_foreground, 0),
    LIGHT("HomeClaro", R.string.icon_light, R.drawable.ic_launcher_foreground_light, 0xFFF2F2F2),
    GOLD("HomeDorado", R.string.icon_gold, R.drawable.ic_launcher_foreground_gold, 0xFFF5C518),
    BLUE("HomeAzul", R.string.icon_blue, R.drawable.ic_launcher_foreground_blue, 0xFF1565C0),
    GREEN("HomeVerde", R.string.icon_green, R.drawable.ic_launcher_foreground_green, 0xFF1B5E20);

    private fun component(context: Context) = ComponentName(context.packageName, "com.zirakusa.telefono.$alias")
    fun isEnabled(context: Context): Boolean = when (context.packageManager.getComponentEnabledSetting(component(context))) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> this == ORIGINAL           // as the manifest says
        else -> false
    }

    companion object {
        var current by mutableStateOf(ORIGINAL); private set

        /** Reads the enabled one; if none is (an odd update), the original comes back so the app never loses its icon. */
        fun load(context: Context) { val on = entries.firstOrNull { it.isEnabled(context) }; if (on == null) apply(context, ORIGINAL) else current = on }

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
@Composable fun IconCard(card: Color, text: Color, muted: Color, accent: Color, original: Color = Color(0xFF1A1A1A)) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(card).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.icon_title), color = text, fontSize = 18.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AppIcon.entries.forEach { icon ->
                val on = icon == AppIcon.current
                val name = stringResource(icon.label)
                Column(Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClickLabel = name) { AppIcon.apply(context, icon) }.padding(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(if (icon.background == 0L) original else Color(icon.background))
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
