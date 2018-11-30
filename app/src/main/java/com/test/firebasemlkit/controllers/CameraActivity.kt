package com.test.firebasemlkit.controllers

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.SparseIntArray
import android.view.Surface
import android.view.View
import android.widget.Toast
import com.google.firebase.FirebaseApp
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.test.firebasemlkit.R
import com.test.firebasemlkit.use_cases.PhotoStorageProvider
import com.test.firebasemlkit.use_cases.TempPhotoStorageService
import com.test.firebasemlkit.views.CameraPreviewView
import kotlinx.android.synthetic.main.activity_camera.*
import java.io.File

class CameraActivity: AppCompatActivity() {
    private val codePermissionsCamera = 1

    private var tts: TextToSpeech? = null
    private var storageProvider: PhotoStorageProvider? = null
    private var preview: CameraPreviewView? = null

    init {
        storageProvider = TempPhotoStorageService( { successFilePath ->
            successFilePath?.let { filePath ->
                storageProvider?.retrievePhoto(File(filePath), getRotationCompensation(this@CameraActivity, applicationContext))
            }
        }, { bitmap ->
            bitmap?.let { validBitmap ->
                image_view_camera_photo.setImageBitmap(validBitmap)
                val fbVisionImage = FirebaseVisionImage.fromBitmap(validBitmap)
                val detector = FirebaseVision.getInstance().onDeviceTextRecognizer
                detector.processImage(fbVisionImage).addOnSuccessListener { firebaseVisionText: FirebaseVisionText? ->
                    firebaseVisionText?.text?.let {
                        tts = TextToSpeech(applicationContext, object: TextToSpeech.OnInitListener {
                            override fun onInit(status: Int) {
                                val map = hashMapOf( Pair(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM.toString()) )
                                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                                    override fun onError(utteranceId: String?) {
                                    }

                                    override fun onDone(utteranceId: String?) {
                                        Log.d("AppLogs", "Utterance done.")
                                    }

                                    override fun onStart(utteranceId: String?) {
                                        Log.d("AppLogs", "Utterance started.")
                                    }

                                })
                                tts?.speak(it, TextToSpeech.QUEUE_FLUSH, map)
                            }

                        })
                    }

                } .addOnFailureListener { _: java.lang.Exception ->
                }
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        button_camera_take_photo.setOnClickListener { _: View ->
            storageProvider?.let { provider ->
                preview?.takeCurrentPicture(applicationContext.filesDir, provider)
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
