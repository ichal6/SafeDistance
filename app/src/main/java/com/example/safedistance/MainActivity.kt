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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.safedistance.service.CheckDistanceService
import com.example.safedistance.service.VibratorService
import com.example.safedistance.ui.theme.SafeDistanceTheme
import com.example.safedistance.utils.Constants
import com.example.safedistance.utils.ServiceCommands

class MainActivity : ComponentActivity() {
    private lateinit var serviceCommands: ServiceCommands

    private var outputMessage: MutableState<String?> = mutableStateOf("")
    private var distance: MutableState<Float?> = mutableStateOf(0.0f)
    private var setDistance: MutableState<Float?> = mutableStateOf(0.0f)

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
                Constants.ACTION_DISTANCE.name -> handleDistance(intent)
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
                    ScreenDistanceView(outputMessage)
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

    private fun handleDistance(intent: Intent) {
        if (intent.hasExtra(Constants.VALUE_DISTANCE.name)) {
            distance.value = intent.getFloatExtra(Constants.VALUE_DISTANCE.name, 0.0F)
            outputMessage.value = "Distance: %.0f mm".format(distance.value)
        } else {
            outputMessage.value = "No face detection!"
        }
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

    @Composable
    fun ScreenDistanceView(message: MutableState<String?>){
        val backgroundColor = Color.Black
        val boxColor = Color(0xFF222222)
        val textColor = Color.White

        Column (modifier = Modifier
            .background(backgroundColor)
            .fillMaxSize()){
            Text(text = "Screen Distance", fontWeight = FontWeight.Bold, modifier = Modifier.offset(x = 16.dp, y = 10.dp), fontSize = 24.sp, color = textColor)
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .shadow(10.dp)
                .padding(16.dp)
                .background(boxColor, shape = RoundedCornerShape(25.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = message.value!!.ifEmpty{"Please look at the front camera"}, color = textColor)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Hold the phone straight.", color = textColor)
                    Button(
                        onClick = {
                            serviceCommands.sendServiceCommand(
                                Constants.ACTION_START_CAMERA.name,
                                CheckDistanceService::class.java)
                        },
                        modifier = Modifier
                            .padding(16.dp)
                            .defaultMinSize(minWidth = 150.dp)
                    ) {
                        Text(text = "Start camera")
                    }
                    Button(
                        onClick = {
                            serviceCommands.sendServiceCommand(
                                Constants.ACTION_STOP_CAMERA.name,
                                CheckDistanceService::class.java)
                        },
                        modifier = Modifier
                            .padding(16.dp)
                            .defaultMinSize(minWidth = 150.dp)
                    ) {
                        Text(text = "Stop camera")
                    }
                }
            }
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .shadow(10.dp)
                .padding(16.dp)
                .background(boxColor, shape = RoundedCornerShape(25.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Press button when text is sharp", color = textColor)
                    Text(
                        text = "Selected distance from face: %.0f mm".format(setDistance.value),
                        color = textColor
                    )
                    Button(
                        onClick = {
                            changeDistance()
                        },
                        modifier = Modifier
                            .padding(16.dp)
                            .defaultMinSize(minWidth = 150.dp)
                    ) {
                        Text(text = "Set sharp distance")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            serviceCommands.sendServiceCommand(
                                Constants.ACTION_STOP_MEASURE.name,
                                CheckDistanceService::class.java)
                        },
                        modifier = Modifier
                            .padding(16.dp)
                            .defaultMinSize(minWidth = 150.dp)
                    ) {
                        Text(text = "Stop measure a sharp distance")
                    }
                }
            }
        }
    }

    private fun changeDistance() {
        setDistance.value = distance.value
        if(setDistance.value != null)
            serviceCommands.sendServiceCommand(
                Constants.ACTION_START_MEASURE.name,
                CheckDistanceService::class.java,
                this.setDistance.value!!)
    }
}
