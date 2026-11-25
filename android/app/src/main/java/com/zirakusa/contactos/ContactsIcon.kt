package com.zirakusa.contactos

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.zirakusa.telefono.IconOption
import com.zirakusa.telefono.IconPicker
import com.zirakusa.telefono.R

/**
 * Contactos' own launcher icon (it lives inside Teléfono and opens straight on the list), in five styles, «Píxel» by
 * default. Each is an `activity-alias` of Contactos' activity; exactly one is enabled. Alias names never change.
 */
enum class ContactsIcon(val alias: String, val label: Int, val preview: Int, val background: Long, val launcher: Int) {
    PIXEL("Home", R.string.c_icon_pixel, R.mipmap.c_ic_launcher_pixel_foreground, 0xFF0042F7, R.mipmap.c_ic_launcher),
    CARTOON("HomeDibujo", R.string.c_icon_cartoon, R.mipmap.c_ic_launcher_cartoon_foreground, 0xFFF87302, R.mipmap.c_ic_launcher_cartoon),
    OIL("HomeOleo", R.string.c_icon_oil, R.mipmap.c_ic_launcher_oil_foreground, 0xFFBF2D0C, R.mipmap.c_ic_launcher_oil),
    CLAY("HomeRelieve", R.string.c_icon_clay, R.mipmap.c_ic_launcher_clay_foreground, 0xFF035ED8, R.mipmap.c_ic_launcher_clay),
    GLASS("HomeCristal", R.string.c_icon_glass, R.mipmap.c_ic_launcher_glass_foreground, 0xFF0183CB, R.mipmap.c_ic_launcher_glass);

    private fun component(context: Context) = ComponentName(context.packageName, "com.zirakusa.contactos.$alias")
    fun isEnabled(context: Context): Boolean = when (context.packageManager.getComponentEnabledSetting(component(context))) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> this == PIXEL           // as the manifest says
        else -> false
    }

    companion object {
        /** The enabled one, read from Android (for the widget, outside the app). */
        fun enabled(context: Context) = entries.firstOrNull { it.isEnabled(context) } ?: PIXEL
        var current by mutableStateOf(PIXEL); private set

        /** Reads the enabled one; if none is (an odd update), «Píxel» comes back so Contactos never loses its icon. */
        fun load(context: Context) { val on = entries.firstOrNull { it.isEnabled(context) }; if (on == null) apply(context, PIXEL) else current = on }

        /** Enables [icon] first and then disables the others, without closing the app. Launchers refresh on their own. */
        fun apply(context: Context, icon: ContactsIcon) {
            val pm = context.packageManager
            pm.setComponentEnabledSetting(icon.component(context), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            entries.filter { it != icon }.forEach {
                pm.setComponentEnabledSetting(it.component(context), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            }
            current = icon
            ContactsWidget.refresh(context)                                   // the widget wears the same icon
        }
    }
}

/** Ajustes › «Icono de Contactos»: a tap changes the Contactos icon on the home screen. */
@Composable fun ContactsIconCard(card: Color, text: Color, muted: Color, accent: Color) {
    val context = LocalContext.current
    IconPicker(R.string.c_icon_title, ContactsIcon.entries.map { IconOption(it.label, it.preview, it.background) }, ContactsIcon.entries.indexOf(ContactsIcon.current),
        card, text, muted, accent) { ContactsIcon.apply(context, ContactsIcon.entries[it]) }
}
