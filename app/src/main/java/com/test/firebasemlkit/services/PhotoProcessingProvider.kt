package com.test.firebasemlkit.services

import android.graphics.Bitmap

interface PhotoProcessingProvider {
    fun registerCompletionResponder(responder: (text: String?) -> Unit)
    fun translateToText(fromBitmap: Bitmap)
}