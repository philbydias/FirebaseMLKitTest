package com.test.firebasemlkit.controllers

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.test.firebasemlkit.R
import com.test.firebasemlkit.views.CameraPreviewView
import kotlinx.android.synthetic.main.activity_camera.*

class CameraActivity : AppCompatActivity() {
    private val CODE_PERMISSIONS_CAMERA = 1

    private var preview: CameraPreviewView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
    }

    override fun onResume() {
        super.onResume()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CODE_PERMISSIONS_CAMERA)
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
            CODE_PERMISSIONS_CAMERA -> {
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
                previewView.takeCurrentPicture {
                    image_view_camera_photo.setImageBitmap(it)
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
