package com.zirakusa.telefono

import android.content.Context
import android.provider.CallLog
import android.provider.ContactsContract
import com.zirakusa.telefono.data.Call
import com.zirakusa.telefono.data.CallType
import com.zirakusa.telefono.data.NoAccess
import com.zirakusa.telefono.data.Person
import com.zirakusa.telefono.data.PhoneBook

/** The phone's call log and contacts, read through Android's providers (always with `?` arguments). Reads only. */
class AndroidBook(context: Context) : PhoneBook {
    private val cr = context.applicationContext.contentResolver

    override fun calls(limit: Int): List<Call> {
        val cols = arrayOf(CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION)
        val cursor = try { cr.query(CallLog.Calls.CONTENT_URI, cols, null, null, CallLog.Calls.DATE + " DESC") } catch (_: SecurityException) { throw NoAccess() }
        val out = ArrayList<Call>()
        cursor?.use { c ->
            while (c.moveToNext() && out.size < limit) {
                out += Call(c.getLong(0), c.getString(1).orEmpty(), c.getString(2).orEmpty(), type(c.getInt(3)), c.getLong(4), c.getLong(5))
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
}
