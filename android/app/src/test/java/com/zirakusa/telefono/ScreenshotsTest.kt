package com.zirakusa.telefono

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.zirakusa.telefono.data.MapValues
import com.zirakusa.telefono.data.MemoryBook
import com.zirakusa.telefono.ui.Page
import com.zirakusa.telefono.ui.TelefonoApp
import com.zirakusa.telefono.ui.TelefonoState
import com.zirakusa.telefono.ui.theme.TelefonoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.ZoneOffset

/** Recientes (with a line open), Teclado with T9 suggestions, Favoritos, Ajustes and the permission card. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "es-w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotsTest {
    @get:Rule val compose = createComposeRule()
    private fun shot(name: String) { compose.waitForIdle(); onView(isRoot()).captureRoboImage("build/screenshots/telefono-$name.png") }

    @Test fun allScreens() {
        val book = MemoryBook(Book.calls, Book.people)
        val state = TelefonoState(MapValues(), book, ZoneOffset.UTC) { LocalDate.of(2026, 11, 21) }
        state.load()
        val called = mutableListOf<String>()
        compose.setContent { TelefonoTheme(state.themeMode, state.accent) { TelefonoApp(state, com.zirakusa.telefono.ui.AppActions(call = { called += it })) } }
        compose.onNodeWithText("Ana Pérez (2)").performClick()
        compose.onNodeWithText("Mensaje").assertExists()
        shot("recents")
        compose.onNodeWithContentDescription("Teclado").performClick()
        compose.onNodeWithContentDescription("2").performClick(); compose.onNodeWithContentDescription("6").performClick(); compose.onNodeWithContentDescription("2").performClick()
        compose.onNodeWithText("Ana Pérez").assertExists()
        shot("keypad")
        compose.onNodeWithText("Ana Pérez").performClick()
        compose.onNodeWithContentDescription("Llamar").performClick(); assertEquals(listOf("0991234567"), called)
        compose.onNodeWithContentDescription("Favoritos").performClick()
        compose.onNodeWithText("Pizzería Ñaño").assertExists()
        shot("favorites")
        state.page = Page.SETTINGS
        compose.onNodeWithText("Llamar directamente").assertExists()
        shot("settings")
        book.allowed = false; state.load(); state.page = Page.MAIN; state.tab = com.zirakusa.telefono.ui.Tab.RECENTS
        compose.onNodeWithText("Permitir").assertExists()
    }
}
