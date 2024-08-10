package com.example.safedistance.utils

import android.content.ContextWrapper
import android.content.Intent

class ServiceCommands(
    private val context: ContextWrapper
) {
    fun sendServiceCommand(action: String, serviceClass: Class<*>) {
        val intent = Intent(context, serviceClass).apply {
            putExtra(Constants.ACTION.name, action)
        }
        context.startService(intent)
    }

    fun sendServiceCommand(action: String, serviceClass: Class<*>, data: Float) {
        val intent = Intent(context, serviceClass).apply {
            putExtra(Constants.ACTION.name, action)
            putExtra(Constants.DATA.name, data)
        }
        context.startService(intent)
    }
}