package com.example.safedistance

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat


class CheckDistance : Service() {
    private lateinit var vibrator: Vibrator
    private val handler = Handler(Looper.getMainLooper())
    private val interval: Long = 5000 // 5 seconds
    private val channelId = "VibrationServiceChannel"
    private lateinit var sharedPreferences: SharedPreferences
    private var isRunnable = false


    private var runnable: Runnable = object : Runnable {
        override fun run() {
            isRunnable = true
            vibrate(2500)
            showVibrationNotification()
            handler.postDelayed(this, interval)
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "START_MEASURE" -> startVibration()
                "STOP_MEASURE" -> stopVibration()
                Intent.ACTION_SCREEN_OFF -> stopVibration()
                Intent.ACTION_USER_PRESENT -> {
                    if (sharedPreferences.getBoolean("vibrate_on_unlock", true)) {
                        startVibration()
                    }
                }
            }
        }
    }

    private fun startVibration() {
        if (!isRunnable)
            handler.post(runnable)
    }

    private fun stopVibration() {
        handler.removeCallbacks(runnable)
        vibrator.cancel() // Cancel ongoing vibration
        isRunnable = false
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

    private fun showVibrationNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val vibrationNotification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Vibration Started")
            .setContentText("The device is vibrating.")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Use built-in icon
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), vibrationNotification)
    }


    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        super.onCreate()
        initVibrator()
        createNotificationChannel()
        val storageContext = createDeviceProtectedStorageContext()
        sharedPreferences = storageContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Vibration Service")
            .setContentText("Service is running")
            .setContentIntent(pendingIntent)
            .build()

        ServiceCompat.startForeground(
            this,
            1,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            } else {
                0
            }
        )
        handler.post(runnable) // Start the initial runnable

        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        })
        startVibration()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val action = it.getStringExtra("ACTION")
            when (action) {
                "START_MEASURE" -> startVibration()
                "STOP_MEASURE" -> stopVibration()
                else -> Log.d("CheckDistance", "Unknown action")
            }
        }
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
}
