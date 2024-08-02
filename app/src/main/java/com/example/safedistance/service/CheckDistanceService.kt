package com.example.safedistance.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class CheckDistanceService : Service() {

    private var focalLength: Float = 0f
    private var sensorX: Float = 0f
    private var sensorY: Float = 0f


    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val action = it.getStringExtra("ACTION")
            when (action) {
                "START_MEASURE" -> startMeasure()
                "STOP_MEASURE" -> stopMeasure()
                else -> Log.d("CheckDistance", "Unknown action")
            }

            focalLength = it.getFloatExtra("focalLength", focalLength)
            sensorX = it.getFloatExtra("sensorX", sensorX)
            sensorY = it.getFloatExtra("sensorY", sensorY)
            Log.d("CheckDistance", "Received focalLength: $focalLength, sensorX: $sensorX, sensorY: $sensorY")
        }
        return START_STICKY
    }


    private fun startMeasure() {
        TODO("Not yet implemented")
    }

    private fun stopMeasure() {
        TODO("Not yet implemented")
    }
}