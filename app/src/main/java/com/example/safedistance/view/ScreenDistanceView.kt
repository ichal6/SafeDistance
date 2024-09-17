package com.example.safedistance.view

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safedistance.service.CheckDistanceService
import com.example.safedistance.utils.Constants
import com.example.safedistance.utils.ServiceCommands


class ScreenDistanceView(private val serviceCommands: ServiceCommands) {
    private val setDistance: MutableState<Float?> = mutableStateOf(0.0f)
    private var distance: MutableState<Float?> = mutableStateOf(0.0f)
    private var outputMessage: MutableState<String?> = mutableStateOf("")


    @Composable
    fun View() {
        val backgroundColor = Color.Black
        val boxColor = Color(0xFF222222)
        val textColor = Color.White

        Column(
            modifier = Modifier
                .background(backgroundColor)
                .fillMaxSize()
        ) {
            Text(
                text = "Screen Distance",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(x = 16.dp, y = 10.dp),
                fontSize = 24.sp,
                color = textColor
            )
            Box(
                modifier = Modifier
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
                    Text(
                        text = outputMessage.value!!.ifEmpty { "Please look at the front camera" },
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Hold the phone straight.", color = textColor)
                    Button(
                        onClick = {
                            serviceCommands.sendServiceCommand(
                                Constants.ACTION_START_CAMERA.name,
                                CheckDistanceService::class.java
                            )
                        },
                        modifier = Modifier
                            .padding(16.dp)
                            .defaultMinSize(minWidth = 150.dp)
                            .testTag(Constants.TAG_START_CAMERA_BUTTON.name)
                    ) {
                        Text(text = "Start camera")
                    }
                    Button(
                        onClick = {
                            serviceCommands.sendServiceCommand(
                                Constants.ACTION_STOP_CAMERA.name,
                                CheckDistanceService::class.java
                            )
                        },
                        modifier = Modifier
                            .padding(16.dp)
                            .defaultMinSize(minWidth = 150.dp)
                    ) {
                        Text(text = "Stop camera")
                    }
                }
            }
            Box(
                modifier = Modifier
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
                                CheckDistanceService::class.java
                            )
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

    fun handleDistance(intent: Intent) {
        if (intent.hasExtra(Constants.VALUE_DISTANCE.name)) {
            distance.value = intent.getFloatExtra(Constants.VALUE_DISTANCE.name, 0.0F)
            outputMessage.value = "Distance: %.0f mm".format(distance.value)
        } else {
            outputMessage.value = "No face detection!"
        }
    }

    private fun changeDistance() {
        setDistance.value = distance.value
        if (setDistance.value != null)
            serviceCommands.sendServiceCommand(
                Constants.ACTION_START_MEASURE.name,
                CheckDistanceService::class.java,
                this.setDistance.value!!
            )
    }
}
