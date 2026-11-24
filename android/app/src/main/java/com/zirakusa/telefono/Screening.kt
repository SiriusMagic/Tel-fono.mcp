package com.zirakusa.telefono

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.TelecomManager
import androidx.annotation.RequiresApi
import com.zirakusa.telefono.data.Announce
import com.zirakusa.telefono.data.AnnounceWords
import com.zirakusa.telefono.data.Filter
import com.zirakusa.telefono.data.Numbers
import com.zirakusa.telefono.data.Settings
import com.zirakusa.telefono.data.SharedValues
import com.zirakusa.telefono.data.Verdict

/**
 * «Filtro de llamadas no deseadas» and «Anuncio de voz»: Android asks this service about each incoming call while
 * Teléfono holds the call screening role (Android 10+, the user grants it). It only answers allow / silence / reject
 * ([Filter.decide]); it never answers, hangs up or records a call. Rejected calls stay in the call log as «Bloqueada».
 */
class Screening : CallScreeningService() {
    override fun onScreenCall(details: Call.Details) {
        if (Build.VERSION.SDK_INT < 29) return respondToCall(details, CallResponse.Builder().build())
        screen(details)
    }

    @RequiresApi(29)
    private fun screen(details: Call.Details) {
        if (details.callDirection != Call.Details.DIRECTION_INCOMING) return respondToCall(details, CallResponse.Builder().build())
        val app = applicationContext
        Thread {
            val settings = Settings(SharedValues(app))
            val number = details.handle?.schemeSpecificPart.orEmpty()
            val hidden = details.handlePresentation != TelecomManager.PRESENTATION_ALLOWED
            val book = AndroidBook(app)
            val person = if (book.canReadContacts()) Numbers.personOf(book.people(), number) else null
            val inContacts = if (book.canReadContacts()) person != null else null
            val verdict = Filter.decide(number, hidden, inContacts, settings.filter)
            val response = CallResponse.Builder()
            when (verdict) {
                Verdict.REJECT -> response.setDisallowCall(true).setRejectCall(true).setSkipCallLog(false).setSkipNotification(true)
                Verdict.SILENCE -> response.setSilenceCall(true)
                Verdict.ALLOW -> Unit
            }
            respondToCall(details, response.build())
            if (verdict == Verdict.ALLOW && Announce.should(settings.announce, Announcer.headphones(app))) {
                val words = AnnounceWords(app.getString(R.string.announce_call_from), app.getString(R.string.announce_private), app.getString(R.string.announce_plus))
                Announcer.say(app, Announce.text(person?.name, number, hidden, words))
            }
        }.start()
    }
}

/** Says the announcement with Android's own voice (TextToSpeech, on the phone), at the ringtone's volume, then lets it go. */
object Announcer {
    private var tts: TextToSpeech? = null
    private val main = Handler(Looper.getMainLooper())

    /** Wired, USB or Bluetooth headphones are connected. */
    fun headphones(context: Context): Boolean {
        val types = buildSet {
            addAll(listOf(AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_USB_HEADSET))
            if (Build.VERSION.SDK_INT >= 31) add(AudioDeviceInfo.TYPE_BLE_HEADSET)
        }
        val audio = context.getSystemService(AudioManager::class.java) ?: return false
        return audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type in types }
    }

    fun say(context: Context, text: String) = main.post {
        tts?.shutdown()
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context.applicationContext) { status ->
            val t = engine ?: return@TextToSpeech
            if (status != TextToSpeech.SUCCESS) return@TextToSpeech done(t)
            // The ring stream: silent when the phone is on silent or vibrate, like the ringtone itself.
            t.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) { main.post { done(t) } }
                @Deprecated("Deprecated in Java") override fun onError(utteranceId: String?) { main.post { done(t) } }
            })
            if (t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "telefono-announce") != TextToSpeech.SUCCESS) done(t)
        }
        tts = engine
    }

    private fun done(t: TextToSpeech) { t.shutdown(); if (tts === t) tts = null }
}
