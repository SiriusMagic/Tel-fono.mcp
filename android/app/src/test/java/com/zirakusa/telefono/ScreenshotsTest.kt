package com.zirakusa.telefono

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.zirakusa.telefono.data.Call
import com.zirakusa.telefono.data.AnnounceMode
import com.zirakusa.telefono.data.CallType
import com.zirakusa.telefono.data.Speed
import com.zirakusa.telefono.data.Unknown
import com.zirakusa.telefono.data.MapValues
import com.zirakusa.telefono.data.MemoryBook
import com.zirakusa.telefono.data.SimChoice
import com.zirakusa.telefono.ui.AppActions
import com.zirakusa.telefono.ui.Page
import com.zirakusa.telefono.ui.Role
import com.zirakusa.telefono.ui.SystemPage
import com.zirakusa.telefono.ui.Tab
import com.zirakusa.telefono.ui.TelefonoApp
import com.zirakusa.telefono.ui.TelefonoState
import com.zirakusa.telefono.ui.theme.TelefonoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.ZoneOffset

/** Recientes (SIM, times, ⓘ), Teclado with two SIMs and WhatsApp, a number's page, «¿Con qué SIM?», Favoritos, Ajustes, permission card. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "es-w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotsTest {
    @get:Rule val compose = createComposeRule()
    private fun shot(name: String) { compose.waitForIdle(); onView(isRoot()).captureRoboImage("build/screenshots/telefono-$name.png") }

    /** Older calls with Ana, so her page has «No conectado», durations and «Ver más». */
    private val older = listOf(
        Call(6, "+593991234567", "Ana Pérez", CallType.OUTGOING, Book.at(18, 16, 8), 0, "2", "Móvil"),
        Call(7, "0991234567", "Ana Pérez", CallType.OUTGOING, Book.at(17, 11, 49), 98, "8959301", "Móvil"),
        Call(8, "0991234567", "Ana Pérez", CallType.INCOMING, Book.at(16, 12, 30), 93, "2", "Móvil"),
    )

    @Test fun allScreens() {
        val book = MemoryBook(Book.calls + older, Book.people, Book.sims)
        val state = TelefonoState(MapValues(), book, ZoneOffset.UTC, now = { Book.at(21, 9, 5) + 30_000 }) { LocalDate.of(2026, 11, 21) }
        state.load(); state.whatsapp = true
        val called = mutableListOf<Pair<String, Int?>>(); val chats = mutableListOf<String>(); var contacts = 0
        compose.setContent {
            TelefonoTheme(state.themeMode, state.accent) {
                TelefonoApp(state, AppActions(call = { n, sim -> called += n to sim?.slot }, whatsapp = { chats += it }, contacts = { contacts++ }))
            }
        }
        compose.onNodeWithText("Justo ahora").assertExists(); compose.onNodeWithText("Ayer").assertExists(); compose.onNodeWithText("08:00 a. m.").assertExists()
        compose.onNodeWithText("Ana Pérez (2)").performClick()
        compose.onNodeWithText("Mensaje").assertExists()
        shot("recents")
        compose.onNodeWithText("Llamadas perdidas").performClick(); compose.onNodeWithText("Ana Pérez (2)").assertExists()
        shot("recents-missed")
        compose.onNodeWithText("Todo").performClick()
        compose.onNodeWithContentDescription("Contactos").performClick(); assertEquals(1, contacts)
        compose.onNodeWithContentDescription("Teclado").performClick()
        compose.onNodeWithContentDescription("2").performClick(); compose.onNodeWithContentDescription("6").performClick(); compose.onNodeWithContentDescription("2").performClick()
        compose.onNodeWithText("Ana Pérez").assertExists()
        compose.onNodeWithText("Ana Pérez").performClick()
        shot("keypad")
        compose.onNodeWithContentDescription("Llamar con SIM 2").performClick(); assertEquals(listOf("0991234567" to 2), called)
        compose.onNodeWithContentDescription("WhatsApp").performClick(); assertEquals(listOf("593991234567"), chats)
        compose.onNodeWithContentDescription("Recientes").performClick()
        compose.onAllNodesWithContentDescription("Detalles")[0].performClick()
        compose.onNodeWithText("Elegir la SIM al llamar").assertExists()
        compose.onAllNodesWithText("Llamada saliente")[0].assertExists()
        shot("detail")
        compose.onNodeWithText("Ver más").performScrollTo().performClick(); compose.onNodeWithText("No conectado").performScrollTo().assertExists()
        compose.onNodeWithText("(1 m 38 s)").assertExists()
        shot("detail-more")
        compose.onAllNodesWithContentDescription("Llamar")[0].performScrollTo().performClick()
        compose.onNodeWithText("¿Con qué SIM llamas?").assertExists()
        shot("sim-dialog")
        compose.onNodeWithText("SIM 1 · Claro").performClick(); assertEquals("0991234567" to 1, called.last())
        state.changeSim(SimChoice.SIM2); compose.onNodeWithText("Llamar con SIM 2 (por defecto)").performScrollTo().assertExists()
        compose.onNodeWithContentDescription("Atrás").performClick()
        compose.onNodeWithContentDescription("Favoritos").performClick()
        compose.onNodeWithText("Pizzería Ñaño").assertExists()
        shot("favorites")
        state.page = Page.SETTINGS
        compose.onNodeWithText("Llamar directamente").assertExists()
        shot("settings")
        compose.onNodeWithText("Características del operador").performScrollTo().performClick()
        compose.onAllNodesWithText("SIM 1")[0].performClick(); assertEquals(SimChoice.SIM1, state.defaultSim)
        state.page = Page.SETTINGS
        book.allowed = false; state.load(); state.page = Page.MAIN; state.tab = Tab.RECENTS
        compose.onNodeWithText("Permitir").assertExists()
    }

    /** Ajustes › Llamadas: the filter (role, options, «Bloqueados»), the voice announcement, speed dial, the carrier page; a long press on 5. */
    @Test fun callingPages() {
        val calls = Book.calls + Call(9, "0980000000", "", CallType.BLOCKED, Book.at(21, 7, 30), label = "Ecuador")
        val book = MemoryBook(calls, Book.people, Book.sims.map { if (it.slot == 1) it.copy(name = "Claro EC") else it }, voicemailNumber = "*100")
        val state = TelefonoState(MapValues(), book, ZoneOffset.UTC, now = { Book.at(21, 9, 5) + 30_000 }) { LocalDate.of(2026, 11, 21) }
        state.load(); state.role = Role.OFF
        val called = mutableListOf<Pair<String, Int?>>(); var asked = 0; val opened = mutableListOf<SystemPage>()
        compose.setContent {
            TelefonoTheme(state.themeMode, state.accent) {
                TelefonoApp(state, AppActions(call = { n, sim -> called += n to sim?.slot }, askScreening = { asked++ }, system = { opened += it }))
            }
        }
        compose.onNodeWithText("Bloqueada").assertExists()                                    // a rejected call in Recientes
        compose.onNodeWithText("Ana Pérez (2)").performClick(); compose.onNodeWithText("Bloquear").performClick()
        assertTrue(state.isBlocked("0991234567")); compose.onNodeWithText("Desbloquear").assertExists()
        state.page = Page.SETTINGS
        compose.onNodeWithText("Filtro de llamadas no deseadas").assertExists(); compose.onNodeWithText("Desactivado").assertExists()
        compose.onNodeWithText("Grabación automática de llamadas").performScrollTo().assertExists()
        shot("settings-calls")
        compose.onNodeWithText("Configuración de llamadas del sistema").performScrollTo().performClick()
        compose.onNodeWithText("Sonidos y vibración").performScrollTo().performClick()
        compose.onNodeWithText("Configuración de SIM y red").performScrollTo().performClick()
        assertEquals(listOf(SystemPage.CALLS, SystemPage.SOUNDS, SystemPage.SIM_NETWORK), opened)
        compose.onNodeWithText("Anuncio de voz de llamada entrante").performScrollTo().performClick()
        compose.onNode(isDialog()).captureRoboImage("build/screenshots/telefono-announce.png")   // a dialog is its own window
        compose.onNodeWithText("Solo con auriculares").performClick(); assertEquals(AnnounceMode.HEADPHONES, state.announce)
        compose.onNodeWithText("Filtro de llamadas no deseadas").performScrollTo().performClick()
        compose.onNodeWithText("Usar Teléfono como filtro").performClick(); assertEquals(1, asked)
        state.role = Role.ON; compose.onNodeWithText("Teléfono es tu filtro de llamadas").assertExists()
        compose.onNodeWithText("Bloquear números ocultos y privados").performClick(); assertTrue(state.blockHidden)
        compose.onNodeWithText("Silenciar").performClick(); assertEquals(Unknown.SILENCE, state.unknown)
        shot("filter")
        compose.onNodeWithText("Bloqueados").performClick()
        compose.onNodeWithText("Ana Pérez").assertExists()
        compose.onNode(hasSetTextAction()).performTextInput("+593 98 000 0000"); compose.onNodeWithContentDescription("Bloquear").performClick()
        assertEquals(listOf("+593980000000", "0991234567"), state.blocked)
        shot("blocked")
        compose.onNodeWithContentDescription("Desbloquear Ana Pérez").performClick(); assertFalse(state.isBlocked("0991234567"))
        state.page = Page.SETTINGS
        compose.onNodeWithText("Marcación rápida").performScrollTo().performClick()
        compose.onNodeWithText("Buzón de voz").assertExists()
        compose.onAllNodesWithText("Sin asignar")[3].performClick()                              // key 5
        compose.onNodeWithText("Tecla 5").assertExists()
        compose.onNode(hasSetTextAction()).performTextInput("ana")
        shot("speed-pick")
        compose.onNodeWithText("Ana Pérez").performClick()
        assertEquals(listOf(Speed('5', "0991234567", "Ana Pérez")), state.speed)
        shot("speed")
        state.page = Page.SETTINGS
        compose.onNodeWithText("Características del operador").performScrollTo().performClick()
        compose.onNodeWithText("Claro EC").assertExists(); compose.onNodeWithText("Ranura 1 · Claro").assertExists(); compose.onNodeWithText("Ranura 2").assertExists()
        compose.onAllNodesWithText("SIM 2")[0].performClick(); assertEquals(SimChoice.SIM2, state.defaultSim)
        compose.onNodeWithText("Por defecto").assertExists()
        shot("carrier")
        state.page = Page.MAIN; state.tab = Tab.KEYPAD
        compose.onNodeWithContentDescription("5").performTouchInput { longClick() }
        assertEquals("0991234567" to 2, called.last())
        compose.onNodeWithContentDescription("8").performTouchInput { longClick() }
        assertEquals(Page.SPEED, state.page); assertEquals('8', state.assigning)
    }
}
