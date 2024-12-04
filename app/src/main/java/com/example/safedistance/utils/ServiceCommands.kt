package com.example.safedistance.utils

import android.content.Context
import android.content.Intent

class ServiceCommands(
    private val context: Context
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