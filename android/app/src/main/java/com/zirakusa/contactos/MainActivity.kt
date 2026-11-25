package com.zirakusa.contactos

import com.zirakusa.telefono.R

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.BlockedNumberContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.SideEffect
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import com.zirakusa.contactos.data.Contact
import com.zirakusa.contactos.data.SharedValues
import com.zirakusa.contactos.data.Transfer
import com.zirakusa.contactos.data.VCard
import com.zirakusa.contactos.mcp.ContactTools
import com.zirakusa.contactos.ui.AppActions
import com.zirakusa.contactos.ui.ContactosApp
import com.zirakusa.contactos.ui.ContactosState
import com.zirakusa.contactos.ui.Page
import com.zirakusa.contactos.ui.screens.labelName
import com.zirakusa.contactos.ui.theme.ContactosColors
import com.zirakusa.contactos.ui.theme.ContactosTheme
import java.io.File
import kotlin.concurrent.thread

/** Creates the state, asks for the contacts, and does what the screens ask: dial, write, email, map, share, save the card, ringtone, block, .vcf and copy. */
class MainActivity : ComponentActivity() {
    private lateinit var state: ContactosState
    private lateinit var store: AndroidStore
    private val allow = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { state.allowedNow(); ContactsWidget.refresh(this) }
    private var ringFor: Contact? = null
    private val pickRing = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val c = ringFor ?: return@registerForActivityResult
        if (r.resultCode == RESULT_OK) {
            val uri = r.data?.let { IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, android.net.Uri::class.java) }
            state.setRingtone(c, uri?.takeIf { it != android.provider.Settings.System.DEFAULT_RINGTONE_URI }?.toString())
        }
    }
    private val importer = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(::importFrom) }
    private val exporter = registerForActivityResult(ActivityResultContracts.CreateDocument("text/x-vcard")) { uri -> uri?.let(::exportTo) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContactsIcon.load(this)
        enableEdgeToEdge()
        store = AndroidStore(this)
        state = ContactosState(SharedValues(this), store)
        ContactTools.onChanged = { state.load() }
        // The tools are served by Teléfono's Modo MCP; loading it here keeps it running when only Contactos was opened.
        com.zirakusa.telefono.mcp.McpMode.load(this)
        val actions = AppActions(allow = ::ask, call = { open(Intent(Intent.ACTION_DIAL, "tel:${android.net.Uri.encode(it)}".toUri())) },
            sms = { open(Intent(Intent.ACTION_SENDTO, "smsto:${android.net.Uri.encode(it)}".toUri())) },
            email = { open(Intent(Intent.ACTION_SENDTO, "mailto:".toUri()).putExtra(Intent.EXTRA_EMAIL, arrayOf(it))) },
            map = { open(Intent(Intent.ACTION_VIEW, "geo:0,0?q=${android.net.Uri.encode(it)}".toUri())) },
            share = ::share, shareImage = ::shareImage, shareText = { c -> open(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, VCard.plain(c)), getString(R.string.c_share))) }, saveImage = ::saveImage, ringtone = ::pickRingtone, block = ::block,
            blockedNumbers = ::blockedList, editProfile = { c -> open(Intent(Intent.ACTION_EDIT, ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, c.id))) },
            importCards = { launch { importer.launch(arrayOf("text/x-vcard", "text/vcard", "text/directory", "application/octet-stream")) } },
            exportCards = { launch { exporter.launch("contactos.vcf") } }, toast = ::toast,
            copy = { copy("Contacto", it, R.string.c_copied) }, addWidget = { if (!ContactsWidget.pin(this)) Toast.makeText(this, R.string.c_widget_cannot, Toast.LENGTH_LONG).show() })
        state.load()
        // Reading isn't enough to save: Teléfono asks only to read, so «modify contacts» is asked here (Android grants it
        // without asking again when reading is already allowed).
        if (!state.allowed || !granted(Manifest.permission.WRITE_CONTACTS)) ask()
        handle(intent)
        setContent {
            ContactosTheme(state.themeMode, state.accent) {
                val light = ContactosColors.isLight
                SideEffect { WindowCompat.getInsetsController(window, window.decorView).run { isAppearanceLightStatusBars = light; isAppearanceLightNavigationBars = light } }
                BackHandler(state.page != Page.LIST || state.query.isNotEmpty()) {
                    when (state.page) {
                        Page.LIST -> state.query = ""; Page.EDIT -> state.cancelEdit()
                        Page.DUPLICATES -> state.page = state.dupesBack; Page.PICK, Page.CARD -> state.page = Page.DETAIL
                        else -> state.page = Page.LIST
                    }
                }
                ContactosApp(state, actions)
            }
        }
    }

    override fun onResume() { super.onResume(); if (state.page != Page.EDIT) state.load() }
    override fun onPause() { super.onPause(); ContactsWidget.refresh(this) }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); handle(intent) }

    private fun granted(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    /**
     * What opened Contactos: «Añadir a contactos» (from Teléfono, the widget or another app) fills «Nuevo contacto» with
     * the number and name it brings; Teléfono's «Editar» opens that contact's editor; the widget opens a contact. Nothing
     * is saved until the user taps «Guardar».
     */
    private fun handle(intent: Intent?) {
        intent ?: return
        when {
            intent.action == ContactsContract.Intents.Insert.ACTION ->
                state.newContact(intent.getStringExtra(ContactsContract.Intents.Insert.PHONE), intent.getStringExtra(ContactsContract.Intents.Insert.NAME))
            intent.getLongExtra(EXTRA_EDIT, 0L) != 0L -> state.items.firstOrNull { it.id == intent.getLongExtra(EXTRA_EDIT, 0L) }?.let(state::edit)
            intent.getLongExtra(EXTRA_OPEN, 0L) != 0L -> state.items.firstOrNull { it.id == intent.getLongExtra(EXTRA_OPEN, 0L) }?.let(state::open)
            else -> return
        }
        // Handled once: turning the screen or coming back doesn't open the editor again.
        setIntent(Intent(this, MainActivity::class.java).setAction(Intent.ACTION_MAIN))
    }

    companion object {
        /** A contact id: Teléfono's «Editar contacto» opens its editor here. */
        const val EXTRA_EDIT = "com.zirakusa.contactos.EDIT"
        /** A contact id: the widget opens its page here. */
        const val EXTRA_OPEN = "com.zirakusa.contactos.OPEN"
    }

    private fun ask() = allow.launch(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS))

    private fun open(intent: Intent) = try { startActivity(intent) } catch (_: ActivityNotFoundException) { Toast.makeText(this, R.string.c_no_app, Toast.LENGTH_SHORT).show() }

    /** The contact as a .vcf card, for any app the user picks. */
    private fun share(c: Contact) {
        val file = File(File(cacheDir, "share").apply { mkdirs() }, "contacto.vcf").apply { writeText(VCard.build(c)) }
        val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
        val send = Intent(Intent.ACTION_SEND).setType("text/x-vcard").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(c.shownName, uri)
        open(Intent.createChooser(send, getString(R.string.c_share)))
    }

    private fun toast(message: Int) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    private fun launch(go: () -> Unit) = try { go() } catch (_: ActivityNotFoundException) { toast(R.string.c_no_app) }

    private fun cardPng(c: Contact) = CardImage.png(c, state.name(c), c.phones.firstOrNull()?.let { getString(labelName(it.label)) }, getString(R.string.c_card_scan))

    /** «Compartir» › «Imagen»: the card as a PNG from the cache, for the app the user picks. */
    private fun shareImage(c: Contact) {
        val file = File(File(cacheDir, "share").apply { mkdirs() }, "contacto.png").apply { writeBytes(cardPng(c)) }
        val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
        val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(c.shownName, uri)
        open(Intent.createChooser(send, getString(R.string.c_share)))
    }

    /** «Guardar»: the card's image in Pictures/Contactos (the gallery), without any storage permission. */
    private fun saveImage(c: Contact) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return toast(R.string.c_image_failed)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Contacto ${c.shownName.filter { it.isLetterOrDigit() || it == ' ' }.take(40)}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Contactos")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val ok = runCatching {
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("no uri")
            contentResolver.openOutputStream(uri)?.use { it.write(cardPng(c)) } ?: error("no stream")
            contentResolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        }.isSuccess
        toast(if (ok) R.string.c_image_saved else R.string.c_image_failed)
    }

    private fun pickRingtone(c: Contact) {
        ringFor = c
        val pick = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true).putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.c_ringtone))
            .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, c.ringtone?.toUri() ?: android.provider.Settings.System.DEFAULT_RINGTONE_URI)
        launch { pickRing.launch(pick) }
    }

    /**
     * «Bloquear contacto»: Android lets only the default phone or SMS app write the blocked list. If Contactos can't, the
     * phone's own list opens with the number copied.
     */
    private fun block(c: Contact) {
        val done = try {
            BlockedNumberContract.canCurrentUserBlockNumbers(this) && c.phones.all { p ->
                contentResolver.insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                    ContentValues().apply { put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, p.number) }) != null
            }
        } catch (_: SecurityException) { false } catch (_: UnsupportedOperationException) { false } catch (_: IllegalArgumentException) { false }
        if (done) return toast(R.string.c_blocked_done)
        getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Contacto", c.phones.first().number))
        Toast.makeText(this, R.string.c_blocked_system, Toast.LENGTH_LONG).show()
        blockedList()
    }

    private fun blockedList() { getSystemService(TelecomManager::class.java)?.createManageBlockedNumbersIntent()?.let(::open) ?: toast(R.string.c_no_app) }

    /** «Importar»: every card of the .vcf, into «Almacenamiento predeterminado», skipping people already there. */
    private fun importFrom(uri: android.net.Uri) = thread {
        val result = runCatching {
            val text = contentResolver.openInputStream(uri)?.use { it.readBytes().let { b -> String(b, 0, minOf(b.size, 8_000_000), Charsets.UTF_8) } } ?: error("empty")
            Transfer.import(store, text, state.storage)
        }.getOrNull()
        runOnUiThread {
            state.load()
            if (result == null) toast(R.string.c_transfer_failed)
            else Toast.makeText(this, getString(R.string.c_imported, result.added, result.skipped), Toast.LENGTH_LONG).show()
        }
    }

    /** «Exportar»: everyone the list shows, in one .vcf where the user chose. */
    private fun exportTo(uri: android.net.Uri) {
        val text = Transfer.export(state.visible)
        val ok = runCatching { contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) } ?: error("no stream") }.isSuccess
        toast(if (ok) R.string.c_exported else R.string.c_transfer_failed)
    }

    private fun copy(label: String, text: String, message: Int) {
        getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
