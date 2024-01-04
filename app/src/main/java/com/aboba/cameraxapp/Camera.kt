package com.aboba.cameraxapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.video.AudioConfig
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.tensorflow.lite.task.vision.classifier.Classifications
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.label.Category
import java.io.IOException
import java.util.UUID
import kotlin.math.min

@Composable
fun CameraPreview(
    controller: LifecycleCameraController,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        factory = {
            PreviewView(it).apply {
                this.controller = controller
                controller.bindToLifecycle(lifecycleOwner)
            }
        },
        modifier = modifier
    )
}
class Camera(applicationContext: Context): ImageClassifierHelper.ClassifierListener
{
    var recording: Recording? = null
    val cartoonizer: Cartoonizer = Cartoonizer(applicationContext)

    val controller = LifecycleCameraController(applicationContext).apply {
            setEnabledUseCases(
                CameraController.IMAGE_CAPTURE or
                        CameraController.VIDEO_CAPTURE
            )
        }

    var analyze: ImageClassifierHelper = ImageClassifierHelper(context = applicationContext, imageClassifierListener = this)

    var result = mutableStateOf("")

    val REQUIRED_PERMISSIONS =
        mutableListOf (
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        ).toTypedArray()

    private fun hasRequiredPermissions(applicationContext: Context): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                applicationContext,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun takePhoto(
        applicationContext: Context,
        baseContext: Context,
        contentResolver: ContentResolver,
        flashMode: Int
    ) {
        if(!hasRequiredPermissions(applicationContext)) {
            Toast.makeText(baseContext,
                "Permission request denied, allow in settings",
                Toast.LENGTH_LONG).show()
            return
        }
        controller.imageCaptureFlashMode = flashMode
        controller.takePicture(

            ContextCompat.getMainExecutor(applicationContext),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    val matrix = Matrix().apply {
                        postRotate(image.imageInfo.rotationDegrees.toFloat())
                    }
                    val rotatedBitmap = Bitmap.createBitmap(
                        image.toBitmap(),
                        0,
                        0,
                        image.width,
                        image.height,
                        matrix,
                        true
                    )
                    savePhotoToExternalStorage(UUID.randomUUID().toString(), rotatedBitmap, contentResolver)
                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Log.e("Camera", "Couldn't take photo: ", exception)
                }
            }
        )
    }

    fun takeClassifyPhoto(
        applicationContext: Context,
        baseContext: Context,
    ) {
        if(!hasRequiredPermissions(applicationContext)) {
            Toast.makeText(baseContext,
                "Permission request denied, allow in settings",
                Toast.LENGTH_LONG).show()
            return
        }
        controller.takePicture(

            ContextCompat.getMainExecutor(applicationContext),
            object : ImageCapture.OnImageCapturedCallback() {
                @SuppressLint("SuspiciousIndentation")
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    val matrix = Matrix().apply {
                        postRotate(image.imageInfo.rotationDegrees.toFloat())
                    }
                    val rotatedBitmap = Bitmap.createBitmap(
                        image.toBitmap(),
                        0,
                        0,
                        image.width,
                        image.height,
                        matrix,
                        true
                    )
                        // Pass Bitmap and rotation to the image classifier helper for processing and classification
                        analyze.classify(rotatedBitmap, 0)

                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Log.e("Camera", "Couldn't take photo: ", exception)
                }
            }
        )
    }

    fun takeCartoonPhoto(applicationContext: Context,
                         baseContext: Context,
                         contentResolver: ContentResolver,
                         flashMode: Int,
                         cartoonizerState: MutableState<Boolean>){
        if(!hasRequiredPermissions(applicationContext)) {
            Toast.makeText(baseContext,
                "Permission request denied, allow in settings",
                Toast.LENGTH_LONG).show()
            return
        }

        controller.imageCaptureFlashMode = flashMode
        controller.takePicture(

            ContextCompat.getMainExecutor(applicationContext),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    val matrix = Matrix().apply {
                        postRotate(image.imageInfo.rotationDegrees.toFloat())
                    }
                    val rotatedBitmap = Bitmap.createBitmap(
                        image.toBitmap(),
                        0,
                        0,
                        image.width,
                        image.height,
                        matrix,
                        true
                    )

                    val notification = Notification(applicationContext)

                    GlobalScope.launch {
                        suspend {
                            cartoonizerState.value = true
                            val cartoon = cartoonizer.makeCartoon(rotatedBitmap)
                            savePhotoToExternalStorage(UUID.randomUUID().toString(), cartoon, contentResolver)
                            image.close()

                            cartoonizerState.value = false
                            notification.sendNotification("Cartoonized image ready.", "Your cartoonized image is ready, check it out in gallery.")
                        }.invoke()
                    }

                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Log.e("Camera", "Couldn't take photo: ", exception)
                }
            })

    }

    @SuppressLint("MissingPermission")
    fun recordVideo(
        applicationContext: Context,
        baseContext: Context,
        contentResolver: ContentResolver,
    ) {
        if (recording != null) {
            recording?.stop()
            recording = null
            return
        }

        if (!hasRequiredPermissions(applicationContext)) {
            Toast.makeText(baseContext,
                "Permission request denied, allow in settings",
                Toast.LENGTH_LONG).show()
            return
        }
        val fileName = "${UUID.randomUUID()}.mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
        }
        val mediaStoreOutput = MediaStoreOutputOptions.Builder(contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        recording = controller.startRecording(
            mediaStoreOutput,
            AudioConfig.create(true),
            ContextCompat.getMainExecutor(applicationContext),
        ) { event ->
            when (event) {
                is VideoRecordEvent.Finalize -> {
                    if (event.hasError()) {
                        recording?.close()
                        recording = null


                    }
                }
            }
        }
    }
        @SuppressLint("NotifyDataSetChanged")
    override fun onError(error: String) {
        Log.i("Clasificator", error)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResults(
        results: List<Classifications>?,
        inferenceTime: Long
    ) {
        Log.i("Classificator", results.toString())

        val categories:MutableList<Category?>  = MutableList(1) { null }

        results?.let { it ->
            if (it.isNotEmpty()) {
                val sortedCategories = it[0].categories.sortedBy { it?.score }
                val min = min(sortedCategories.size, categories.size)
                for (i in 0 until min) {
                    categories[i] = sortedCategories[i]
                }
            }
        }
        analyze.englishUkrainianTranslator?.translate(categories[0]?.label ?: "")
            ?.addOnSuccessListener { translatedText ->
                result.value = translatedText
            }
            ?.addOnFailureListener {
                result.value = categories[0]?.label ?: ""}
            }
    

}


fun savePhotoToExternalStorage(displayName: String, bmp: Bitmap, contentResolver: ContentResolver): Boolean {
    val imageCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    Log.i("SAVE", "${imageCollection.path}")
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.WIDTH, bmp.width)
        put(MediaStore.Images.Media.HEIGHT, bmp.height)
    }
    return try {
        contentResolver.insert(imageCollection, contentValues)?.also { uri ->
            contentResolver.openOutputStream(uri).use { outputStream ->
                if(!outputStream?.let { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }!!) {
                    throw IOException("Couldn't save bitmap")
                }
            }
        } ?: throw IOException("Couldn't create MediaStore entry")
        true
    } catch(e: IOException) {
        e.printStackTrace()
        false
    }
}

