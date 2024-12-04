package com.example.safedistance.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.safedistance.MainActivity
import java.util.UUID

class NotificationHelper private constructor(
    private val context: Context,
    private val channelName: String,
    private val channelDescription: String,
    private val isSilent: Boolean
) {
    private val channelID = UUID.randomUUID().toString()
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    companion object Factory {
        fun create(context: Context, channelName: String, channelDescription: String) =
            NotificationHelper(
                context,
                channelName,
                channelDescription,
                isSilent = false)

        fun createSilent(context: Context, channelName: String, channelDescription: String) =
            NotificationHelper(
                context,
                channelName,
                channelDescription,
                isSilent = true)
    }

    private fun createNotificationChannel() {
        val importance = if (isSilent) NotificationManager.IMPORTANCE_LOW else NotificationManager.IMPORTANCE_DEFAULT

        val channel = NotificationChannel(
            channelID,
            channelName,
            importance
        ).apply {
            description = channelDescription
            if (isSilent) {
                setSound(null, null)
                enableVibration(false)
            }
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun getNotificationBuilder(title: String, body: String, intent: PendingIntent?) =
        NotificationCompat.Builder(context.applicationContext, channelID)
            .setWhen(System.currentTimeMillis())
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Use built-in icon
            .setContentIntent(intent)
            .setSilent(isSilent)

    fun notify(id: Int, notification: Notification) {
        notificationManager.notify(id, notification)
    }

    fun createNotification(title: String, body: String, context: Context): Notification {
        val mainActivityIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, mainActivityIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val notificationBuilder =
            getNotificationBuilder(title, body, pendingIntent)

        return notificationBuilder.build()
    }
}
