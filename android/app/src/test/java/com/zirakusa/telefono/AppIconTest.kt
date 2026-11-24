package com.zirakusa.telefono

import android.content.ComponentName
import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** The five launcher icons: one alias enabled at a time, and never none. */
@RunWith(RobolectricTestRunner::class)
class AppIconTest {
    private val context = RuntimeEnvironment.getApplication()
    private fun state(icon: AppIcon) = context.packageManager.getComponentEnabledSetting(ComponentName(context, "com.zirakusa.telefono.${icon.alias}"))

    @Test fun oneIconAtATime() {
        AppIcon.load(context); assertEquals(AppIcon.ORIGINAL, AppIcon.current)
        AppIcon.entries.forEach { icon ->
            AppIcon.apply(context, icon)
            assertEquals(icon, AppIcon.current)
            AppIcon.entries.forEach { other ->
                assertEquals(other.name, if (other == icon) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED, state(other))
            }
        }
        AppIcon.entries.forEach { context.packageManager.setComponentEnabledSetting(ComponentName(context, "com.zirakusa.telefono.${it.alias}"),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP) }
        AppIcon.load(context); assertEquals(AppIcon.ORIGINAL, AppIcon.current)      // never without an icon
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, state(AppIcon.ORIGINAL))
    }
}
