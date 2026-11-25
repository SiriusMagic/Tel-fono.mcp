package com.zirakusa.contactos

import android.content.ComponentName
import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Contactos’ five launcher icons: one alias enabled at a time, and never none. */
@RunWith(RobolectricTestRunner::class)
class ContactsIconTest {
    private val context = RuntimeEnvironment.getApplication()
    private fun state(icon: ContactsIcon) = context.packageManager.getComponentEnabledSetting(ComponentName(context, "com.zirakusa.contactos.${icon.alias}"))

    @Test fun oneIconAtATime() {
        ContactsIcon.load(context); assertEquals(ContactsIcon.PIXEL, ContactsIcon.current)
        ContactsIcon.entries.forEach { icon ->
            ContactsIcon.apply(context, icon)
            assertEquals(icon, ContactsIcon.current)
            ContactsIcon.entries.forEach { other ->
                assertEquals(other.name, if (other == icon) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED, state(other))
            }
        }
        ContactsIcon.entries.forEach { context.packageManager.setComponentEnabledSetting(ComponentName(context, "com.zirakusa.contactos.${it.alias}"),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP) }
        ContactsIcon.load(context); assertEquals(ContactsIcon.PIXEL, ContactsIcon.current)      // never without an icon
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, state(ContactsIcon.PIXEL))
    }
}
