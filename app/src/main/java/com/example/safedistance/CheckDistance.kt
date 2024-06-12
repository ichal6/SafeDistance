package com.example.safedistance

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.core.app.NotificationCompat

class CheckDistance : Service() {
    private lateinit var vibrator: Vibrator
    private val handler = Handler(Looper.getMainLooper())
    private val interval: Long = 5000 // 5 seconds
    private val channelId = "VibrationServiceChannel"

    private var runnable: Runnable = object : Runnable {
        override fun run() {
            vibrator.vibrate(2500) // Vibrate for 500 millisecondsinterval
            handler.postDelayed(this, interval)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                "Vibration Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }


    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        super.onCreate()
        initVibrator()
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Vibration Service")
            .setContentText("Service is running")
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
        handler.post(runnable) // Start the initial runnable


    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Toast.makeText(this, "Info from service", Toast.LENGTH_SHORT).show()
        this.vibrate(2500)

        return START_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable) // Remove the runnable when service is destroyed
    }

    private fun initVibrator() {
        this.vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun vibrate(ms: Long) {
        val vibrationEffect1: VibrationEffect
        // requires system version Oreo (API 26)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrationEffect1 =
                VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)

            // it is safe to cancel other vibrations currently taking place
            vibrator.cancel()
            vibrator.vibrate(vibrationEffect1)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(ms)
        }
    }

    private fun showToast(appContext: Context) {
        Toast.makeText(appContext, "This is a periodic toast", Toast.LENGTH_SHORT).show()
    }

}
