package com.zirakusa.telefono

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.zirakusa.telefono.data.Call
import com.zirakusa.telefono.data.CallType
import com.zirakusa.telefono.data.NoAccess
import com.zirakusa.telefono.data.Person
import com.zirakusa.telefono.data.PhoneBook
import com.zirakusa.telefono.data.Handle
import com.zirakusa.telefono.data.Sim
import com.zirakusa.telefono.data.Sims
import com.zirakusa.telefono.data.SubInfo

/** The phone's call log, contacts and SIMs, read through Android's providers and TelecomManager. Reads only. */
class AndroidBook(context: Context) : PhoneBook {
    private val app = context.applicationContext
    private val cr = app.contentResolver
    /** The phone account of each SIM, by id, for «Llamar con SIM n» (filled by [sims]). */
    val accounts = HashMap<String, PhoneAccountHandle>()

    override fun calls(limit: Int): List<Call> {
        val cols = arrayOf(CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION,
            CallLog.Calls.PHONE_ACCOUNT_ID, CallLog.Calls.CACHED_NUMBER_TYPE, CallLog.Calls.CACHED_NUMBER_LABEL, CallLog.Calls.GEOCODED_LOCATION)
        val cursor = try { cr.query(CallLog.Calls.CONTENT_URI, cols, null, null, CallLog.Calls.DATE + " DESC") } catch (_: SecurityException) { throw NoAccess() }
        val out = ArrayList<Call>()
        cursor?.use { c ->
            while (c.moveToNext() && out.size < limit) {
                val name = c.getString(2).orEmpty()
                // «Móvil» (the contact's kind of number) for a saved number; the place («Ecuador») for the rest.
                val label = if (name.isNotBlank() && !c.isNull(7)) ContactsContract.CommonDataKinds.Phone.getTypeLabel(app.resources, c.getInt(7), c.getString(8)).toString()
                    else c.getString(9).orEmpty()
                out += Call(c.getLong(0), c.getString(1).orEmpty(), name, type(c.getInt(3)), c.getLong(4), c.getLong(5), c.getString(6).orEmpty(), label)
            }
        }
        return out
    }

    override fun people(): List<Person> = runCatching {
        val cols = arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.STARRED)
        val by = LinkedHashMap<Long, Person>()
        cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, cols, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0); val n = c.getString(2).orEmpty()
                val p = by[id] ?: Person(id, c.getString(1).orEmpty(), emptyList(), c.getInt(3) == 1)
                by[id] = if (n.isBlank() || n in p.numbers) p else p.copy(numbers = p.numbers + n)
            }
        }
        by.values.toList()
    }.getOrDefault(emptyList())

    private fun type(t: Int) = when (t) {
        CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING; CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING; CallLog.Calls.MISSED_TYPE -> CallType.MISSED
        CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED; CallLog.Calls.BLOCKED_TYPE -> CallType.BLOCKED; CallLog.Calls.VOICEMAIL_TYPE -> CallType.VOICEMAIL
        else -> CallType.INCOMING
    }

    /** The SIMs that can call (needs «Teléfono» permission: READ_PHONE_STATE); empty without it. */
    override fun sims(): List<Sim> {
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) return emptyList()
        return try {
            val telecom = app.getSystemService(TelecomManager::class.java) ?: return emptyList()
            val tm = app.getSystemService(TelephonyManager::class.java)
            val handles = telecom.callCapablePhoneAccounts
            val subs = app.getSystemService(SubscriptionManager::class.java)?.activeSubscriptionInfoList.orEmpty()
                .map { SubInfo(it.simSlotIndex, it.subscriptionId, runCatching { it.iccId.orEmpty() }.getOrDefault(""), it.carrierName?.toString().orEmpty()) }
            accounts.clear(); handles.forEach { accounts[it.id] = it }
            Sims.match(handles.map { h -> Handle(h.id, if (Build.VERSION.SDK_INT >= 30 && tm != null) runCatching { tm.getSubscriptionId(h) }.getOrNull()?.takeIf { it >= 0 } else null) }, subs)
        } catch (_: SecurityException) { emptyList() }
    }
}
