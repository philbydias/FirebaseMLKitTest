package com.test.firebasemlkit.services

import android.graphics.Bitmap
import java.io.File

typealias PhotoStorageCompletion = (path: String?) -> Unit
typealias PhotoRetrievalCompletion = (bitmap: Bitmap?) -> Unit

interface PhotoStorageProvider {
    fun storePhoto(withByteArray: ByteArray, withCameraSize: android.hardware.Camera.Size, inFolder: File)
    fun retrievePhoto(from: File, usingRotation: Int)
}