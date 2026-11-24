package com.zirakusa.contactos

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.zirakusa.contactos.data.Account
import com.zirakusa.contactos.data.Contact
import com.zirakusa.contactos.data.MapValues
import com.zirakusa.contactos.data.MemoryStore
import com.zirakusa.contactos.data.Phone
import com.zirakusa.contactos.data.Raw
import com.zirakusa.contactos.ui.ContactosApp
import com.zirakusa.contactos.ui.ContactosState
import com.zirakusa.contactos.ui.Page
import com.zirakusa.contactos.ui.theme.ContactosTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** Every screen, painted: the list, a search, a contact, its card, the editor, «Elige qué editar», «Guardar en», Ajustes, duplicates and the permission card. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "es-w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotsTest {
    @get:Rule val compose = createComposeRule()
    private fun shot(name: String) { compose.waitForIdle(); onView(isRoot()).captureRoboImage("build/screenshots/contactos-$name.png") }

    @Test fun allScreens() {
        val google = Account("com.google", "ana@gmail.com"); val sim1 = Account("SIM Account", "SIM1")
        val rosa = Contact(10, "Rosa Mora", listOf(Phone("0981112222")), raws = listOf(Raw(100, google), Raw(101, sim1)))
        val beto = Contact(11, "Beto Ruiz", listOf(Phone("0997776666")), raws = listOf(Raw(110, sim1)))
        val store = MemoryStore(People.all + rosa + beto, listOf(Account.PHONE, sim1, google), me = Contact(99, "Adonis S.", listOf(Phone("0990000000"))))
        val state = ContactosState(MapValues(), store) { LocalDate.of(2026, 11, 21) }
        state.load()
        compose.setContent { ContactosTheme(state.themeMode, state.accent) { ContactosApp(state) } }
        compose.onNodeWithText("Cumpleaños próximos").assertExists(); compose.onNodeWithText("Mi perfil").assertExists()
        compose.onNodeWithText("8 contactos").assertExists(); compose.onNodeWithText("Combinar").assertExists()
        shot("list")
        compose.onNodeWithText("R").performClick()                                   // the letter bar jumps to R (Rosa, with her SIM mark)
        compose.onNodeWithText("Rosa Mora").assertExists()
        shot("letters")
        state.query = "099"
        compose.onNodeWithText("099 123 4567").assertExists()
        shot("search")
        state.query = ""
        compose.onNodeWithText("Anita P.").performClick()
        state.open(store.list().first { it.id == 1L })
        compose.onNodeWithText("Zirakusa").assertExists(); compose.onNodeWithText("Cumpleaños · cumple 35").assertExists()
        shot("detail")
        compose.onNodeWithText("Guardado en").assertExists()
        state.card(People.ana)
        compose.onNodeWithText("Escanea el código QR para añadir el contacto").assertExists()
        shot("card")
        state.open(store.list().first { it.id == 1L })
        compose.onNodeWithText("Editar").performClick()
        compose.onNodeWithText("Guardar").assertExists()
        shot("edit")
        state.open(store.list().first { it.id == 10L }); state.edit(state.shown)
        compose.onNodeWithText("SIM 1 · 098 111 2222").assertExists()
        shot("pick")
        state.page = Page.LIST; state.edit()
        compose.onNodeWithText("Guardar en").assertExists()
        shot("new")
        state.page = Page.SETTINGS
        compose.onNodeWithText("Almacenamiento predeterminado").assertExists()
        shot("settings")
        state.page = Page.SETTINGS
        compose.onNodeWithText("Contactos repetidos").performClick()
        compose.onNodeWithText("Anita P.").assertExists()
        shot("duplicates")
        store.allowed = false; state.load(); state.page = Page.LIST
        compose.onNodeWithText("Permitir").assertExists()
        shot("permission")
    }
}
