package com.example.safedistance.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.UUID

class NotificationHelper(
    private val context: ContextWrapper,
    private val channelName: String,
    private val descriptionChannel: String
) {
    private val channelID = UUID.randomUUID().toString()
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelID,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = descriptionChannel
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun getNotificationBuilder(title: String, body: String, intent: PendingIntent) =
        NotificationCompat.Builder(context.applicationContext, channelID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Use built-in icon
            .setContentIntent(intent)

    fun notify(id: Int, notification: Notification) {
        notificationManager.notify(id, notification)
    }
}
