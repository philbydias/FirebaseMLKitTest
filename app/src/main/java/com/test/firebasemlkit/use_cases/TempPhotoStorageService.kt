package com.test.firebasemlkit.use_cases

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.AsyncTask
import java.io.File
import java.io.FileOutputStream
import java.util.*


class TempPhotoStorageService(private val photoStorageCompletion: PhotoStorageCompletion, private val photoRetrievalCompletion: PhotoRetrievalCompletion): PhotoStorageProvider {
    override fun storePhoto(withByteArray: ByteArray, inFolder: File) {
        val tempFile = File(inFolder, "${Date().time}.jpg")
        val writer = TempPhotoWriter(tempFile, photoStorageCompletion)
        writer.execute(withByteArray)
    }

    override fun retrievePhoto(from: File, usingRotation: Int) {
        val retriever = TempPhotoRetriever(from, usingRotation, photoRetrievalCompletion)
        retriever.execute()
    }

}

private class TempPhotoWriter(private val file: File, private val photoStorageCompletion: PhotoStorageCompletion): AsyncTask<ByteArray, Void, String?>() {

    override fun doInBackground(vararg params: ByteArray?): String? {
        if ( params.isEmpty() ) {
            return null
        }

        if ( file.exists() ) {
            file.delete()
        }
        try {
            val fStream = FileOutputStream(file)
            fStream.write(params[0])
            fStream.close()
        } catch (e: Exception) {
            return null
        }
        return file.path
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)

        photoStorageCompletion(result)
    }

}

private class TempPhotoRetriever(private val file: File, private val rotation: Int, private val retrievalCompletion: PhotoRetrievalCompletion): AsyncTask<Void, Void, Bitmap?>() {

    override fun doInBackground(vararg params: Void?): Bitmap? {
        if ( !file.exists() ) {
            return null
        }

        try {
            val optionsScan = BitmapFactory.Options()
            optionsScan.inJustDecodeBounds = true
            val inputStream = file.inputStream()
            BitmapFactory.decodeStream(inputStream, null, optionsScan)
            inputStream.close()

            val options = BitmapFactory.Options()
            options.inSampleSize = calculateInSampleSize(optionsScan.outWidth, optionsScan.outHeight, 720, 1280)
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
            return if (rotation != 0) {
                val matrix = Matrix()
                matrix.postRotate(rotation.toFloat())
                val rotatedImg = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                bitmap.recycle()
                rotatedImg
            } else {
                bitmap
            }
        } catch (e: Exception) {
            return null
        }
    }

    override fun onPostExecute(result: Bitmap?) {
        super.onPostExecute(result)

        retrievalCompletion(result)
    }

    private fun calculateInSampleSize(curWidth: Int, curHeight: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1

        if (curHeight > reqHeight || curWidth > reqWidth) {
            val heightRatio = Math.round(curHeight.toFloat() / reqHeight.toFloat())
            val widthRatio = Math.round(curHeight.toFloat() / reqWidth.toFloat())
            inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio

            val totalPixels = (curWidth* curHeight).toFloat()
            val totalReqPixelsCap = (reqWidth * reqHeight * 2).toFloat()
            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++
            }
        }
        return inSampleSize
    }

}
