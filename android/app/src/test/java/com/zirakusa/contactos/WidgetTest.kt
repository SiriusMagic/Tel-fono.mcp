package com.zirakusa.contactos

import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.zirakusa.contactos.data.WidgetPeople
import com.zirakusa.telefono.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The «Contactos» widget: favourites with initials, the empty and no-access texts, and a harmless refresh. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "es-w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetTest {
    private val context = RuntimeEnvironment.getApplication()

    private fun draw(people: List<com.zirakusa.contactos.data.Contact>?, name: String): View {
        val host = FrameLayout(context)
        val view = ContactsWidget.views(context, people).apply(context, host)
        host.addView(view, FrameLayout.LayoutParams(context.resources.displayMetrics.widthPixels - 40, (180 * context.resources.displayMetrics.density).toInt()))
        host.measure(View.MeasureSpec.makeMeasureSpec(context.resources.displayMetrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec((180 * context.resources.displayMetrics.density).toInt(), View.MeasureSpec.EXACTLY))
        host.layout(0, 0, host.measuredWidth, host.measuredHeight)
        val bitmap = android.graphics.Bitmap.createBitmap(host.measuredWidth, host.measuredHeight, android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(0xFF6B8FB8.toInt())                 // a wallpaper-like colour behind the card
        host.draw(android.graphics.Canvas(bitmap))
        java.io.File("build/screenshots").mkdirs()
        java.io.FileOutputStream("build/screenshots/contactos-widget-$name.png").use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        return view
    }

    @Test fun favouritesEmptyAndNoAccess() {
        val people = WidgetPeople.pick(People.all.map { it.copy(starred = true) })
        val full = draw(people, "favorites")
        val row = full.findViewById<android.view.ViewGroup>(R.id.c_widget_people)
        assertEquals(4, row.childCount)
        assertEquals("ÁN", row.getChildAt(0).findViewById<TextView>(R.id.c_widget_initials).text.toString())
        assertEquals("Álvaro", row.getChildAt(0).findViewById<TextView>(R.id.c_widget_name).text.toString())
        assertEquals("Marca contactos con ★ para verlos aquí", draw(emptyList(), "empty").findViewById<TextView>(R.id.c_widget_empty).text.toString())
        assertEquals("Abre Contactos para permitir el acceso", draw(null, "no-access").findViewById<TextView>(R.id.c_widget_empty).text.toString())
        assertNotNull(ContactsWidget.views(context))
        ContactsWidget.refresh(context)
    }
}
