package com.example.safedistance.service

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
import com.example.safedistance.utils.Constants
import com.example.safedistance.utils.NotificationHelper
import com.example.safedistance.utils.ServiceCommands

class VibratorService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val interval: Long = 5000 // 5 seconds
    private lateinit var vibrator: Vibrator
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var notificationHelper: NotificationHelper
    private var serviceCommands: ServiceCommands = ServiceCommands(this)
    private var isRunnable = false
    internal var isScreenOn = true
        private set

    private var runnable: Runnable = object : Runnable {
        override fun run() {
            isRunnable = true
            vibrate(2500)
            showVibrationNotification("Too close", "You are too close to device!")
            handler.postDelayed(this, interval)
        }
    }

    internal val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    stopVibration()
                    serviceCommands.sendServiceCommand(
                        Constants.ACTION_STOP_CAMERA.name, CheckDistanceService::class.java
                    )
                }
                Intent.ACTION_USER_PRESENT -> {
                    isScreenOn = true
                    if (sharedPreferences.getBoolean("vibrate_on_unlock", true)) {
                        startVibration()
                        serviceCommands.sendServiceCommand(
                            Constants.ACTION_START_CAMERA.name, CheckDistanceService::class.java
                        )
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
        initForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val action = it.getStringExtra(Constants.ACTION.name)
            when(action) {
                Constants.ACTION_START_VIBRATION.name -> {
                    if (isScreenOn) {
                        startVibration()
                    } else {
                        stopVibration()
                    }
                }
                Constants.ACTION_STOP_VIBRATION.name -> stopVibration()
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
        this.notificationHelper = NotificationHelper.create(
            this,
            "Vibration Notification",
            "Channel use to display notification for Vibration"
        )
    }

    private fun initForegroundService() {
        ServiceCompat.startForeground(
            this,
            1,
            notificationHelper.createNotification("Vibration Service", "Service is running", this),
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

    private fun showVibrationNotification(title :String, body: String) {
        val notificationBuilder = notificationHelper.createNotification(title, body, this)
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
        val vibrationEffect = VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.cancel()
        vibrator.vibrate(vibrationEffect)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable) // Remove the runnable when service is destroyed
        unregisterReceiver(screenReceiver)
    }
}
