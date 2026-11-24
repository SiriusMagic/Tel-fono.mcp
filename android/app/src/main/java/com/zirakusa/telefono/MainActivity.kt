package com.zirakusa.telefono

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ContentUris
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.SideEffect
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import com.zirakusa.telefono.data.Dial
import com.zirakusa.telefono.data.SharedValues
import com.zirakusa.telefono.data.Sim
import com.zirakusa.telefono.mcp.McpMode
import com.zirakusa.telefono.ui.AppActions
import com.zirakusa.telefono.ui.Page
import com.zirakusa.telefono.ui.Tab
import com.zirakusa.telefono.ui.TelefonoApp
import com.zirakusa.telefono.ui.TelefonoState
import com.zirakusa.telefono.ui.theme.TelefonoColors
import com.zirakusa.telefono.ui.theme.TelefonoTheme

/** Creates the state, asks for permissions, opens numbers other apps send (tel:), calls, and plays the keypad tones. */
class MainActivity : ComponentActivity() {
    private lateinit var state: TelefonoState
    private lateinit var book: AndroidBook
    private var tones: ToneGenerator? = null
    private val allow = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { reload() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        book = AndroidBook(this)
        state = TelefonoState(SharedValues(this), book)
        McpMode.load(this)
        tones = runCatching { ToneGenerator(AudioManager.STREAM_DTMF, 60) }.getOrNull()
        val actions = AppActions(allow = ::ask, call = ::call, sms = { open(Intent(Intent.ACTION_SENDTO, "smsto:${android.net.Uri.encode(it)}".toUri())) },
            addContact = { open(Intent(ContactsContract.Intents.Insert.ACTION).setType(ContactsContract.RawContacts.CONTENT_TYPE).putExtra(ContactsContract.Intents.Insert.PHONE, it)) },
            copy = { copy("Teléfono", it) }, tone = ::tone, copyLink = { copy("Teléfono MCP", it) }, goToHub = ::goToHub,
            whatsapp = ::whatsapp, contacts = ::contacts,
            editContact = { open(Intent(Intent.ACTION_EDIT, ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, it))) })
        reload()
        if (!state.allowed) ask()
        dialed(intent)
        setContent {
            TelefonoTheme(state.themeMode, state.accent) {
                val light = TelefonoColors.isLight
                SideEffect { WindowCompat.getInsetsController(window, window.decorView).run { isAppearanceLightStatusBars = light; isAppearanceLightNavigationBars = light } }
                BackHandler(state.page != Page.MAIN || state.tab != Tab.RECENTS) {
                    when (state.page) {
                        Page.MCP -> state.page = Page.SETTINGS; Page.SETTINGS -> state.page = Page.MAIN; Page.DETAIL -> state.closeDetail()
                        Page.MAIN -> state.tab = Tab.RECENTS
                    }
                }
                TelefonoApp(state, actions)
            }
        }
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); dialed(intent) }
    override fun onResume() { super.onResume(); reload() }

    private fun reload() { state.whatsapp = whatsappPackage() != null; state.load() }
    override fun onDestroy() { tones?.release(); super.onDestroy() }

    /** A tel: link from another app opens the keypad with the number (never calls by itself). */
    private fun dialed(intent: Intent?) {
        val n = intent?.data?.takeIf { it.scheme == "tel" }?.schemeSpecificPart ?: return
        state.typed = Dial.clean(n); state.tab = Tab.KEYPAD; state.page = Page.MAIN
    }

    private fun granted(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED
    private fun ask() = allow.launch(buildList {
        add(Manifest.permission.READ_CALL_LOG); add(Manifest.permission.READ_CONTACTS); add(Manifest.permission.CALL_PHONE)
        add(Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray())

    /**
     * Calls at once when allowed (Ajustes › Llamar directamente), with the SIM the user chose; otherwise opens the number
     * in the phone's dialer. Only ever after the user's tap.
     */
    private fun call(number: String, sim: Sim?) {
        if (number.none { it.isDigit() }) return
        val direct = state.direct && granted(Manifest.permission.CALL_PHONE)
        val intent = Intent(if (direct) Intent.ACTION_CALL else Intent.ACTION_DIAL, Dial.telUri(number).toUri())
        val account = sim?.let { book.accounts[it.id] }
        if (direct && account != null) intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, account)
        open(intent)
    }

    /** WhatsApp (or WhatsApp Business) if installed; Teléfono itself never goes online for this. */
    private fun whatsappPackage() = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { packageManager.getLaunchIntentForPackage(it) != null }

    /** Opens WhatsApp's chat with these digits (wa.me link, handed to the WhatsApp app). */
    private fun whatsapp(digits: String) {
        val pkg = whatsappPackage() ?: return
        open(Intent(Intent.ACTION_VIEW, "https://wa.me/$digits".toUri()).setPackage(pkg))
    }

    /** «Contactos»: inside Teléfono now (its own screen and launcher icon), opened at once. */
    private fun contacts() = open(Intent(this, com.zirakusa.contactos.MainActivity::class.java))

    private fun tone(k: Char) {
        if (!state.tones) return
        val t = when (k) { in '0'..'9' -> ToneGenerator.TONE_DTMF_0 + (k - '0'); '*' -> ToneGenerator.TONE_DTMF_S; '#' -> ToneGenerator.TONE_DTMF_P; else -> return }
        tones?.startTone(t, 120)
    }

    private fun open(intent: Intent) = try { startActivity(intent) } catch (_: ActivityNotFoundException) { Toast.makeText(this, R.string.no_app, Toast.LENGTH_SHORT).show() }
        catch (_: SecurityException) { Toast.makeText(this, R.string.no_app, Toast.LENGTH_SHORT).show() }

    private fun copy(label: String, text: String) {
        getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
    }

    /** «Ir al Hub»: copies the hub's link (in case it must be pasted) and opens the Gateway Hub app. */
    private fun goToHub() {
        copy("Teléfono › Gateway Hub", McpMode.hubLink())
        val hub = packageManager.getLaunchIntentForPackage("com.zirakusa.gatewayhub")
        if (hub != null) startActivity(hub) else Toast.makeText(this, R.string.mcp_paste_in_hub, Toast.LENGTH_LONG).show()
    }
}
