package com.zirakusa.contactos

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.view.View
import android.widget.RemoteViews
import com.zirakusa.contactos.data.Contact
import com.zirakusa.contactos.data.NoAccess
import com.zirakusa.contactos.data.Settings
import com.zirakusa.contactos.data.SharedValues
import com.zirakusa.contactos.data.WidgetPeople
import com.zirakusa.telefono.R

/**
 * The «Contactos» home-screen widget, one for Teléfono and Contactos (they are one app): the title opens Contactos, the
 * green button Teléfono, + a new contact, and up to four favourites. A favourite opens Teléfono with the number ready
 * (one more tap calls); nothing calls by itself. Redrawn when contacts change in the app, through MCP, and every 30 min.
 */
object ContactsWidget {
    /** Redraws every «Contactos» widget on the home screen. */
    fun refresh(context: Context) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val ids = runCatching { manager.getAppWidgetIds(ComponentName(context, ContactsWidgetProvider::class.java)) }.getOrNull() ?: return
        if (ids.isNotEmpty()) views(context).let { v -> ids.forEach { manager.updateAppWidget(it, v) } }
    }

    /** Puts the widget on the home screen (the launcher asks the user). False when the launcher cannot. */
    fun pin(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context) ?: return false
        return manager.isRequestPinAppWidgetSupported && manager.requestPinAppWidget(ComponentName(context, ContactsWidgetProvider::class.java), null, null)
    }

    /** The widget with the phone's favourites (null when Android doesn't let Contactos read them). */
    fun views(context: Context): RemoteViews {
        val app = context.applicationContext
        val people = try {
            WidgetPeople.pick(AndroidStore(app).list(), Settings(SharedValues(app)).order)
        } catch (_: NoAccess) { null } catch (_: RuntimeException) { emptyList() }
        return views(app, people)
    }

    /** The widget showing [people] (null: «Abre Contactos para permitir el acceso»). */
    fun views(context: Context, people: List<Contact>?): RemoteViews = RemoteViews(context.packageName, R.layout.c_widget_contacts).apply {
        val app = context.applicationContext
        setImageViewResource(R.id.c_widget_icon, runCatching { ContactsIcon.enabled(app).launcher }.getOrDefault(R.mipmap.c_ic_launcher))
        setOnClickPendingIntent(R.id.c_widget_title, contacts(app, 1, Intent()))
        setOnClickPendingIntent(R.id.c_widget_phone, PendingIntent.getActivity(app, 2,
            Intent(app, com.zirakusa.telefono.MainActivity::class.java).setAction(Intent.ACTION_MAIN)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), FLAGS))
        setOnClickPendingIntent(R.id.c_widget_add, contacts(app, 3, Intent(ContactsContract.Intents.Insert.ACTION)))
        removeAllViews(R.id.c_widget_people)
        people.orEmpty().forEach { c ->
            addView(R.id.c_widget_people, RemoteViews(app.packageName, R.layout.c_widget_person).apply {
                setTextViewText(R.id.c_widget_initials, c.initials)
                setTextViewText(R.id.c_widget_name, WidgetPeople.short(c))
                val number = WidgetPeople.number(c)
                setOnClickPendingIntent(R.id.c_widget_person, if (number != null) PendingIntent.getActivity(app, 1000 + (c.id % 100_000).toInt(),
                    Intent(app, com.zirakusa.telefono.MainActivity::class.java).setAction(Intent.ACTION_DIAL).setData(Uri.fromParts("tel", number, null))
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), FLAGS)
                    else contacts(app, 200_000 + (c.id % 100_000).toInt(), Intent().putExtra(MainActivity.EXTRA_OPEN, c.id)))
            })
        }
        val empty = when { people == null -> R.string.c_widget_no_access; people.isEmpty() -> R.string.c_widget_empty; else -> 0 }
        setViewVisibility(R.id.c_widget_people, if (empty == 0) View.VISIBLE else View.GONE)
        setViewVisibility(R.id.c_widget_empty, if (empty == 0) View.GONE else View.VISIBLE)
        if (empty != 0) { setTextViewText(R.id.c_widget_empty, app.getString(empty)); setOnClickPendingIntent(R.id.c_widget_empty, contacts(app, 4, Intent())) }
    }

    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    private fun contacts(context: Context, code: Int, intent: Intent): PendingIntent = PendingIntent.getActivity(context, code,
        intent.setClass(context, MainActivity::class.java).apply { if (action == null) action = Intent.ACTION_MAIN }
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), FLAGS)
}

class ContactsWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = ContactsWidget.views(context)
        ids.forEach { manager.updateAppWidget(it, views) }
    }
}
