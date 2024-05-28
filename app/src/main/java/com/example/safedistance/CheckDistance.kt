package com.example.safedistance

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast

class CheckDistance : Service() {

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Toast.makeText(this, "Info from service", Toast.LENGTH_SHORT).show()

        return START_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()
    }
}
