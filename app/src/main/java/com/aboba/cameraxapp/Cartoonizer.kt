package com.aboba.cameraxapp

import android.content.Context
import android.graphics.Bitmap
import com.aboba.cameraxapp.ml.WhiteboxCartoonGanDr
import org.tensorflow.lite.support.image.TensorImage


class Cartoonizer(applicationContext: Context){

    val appContext = applicationContext
    
    fun makeCartoon(bitmap: Bitmap): Bitmap{
        val sourceImage = TensorImage.fromBitmap(bitmap)
        val cartoonizedImage: TensorImage = inferenceWithDrModel(sourceImage)
        return cartoonizedImage.bitmap
    }

    /**
     * Run inference with the dynamic range tflite model
     */
    private fun inferenceWithDrModel(sourceImage: TensorImage): TensorImage {
        val model = WhiteboxCartoonGanDr.newInstance(appContext)

        // Runs model inference and gets result.
        val outputs = model.process(sourceImage)
        val cartoonizedImage = outputs.cartoonizedImageAsTensorImage

        // Releases model resources if no longer used.
        model.close()

        return cartoonizedImage
    }

}