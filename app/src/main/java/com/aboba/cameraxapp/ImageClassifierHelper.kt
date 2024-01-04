package com.aboba.cameraxapp

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel


class ImageClassifierHelper(
    private var threshold: Float = 0.2f,
    private var numThreads: Int = 5,
    private var maxResults: Int = 3,
    private var currentDelegate: Int = 2,
    val context: Context,
    val imageClassifierListener: ClassifierListener?
) {
    private var imageClassifier: ImageClassifier? = null
    var englishUkrainianTranslator: com.google.mlkit.nl.translate.Translator? = null

    init {
        try{
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.UKRAINIAN)
                .build()
            englishUkrainianTranslator = Translation.getClient(options)
            englishUkrainianTranslator!!.downloadModelIfNeeded()
                .addOnSuccessListener {
                    Log.i("Translator", "downloaded")
                }
                .addOnFailureListener {
                }
            setupImageClassifier()
        }catch (e: Error){
        Log.i("Classificator", e.toString())}
    }


    private fun loadModelFile(context: Context): ByteBuffer {
        context.assets.openFd("2.tflite").use { fileDescriptor ->
            FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                val fileChannel = inputStream.channel
                val startOffset = fileDescriptor.startOffset
                val declaredLength = fileDescriptor.declaredLength
                return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            }
        }
    }

    private fun setupImageClassifier() {
        val optionsBuilder = ImageClassifier.ImageClassifierOptions.builder()
            .setScoreThreshold(threshold)
            .setMaxResults(maxResults)

        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)

        when (currentDelegate) {
            DELEGATE_CPU -> {
                // Default
            }
            DELEGATE_GPU -> {
                if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                    baseOptionsBuilder.useGpu()
                } else {
                    imageClassifierListener?.onError("GPU is not supported on this device")
                }
            }
            DELEGATE_NNAPI -> {
                baseOptionsBuilder.useNnapi()
            }
        }

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        try {
            imageClassifier =
                ImageClassifier.createFromBufferAndOptions(loadModelFile(context), optionsBuilder.build())
        } catch (e: Error) {
            imageClassifierListener?.onError(
                "Image classifier failed to initialize. See error logs for details"
            )
            Log.e(TAG, "TFLite failed to load model with error: " + e.message)
        }
    }

    fun classify(image: Bitmap, rotation: Int) {
        if (imageClassifier == null) {
            setupImageClassifier()
        }

        // Inference time is the difference between the system time at the start and finish of the
        // process
        var inferenceTime = SystemClock.uptimeMillis()

        // Create preprocessor for the image.
        // See https://www.tensorflow.org/lite/inference_with_metadata/
        //            lite_support#imageprocessor_architecture
        val imageProcessor =
            ImageProcessor.Builder()
                .build()
        Log.i("Classificator", "Options set 0")

        // Preprocess the image and convert it into a TensorImage for classification.
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))
        Log.i("Classificator", "Options set 1")
        val imageProcessingOptions = ImageProcessingOptions.builder()
            .setOrientation(getOrientationFromRotation(rotation))
            .build()
        Log.i("Classificator", "Options set 2")
        val results = imageClassifier?.classify(tensorImage, imageProcessingOptions)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime
        Log.i("Classificator", "Options set 3")
        imageClassifierListener?.onResults(
            results,
            inferenceTime
        )
    }

    // Receive the device rotation (Surface.x values range from 0->3) and return EXIF orientation
    // http://jpegclub.org/exif_orientation.html
    private fun getOrientationFromRotation(rotation: Int) : ImageProcessingOptions.Orientation {
        return when (rotation) {
            Surface.ROTATION_270 ->
                ImageProcessingOptions.Orientation.BOTTOM_RIGHT

            Surface.ROTATION_180 ->
                ImageProcessingOptions.Orientation.RIGHT_BOTTOM

            Surface.ROTATION_90 ->
                ImageProcessingOptions.Orientation.TOP_LEFT

            else ->
                ImageProcessingOptions.Orientation.RIGHT_TOP
        }
    }

    interface ClassifierListener {
        fun onError(error: String)
        fun onResults(
            results: List<Classifications>?,
            inferenceTime: Long
        )
    }

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DELEGATE_NNAPI = 2
        private const val TAG = "com.aboba.cameraxapp.ImageClassifierHelper"
    }
}
