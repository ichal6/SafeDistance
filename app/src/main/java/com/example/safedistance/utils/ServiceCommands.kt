package com.example.safedistance.utils

import android.content.ContextWrapper
import android.content.Intent

class ServiceCommands(
    private val context: ContextWrapper
) {
    fun sendServiceCommand(action: String, serviceClass: Class<*>) {
        val intent = Intent(context, serviceClass).apply {
            putExtra("ACTION", action)
        }
        context.startService(intent)
    }
}