package com.example.safedistance

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import com.example.safedistance.service.CheckDistanceService
import com.example.safedistance.service.VibratorService
import com.example.safedistance.ui.theme.SafeDistanceTheme
import com.example.safedistance.utils.Constants
import com.example.safedistance.utils.ServiceCommands
import com.example.safedistance.view.ScreenDistanceView

class MainActivity : ComponentActivity() {
    private lateinit var serviceCommands: ServiceCommands
    private lateinit var screenDistanceView: ScreenDistanceView

    private val cameraPermissionRequestLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted: proceed with opening the camera
                initCamera()
                initVibration()
            } else {
                // Permission denied: inform the user to enable it through settings
                toastInformUserToGrantedPermission()
            }
        }

    private val multiplePermissionRequestLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionsGranted: Map<String, Boolean> ->
            permissionsGranted.forEach {
                permission ->
                run {
                    if(!permission.value)
                        // Permission denied: inform the user to enable it through settings
                        toastInformUserToGrantedPermission()
                    if(permission.key == Manifest.permission.CAMERA && permission.value) {
                        // Permission granted: proceed with opening the camera
                        initCamera()
                        initVibration()
                    }
                }
            }
        }

    private val activitiesReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Constants.ACTION_CLOSE_ALL_ACTIVITIES.name -> handleCloseAllActivities(intent)
                Constants.ACTION_DISTANCE.name -> screenDistanceView.handleDistance(intent)
            }
        }
    }

    private fun MainActivity.toastInformUserToGrantedPermission() {
        Toast.makeText(
            this,
            "Go to settings and enable permissions to use this app",
            Toast.LENGTH_SHORT
        ).show()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SafeDistanceTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    screenDistanceView = ScreenDistanceView(serviceCommands)
                    screenDistanceView.View()
                }
            }
        }

        registerBroadcastReceiver()
        serviceCommands = ServiceCommands(this)

        if(isGrantedPermissionForCamera() && isGrantedPermissionForNotification()) {
            initCamera()
            initVibration()
        } else {
            requestPermissionsFromUser()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(activitiesReceiver)
    }

    private fun handleCloseAllActivities(intent: Intent) {
        val title = intent.getStringExtra(Constants.VALUE_TITLE.name)
        val message = intent.getStringExtra(Constants.VALUE_MESSAGE.name)
        serviceCommands.sendServiceCommand(
            Constants.ACTION_CLOSE_ALL_SERVICES.name,
            CheckDistanceService::class.java)
        serviceCommands.sendServiceCommand(
            Constants.ACTION_CLOSE_ALL_SERVICES.name,
            VibratorService::class.java)
        showErrorDialog(title, message)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag") // For Android version < TIRAMISU
    private fun registerBroadcastReceiver() {
        val filter = IntentFilter()
        filter.addAction(Constants.ACTION_CLOSE_ALL_ACTIVITIES.name)
        filter.addAction(Constants.ACTION_DISTANCE.name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            registerReceiver(activitiesReceiver, filter, RECEIVER_NOT_EXPORTED)
        else
            registerReceiver(activitiesReceiver, filter)
    }

    private fun requestPermissionsFromUser() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, "Please grant require permissions", Toast.LENGTH_SHORT).show()
            multiplePermissionRequestLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.CAMERA))
        } else {
            Toast.makeText(this, "Please grant permission to the camera", Toast.LENGTH_SHORT).show()
            cameraPermissionRequestLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun isGrantedPermissionForNotification(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
                return true
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                            == PackageManager.PERMISSION_GRANTED)
    }


    private fun isGrantedPermissionForCamera() = (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED)

    private fun initVibration() {
        val serviceIntent = Intent(this, VibratorService::class.java)
        startService(serviceIntent)
    }

    private fun initCamera() {
        val serviceIntent = Intent(this, CheckDistanceService::class.java)
        startService(serviceIntent)
    }

    private fun showErrorDialog(title: String?, message: String?) {
        AlertDialog.Builder(this)
            .setTitle(title ?: "No title")
            .setMessage(message ?: "No description")
            .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
            .setOnDismissListener { finish() }
            .show()
    }
}
