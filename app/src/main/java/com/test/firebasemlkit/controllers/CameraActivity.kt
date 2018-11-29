package com.test.firebasemlkit.controllers

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.test.firebasemlkit.R
import com.test.firebasemlkit.use_cases.PhotoStorageProvider
import com.test.firebasemlkit.use_cases.TempPhotoStorageService
import com.test.firebasemlkit.views.CameraPreviewView
import kotlinx.android.synthetic.main.activity_camera.*
import java.io.File

class CameraActivity: AppCompatActivity() {
    private val codePermissionsCamera = 1

    private var storageProvider: PhotoStorageProvider? = null
    private var preview: CameraPreviewView? = null

    init {
        storageProvider = TempPhotoStorageService( { successFilePath ->
            Log.d("AppLogs", "File stored at: $successFilePath")
            successFilePath?.let { filePath ->
                Log.d("AppLogs", "Retrieving from ${File(filePath)}")
                storageProvider?.retrievePhoto(File(filePath))
            }
        }, { bitmap ->
            Log.d("AppLogs", "Retrieved bitmap: ${bitmap == null}")
            bitmap.let { validBitmap ->
                image_view_camera_photo.setImageBitmap(validBitmap)
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
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

    private fun initializeCamera() {
        preview = getCameraInstance()?.let {
            CameraPreviewView(this, it)
        }
        preview?.also {
            layout_camera_preview.addView(it)
        }

        button_camera_take_photo.setOnClickListener { _: View ->
            preview?.let { previewView ->
                storageProvider?.let { provider ->
                    previewView.takeCurrentPicture(applicationContext.filesDir, provider)
                }
            }
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
