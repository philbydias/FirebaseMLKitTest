package com.test.firebasemlkit.services

import android.graphics.Bitmap
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText

class FirebasePhotoService: PhotoProcessingProvider {
    private var responder: ( (text: String?) -> Unit )? = null

    override fun registerCompletionResponder(responder: (text: String?) -> Unit) {
        this.responder = responder
    }

    override fun translateToText(fromBitmap: Bitmap) {
        val fbVisionImage = FirebaseVisionImage.fromBitmap(fromBitmap)
        val detector = FirebaseVision.getInstance().onDeviceTextRecognizer
        detector.processImage(fbVisionImage).addOnSuccessListener { firebaseVisionText: FirebaseVisionText? ->
            firebaseVisionText?.text?.let {
                responder?.invoke(it)
            }
        } .addOnFailureListener { _: java.lang.Exception ->
            responder?.invoke(null)
        }
    }

}