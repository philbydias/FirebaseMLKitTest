package com.test.firebasemlkit.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.IOException

class CameraPreviewView(context: Context, private val camera: Camera) : SurfaceView(context), SurfaceHolder.Callback {
    init {
        camera.setDisplayOrientation(90)
    }

    private val mHolder: SurfaceHolder = holder.apply {
        addCallback(this@CameraPreviewView)
        setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        camera.apply {
            try {
                setPreviewDisplay(holder)
                startPreview()
            } catch (e: IOException) {
            }
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        if (mHolder.surface == null) {
            return
        }

        try {
            camera.stopPreview()
        } catch (e: Exception) {
        }

        camera.apply {
            try {
                setPreviewDisplay(mHolder)
                startPreview()
            } catch (e: Exception) {
            }
        }
    }

    fun takeCurrentPicture(delegate: (Bitmap) -> Unit) {
        camera.takePicture(null, null, { bytes: ByteArray, camera: Camera ->
            camera.startPreview()
            delegate(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
        })
    }

    fun releaseResources() {
        camera.stopPreview()
        camera.release()
    }
}
