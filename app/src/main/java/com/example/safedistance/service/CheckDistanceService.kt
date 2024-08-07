package com.example.safedistance.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PointF
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.IBinder
import android.util.Log
import android.util.SizeF
import androidx.core.app.ActivityCompat
import com.example.safedistance.exception.NoAccessToCameraException
import com.example.safedistance.exception.NoFocalLengthInfoException
import com.example.safedistance.exception.NoFrontCameraException
import com.example.safedistance.exception.NoSensorSizeException
import com.example.safedistance.utils.Constants
import com.google.mlkit.vision.camera.CameraSourceConfig
import com.google.mlkit.vision.camera.CameraXSource
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.io.IOException
import kotlin.math.abs

class CheckDistanceService : Service() {
    companion object{
        const val IMAGE_WIDTH = 1024
        const val IMAGE_HEIGHT = 1024

        const val AVERAGE_EYE_DISTANCE = 63 // in mm
    }

    private lateinit var cameraXSource: CameraXSource
    private var distance: Float? = null
    private var focalLength: Float = 0f
    private var sensorX: Float = 0f
    private var sensorY: Float = 0f

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        super.onCreate()
        try {
            initializeParams()
            createCameraSource()
        } catch (e: Exception) {
            stopService()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val action = it.getStringExtra("ACTION")
            when (action) {
                Constants.ACTION_START_CAMERA.name -> {
                    try {
                        startCamera()
                    } catch (e: Exception) {
                        stopService()
                        return START_NOT_STICKY
                    }
                }
                Constants.ACTION_STOP_CAMERA.name -> {
                    try {
                        stopCamera()
                    } catch (e: Exception) {
                        stopService()
                        return START_NOT_STICKY
                    }
                }
                else -> Log.d("CheckDistance", "Unknown action")
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraXSource.close()
    }

    private fun initializeParams() {
        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        initCameraParams(cameraManager)
    }

    private fun initCameraParams(cameraManager: CameraManager) {
        try {
            val cameraId = getFrontCameraId(this)
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)

            val focalLengths: FloatArray? =
                cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            assignFocalLength(focalLengths, cameraId)

            val sensorSize =
                cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
            assignSensorXY(sensorSize, cameraId)
        } catch (e: NoFrontCameraException) {
            sendErrorMessage(
                "No Front Camera",
                "This device does not have a front camera. " +
                        "The application will now close."
            )
            throw e
        } catch (e: NoSensorSizeException) {
            sendErrorMessage(
                "No Sensor size info",
                "This camera doesn't get an information about sensor size. " +
                        "The application will now close."
            )
            throw e
        } catch (e: NoFocalLengthInfoException) {
            sendErrorMessage(
                "No focal length info",
                "This camera doesn't have an information about focal length. " +
                        "The application will now close."
            )
            throw e
        }
    }

    private fun stopService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @Throws(NoFrontCameraException::class)
    private fun getFrontCameraId(context: Context): String {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                    return cameraId
                }
            }
        } catch (e: Exception) {
            throw NoFrontCameraException("Error accessing camera: ${e.message}")
        }
        throw NoAccessToCameraException("No front camera found")
    }

    @Throws(NoFocalLengthInfoException::class)
    private fun assignFocalLength(focalLengths: FloatArray?, cameraId: String) {
        if (focalLengths != null && focalLengths.isNotEmpty()) {
            // Retrieving the first focal length
            // TODO add select for user if not correct value
            focalLength = focalLengths[0]
        } else {
            throw NoFocalLengthInfoException(
                "For camera: $cameraId Focal Length info isn't available."
            )
        }
    }

    @Throws(NoSensorSizeException::class)
    private fun assignSensorXY(sensorSize: SizeF?, cameraId: String) {
        if (sensorSize != null) {
            sensorX = sensorSize.width
            sensorY = sensorSize.height
        } else {
            throw NoSensorSizeException(
                "For camera: $cameraId Sensor size info isn't available."
            )
        }
    }

    private fun sendErrorMessage(title: String, message: String) {
        val intent = Intent(Constants.ACTION_CLOSE_ALL_ACTIVITIES.name).apply {
            setPackage(packageName)
            putExtra(Constants.VALUE_TITLE.name, title)
            putExtra(Constants.VALUE_MESSAGE.name, message)
        }
        sendBroadcast(intent)
    }

    private fun sendDistance(distance: Float?) {
        val intent = Intent(Constants.ACTION_DISTANCE.name).apply {
            setPackage(packageName)
            if(distance != null)
                putExtra(Constants.VALUE_DISTANCE.name, distance)
        }
        sendBroadcast(intent)
    }

    private fun createCameraSource(){
        val highAccuracyOpts = faceDetectorOptions()
        val detector = FaceDetection.getClient(highAccuracyOpts)

        val cameraSourceConfig = cameraSourceConfig(detector)

        cameraXSource = CameraXSource(cameraSourceConfig)
        Log.d("MyCamera", "createCameraSource: ${cameraXSource.previewSize}")
    }

    @SuppressLint("MissingPermission") // permission has checked in isGrantedPermissionForCamera function
    private fun startCamera() {
        try {
            if (!isGrantedPermissionForCamera()) {
                return
            }
            cameraXSource.start()
        } catch (e: IOException) {
            e.printStackTrace()
            sendErrorMessage(e.cause?.message?:"Camera failure!", e.message?: "No extra message")
            throw e
        }
    }

    @SuppressLint("MissingPermission") // permission has checked in isGrantedPermissionForCamera function
    private fun stopCamera() {
        try {
            if (!isGrantedPermissionForCamera()) {
                return
            }
            cameraXSource.stop()
            sendDistance(null)
        } catch (e: IOException) {
            e.printStackTrace()
            sendErrorMessage(e.cause?.message?:"Camera failure!", e.message?: "No extra message")
            throw e
        }
    }

    private fun cameraSourceConfig(detector: FaceDetector): CameraSourceConfig {
        val cameraSourceConfig = CameraSourceConfig.Builder(this, detector) {
            it.addOnSuccessListener { faces ->
                this.distance = null
                for (face in faces) {
                    val leftEyePos: PointF? = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
                    val rightEyePos: PointF? = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position

                    if (leftEyePos != null && rightEyePos != null) {
                        val deltaX: Float = abs(leftEyePos.x - rightEyePos.x)
                        val deltaY: Float = abs(leftEyePos.y - rightEyePos.y)

                        val distance: Float = if (deltaX >= deltaY) {
                            focalLength * (AVERAGE_EYE_DISTANCE / sensorX) * (IMAGE_WIDTH / deltaX)
                        } else {
                            focalLength * (AVERAGE_EYE_DISTANCE / sensorY) * (IMAGE_HEIGHT / deltaY)
                        }

                        this.distance = distance
                    }
                }
                sendDistance(this.distance)
            }

            it.addOnFailureListener { e ->
                Log.e("Camera", e.message?:"Problem with calculation of distance")
            }

        }.setFacing(CameraSourceConfig.CAMERA_FACING_FRONT).build()
        return cameraSourceConfig
    }

    private fun faceDetectorOptions(): FaceDetectorOptions {
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .build()
        return highAccuracyOpts
    }

    private fun isGrantedPermissionForCamera() = (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED)

    private fun startMeasure() {
        TODO("Not yet implemented")
    }

    private fun stopMeasure() {
        TODO("Not yet implemented")
    }
}