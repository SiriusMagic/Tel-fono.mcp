package com.zirakusa.telefono

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.zirakusa.telefono.data.Dial
import com.zirakusa.telefono.data.Numbers

/**
 * When the AI asks for a call, the phone shows a notification: «Claude quiere que llames a Ana Pérez». Tapping it only
 * opens the number in the dialer; the user still presses «Llamar». Nothing is ever dialled by the AI.
 */
object Requests {
    private const val CHANNEL = "requests"

    /** Shows the request; false when notifications are off for Teléfono. */
    fun post(context: Context, who: String, number: String, name: String): Boolean {
        val app = context.applicationContext
        if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        if (!NotificationManagerCompat.from(app).areNotificationsEnabled()) return false
        app.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(CHANNEL, app.getString(R.string.requests_channel), NotificationManager.IMPORTANCE_HIGH))
        val dial = Intent(Intent.ACTION_DIAL, Dial.telUri(number).toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val tap = PendingIntent.getActivity(app, number.hashCode(), dial, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val shown = name.ifBlank { Numbers.pretty(number) }
        val n = NotificationCompat.Builder(app, CHANNEL).setSmallIcon(R.drawable.ic_stat_call)
            .setContentTitle(app.getString(R.string.request_title, who, shown))
            .setContentText(app.getString(R.string.request_text)).setAutoCancel(true).setContentIntent(tap)
            .setCategory(NotificationCompat.CATEGORY_REMINDER).build()
        return runCatching { NotificationManagerCompat.from(app).notify(number.hashCode(), n); true }.getOrDefault(false)
    }
}
