package com.example.safedistance.service

import android.app.Notification
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
import androidx.core.app.ServiceCompat
import com.example.safedistance.MainActivity
import com.example.safedistance.utils.Constants
import com.example.safedistance.utils.NotificationHelper

class VibratorService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val interval: Long = 5000 // 5 seconds
    private lateinit var vibrator: Vibrator
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var notificationHelper: NotificationHelper
    private var isRunnable = false

    private var runnable: Runnable = object : Runnable {
        override fun run() {
            isRunnable = true
            vibrate(2500)
            showVibrationNotification("Vibration Started", "The device is vibrating.")
            handler.postDelayed(this, interval)
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> stopVibration()
                Intent.ACTION_USER_PRESENT -> {
                    if (sharedPreferences.getBoolean("vibrate_on_unlock", true)) {
                        //startVibration()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        initVibrator()

        initActionsForScreenStatus()

        initNotificationHelper()
        showVibrationNotification("Vibration Service", "Service is running")

        initForegroundService()

        //runRunnable() // Start the initial runnable
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val action = it.getStringExtra("ACTION")
            when(action) {
                "START_MEASURE" -> startVibration()
                "STOP_MEASURE" -> stopVibration()
                Constants.ACTION_CLOSE_ALL_SERVICES.name -> stopService()
                else -> Log.d("VibrationService", "Unknown action")
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
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

    private fun initNotificationHelper() {
        this.notificationHelper = NotificationHelper(
            this,
            "Vibration Notification",
            "Channel use to display notification for Vibration"
        )
    }

    private fun initForegroundService() {
        ServiceCompat.startForeground(
            this,
            1,
            createNotification("Vibration Service", "Service is running"),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            } else {
                0
            }
        )
    }

    private fun initActionsForScreenStatus() {
        val storageContext = createDeviceProtectedStorageContext()
        sharedPreferences = storageContext.getSharedPreferences("settings", MODE_PRIVATE)

        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        })
    }

    private fun createNotification(title: String, body: String): Notification {
        val mainActivityIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, mainActivityIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return notificationHelper.getNotificationBuilder(title, body, pendingIntent).build()
    }

    private fun showVibrationNotification(title :String, body: String) {
        val notificationBuilder = createNotification(title, body)
        notificationHelper.notify(System.currentTimeMillis().toInt(), notificationBuilder)
    }

    private fun startVibration() {
        if (!isRunnable)
            runRunnable()
    }

    private fun runRunnable() {
        handler.post(runnable)
    }

    private fun stopVibration() {
        handler.removeCallbacks(runnable)
        vibrator.cancel() // Cancel ongoing vibration
        isRunnable = false
    }

    private fun stopService() {
        stopVibration()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable) // Remove the runnable when service is destroyed
        unregisterReceiver(screenReceiver)
    }
}
