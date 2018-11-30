package com.test.firebasemlkit.controllers

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.SparseIntArray
import android.view.Surface
import android.view.View
import android.widget.Toast
import com.test.firebasemlkit.R
import com.test.firebasemlkit.services.*
import com.test.firebasemlkit.views.CameraPreviewView
import kotlinx.android.synthetic.main.activity_camera.*
import java.io.File

class CameraActivity: AppCompatActivity() {
    private val codePermissionsCamera = 1

    private var bProcessVision: Boolean = false
    private var textResultConsumer: TextResultConsumer? = null
    private var storageProvider: PhotoStorageProvider? = null
    private var photoProcessingProvider: PhotoProcessingProvider? = null

    private var preview: CameraPreviewView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        textResultConsumer = GoogleTTSTextResultService(applicationContext)
        textResultConsumer?.registerCompletionResponder {
            if ( bProcessVision ) {
                storageProvider?.let { provider ->
                    preview?.takeCurrentPicture(applicationContext.filesDir, provider)
                }
            }
        }

        storageProvider = TempPhotoStorageService( { successFilePath ->
            successFilePath?.let { filePath ->
                storageProvider?.retrievePhoto(File(filePath), getRotationCompensation(this@CameraActivity, applicationContext))
            }
        }, { bitmap ->
            bitmap?.let { validBitmap ->
                if ( bProcessVision ) {
                    photoProcessingProvider?.translateToText(validBitmap)
                }
            }
        })

        photoProcessingProvider = FirebasePhotoService()
        photoProcessingProvider?.registerCompletionResponder {text: String? ->
            text?.let {
                if ( bProcessVision ) {
                    textResultConsumer?.consume(it)
                }
            }
        }

        btn_camera_take_photo.setOnClickListener { _: View ->
            bProcessVision = !bProcessVision

            if ( bProcessVision ) {
                storageProvider?.let { provider ->
                    preview?.takeCurrentPicture(applicationContext.filesDir, provider)
                }
            } else {
                textResultConsumer?.abort()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), codePermissionsCamera)
        } else {
            initializeCamera()
        }
    }

    override fun onPause() {
        super.onPause()

        preview?.releaseResources()
        preview = null
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            codePermissionsCamera -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    initializeCamera()
                } else {
                    Toast.makeText(applicationContext, "This application needs access to your device's camera.", Toast.LENGTH_LONG).show()
                }
                return
            }
        }
    }

    private fun getRotationCompensation(activity: Activity, context: Context): Int {
        val orientations = SparseIntArray()
        orientations.append(Surface.ROTATION_0, 90)
        orientations.append(Surface.ROTATION_90, 0)
        orientations.append(Surface.ROTATION_180, 270)
        orientations.append(Surface.ROTATION_270, 180)

        val deviceRotation = activity.windowManager.defaultDisplay.rotation
        var rotationCompensation = orientations.get(deviceRotation)

        val cameraManager = context.getSystemService(CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList.first()
        val sensorOrientation = cameraManager
            .getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        rotationCompensation = (rotationCompensation + sensorOrientation + 270) % 360
        return rotationCompensation
    }

    private fun initializeCamera() {
        preview = getCameraInstance()?.let {
            CameraPreviewView(this, it)
        }
        preview?.also {
            layout_camera_preview.addView(it)
        }
    }

    private fun getCameraInstance(): Camera? {
        return try {
            Camera.open()
        } catch (e: Exception) {
            null
        }
    }

}
