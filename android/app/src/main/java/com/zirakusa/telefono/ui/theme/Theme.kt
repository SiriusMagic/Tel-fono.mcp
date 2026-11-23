package com.zirakusa.telefono.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** Ajustes › Temas: follow the phone, always light, dark (navy) or pure black (for OLED screens). */
enum class ThemeMode { SYSTEM, LIGHT, DARK, BLACK }

/** Ajustes › Color de acento. [ink] is the text and icons drawn on the accent; [deep] a darker shade (light screens, glows). */
enum class Accent(val color: Color, val ink: Color, val deep: Color) {
    ROJO(Color(0xFFEB4D3D), Color(0xFFFFFFFF), Color(0xFFC0392B)),
    NARANJA(Color(0xFFFF9A1F), Color(0xFF1A0E00), Color(0xFFC46A00)),
    LIMA(Color(0xFFC6F135), Color(0xFF10180A), Color(0xFF5FA82E)),
    AMARILLO(Color(0xFFF5C518), Color(0xFF111111), Color(0xFFB98A0E)),
    ROSA(Color(0xFFF06292), Color(0xFF111111), Color(0xFFB23A66)),
    AZUL(Color(0xFF4FC3F7), Color(0xFF0A1420), Color(0xFF2479B8)),
    VERDE(Color(0xFF4CE08A), Color(0xFF07160D), Color(0xFF239457)),
}

/** The app's colours for one mode. [sheet] is the background of pages that slide over the tabs; [lines] the faint ticks. */
data class Palette(val background: Color, val card: Color, val chip: Color, val sheet: Color, val lines: Color,
    val text: Color, val muted: Color, val light: Boolean)

private val Navy = Palette(Color(0xFF0D1828), Color(0xFF16243A), Color(0xFF223450), Color(0xFF122036), Color(0x669FB8D8),
    Color(0xFFEFF3F8), Color(0xFF8C9AB0), light = false)
private val Black = Palette(Color(0xFF000000), Color(0xFF1C1C1E), Color(0xFF2C2C2E), Color(0xFF242426), Color(0x66FFFFFF),
    Color(0xFFF2F2F2), Color(0xFF8E8E92), light = false)
private val Light = Palette(Color(0xFFF2F2F5), Color(0xFFFFFFFF), Color(0xFFE3E3E8), Color(0xFFF7F7FA), Color(0x662A2A33),
    Color(0xFF121A26), Color(0xFF66748A), light = true)

/**
 * The global theme. Every screen reads its colours here; they follow Ajustes › Temas and Color de acento (state, so a
 * change repaints at once). Default: pure black with verde, easy on the eyes at night.
 */
object TelefonoColors {
    var palette by mutableStateOf(Black); private set
    var chosen: com.zirakusa.telefono.ui.theme.Accent by mutableStateOf(com.zirakusa.telefono.ui.theme.Accent.VERDE); private set

    val Background get() = palette.background
    val Card get() = palette.card
    val Chip get() = palette.chip
    val Sheet get() = palette.sheet
    val Lines get() = palette.lines
    val Text get() = palette.text
    val Muted get() = palette.muted
    val Accent: Color get() = chosen.color
    val OnAccent: Color get() = chosen.ink
    val Deep: Color get() = chosen.deep
    /** Accent for text and thin lines: the darker shade on light screens, where the bright one is hard to read. */
    val AccentText: Color get() = if (palette.light) chosen.deep else chosen.color
    val Danger = Color(0xFFFF6B6B)
    val isLight get() = palette.light

    /** Changes only what differs, so painting with the same theme never asks for another frame. */
    fun use(mode: ThemeMode, systemDark: Boolean, accent: com.zirakusa.telefono.ui.theme.Accent) {
        val next = when (mode) {
            ThemeMode.SYSTEM -> if (systemDark) Black else Light
            ThemeMode.LIGHT -> Light
            ThemeMode.DARK -> Navy
            ThemeMode.BLACK -> Black
        }
        if (palette != next) palette = next
        if (chosen != accent) chosen = accent
    }
}

@Composable fun TelefonoTheme(mode: ThemeMode = ThemeMode.BLACK, accent: Accent = Accent.VERDE, content: @Composable () -> Unit) {
    TelefonoColors.use(mode, isSystemInDarkTheme(), accent)
    val c = TelefonoColors
    val scheme = if (!c.isLight) darkColorScheme(primary = c.Accent, onPrimary = c.OnAccent, background = c.Background, surface = c.Card,
        surfaceContainer = c.Card, surfaceContainerHigh = c.Card, onBackground = c.Text, onSurface = c.Text, onSurfaceVariant = c.Muted, error = c.Danger)
    else lightColorScheme(primary = c.Accent, onPrimary = c.OnAccent, background = c.Background, surface = c.Card,
        surfaceContainer = c.Card, surfaceContainerHigh = c.Card, onBackground = c.Text, onSurface = c.Text, onSurfaceVariant = c.Muted, error = c.Danger)
    MaterialTheme(scheme) { CompositionLocalProvider(LocalContentColor provides c.Text, content = content) }
}
