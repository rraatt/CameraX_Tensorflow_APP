package com.aboba.cameraxapp

import androidx.compose.runtime.getValue
import android.app.Activity
import android.content.Intent
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.OrientationEventListener
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.view.CameraController
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aboba.cameraxapp.ui.theme.CameraXApp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Timer
import kotlin.concurrent.timer
enum class CameraMode(val ukrString : String){
    Photo("Фото"), Video("Відео"), Cartoon("Мультік"), Classify("Розпізнання")
}

enum class TimerMode(val timerDuration: Long)
{
    ZERO(0),
    TWO(2000),
    FIVE(5000),
    TEN(10000),
    FIFTEEN(15000),
    TWENTY(20000)
}

enum class Orientation(val degree: Float)
{
    DefaultPortrait(180f),
    RightLandscape(270f),
    LeftLandscape(90f),
}

class MainActivity : ComponentActivity() {

    private var flashMode = mutableIntStateOf(ImageCapture.FLASH_MODE_OFF)

    private var cameraMode = mutableStateOf(CameraMode.Photo)

    private var currentOrientation = mutableStateOf(Orientation.DefaultPortrait)

    private var timerMode  = mutableStateOf(TimerMode.ZERO)

    private var timerStringValue = mutableStateOf("")

    lateinit var camera: Camera

    private val isRecording = mutableStateOf(false)

    private val isTimerRunning = mutableStateOf(false)

    private val isPaused = mutableStateOf(false)

    private val timePassed = mutableLongStateOf(0L)

    private var videoTimer: Timer? = null

    private var isShutterAnimationPlaying = mutableStateOf(false)

    private val cartoonizerState = mutableStateOf(false)

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val imageUri: Uri? = data?.data

                if (imageUri != null) {
                    // Do something with the selected image URI
                    editImage(imageUri)
                }
            }
        }

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in camera.REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(baseContext,
                    "Permission request denied, allow in settings",
                    Toast.LENGTH_LONG).show()
            }
        }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        camera = Camera(applicationContext)

        val orientationEventListener = object : OrientationEventListener(applicationContext, SensorManager.SENSOR_DELAY_NORMAL) {
            override fun onOrientationChanged(orientation: Int) {

                Log.i("ORIENTATIONLISTENER", "$orientation")

                val defaultPortrait = 0f
                val rightLandscape = 90f
                val leftLandscape = 270f
                when {
                    orientation == ORIENTATION_UNKNOWN ->
                    {
                        currentOrientation.value = Orientation.DefaultPortrait
                    }
                    isWithinOrientationRange(orientation.toFloat(), defaultPortrait) -> {
                        currentOrientation.value = Orientation.DefaultPortrait
                    }
                    isWithinOrientationRange(orientation.toFloat(), leftLandscape) -> {
                        currentOrientation.value = Orientation.LeftLandscape
                    }
                    isWithinOrientationRange(orientation.toFloat(), rightLandscape) -> {
                        currentOrientation.value = Orientation.RightLandscape
                    }
                }
            }

            fun isWithinOrientationRange(
                currentOrientation: Float, targetOrientation: Float, epsilon: Int = 15
            ): Boolean {
                return currentOrientation > targetOrientation - epsilon
                        && currentOrientation < targetOrientation + epsilon
            }
        }

        orientationEventListener.enable()

        requestPermissions()
        setContent {
            CameraXApp {

                val infiniteTransition = rememberInfiniteTransition(label = "")
                val cartoonizerAngle by infiniteTransition.animateFloat(
                    initialValue = 0F,
                    targetValue = -360F,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing)
                    ), label = ""
                )

                val angle = animateFloatAsState(
                    targetValue = currentOrientation.value.degree,
                    animationSpec = tween(
                        durationMillis = 250,
                        easing = LinearEasing
                    ), label = "rotate icons"
                )

                val currentRotation = -(180+angle.value)


                val cameraModePickerState = rememberFWheelPickerState()

                LaunchedEffect(cameraModePickerState) {
                    snapshotFlow { cameraModePickerState.currentIndex }
                        .collect {
                            cameraMode.value = if (cameraModePickerState.currentIndex == -1) CameraMode.Photo
                            else CameraMode.values()[cameraModePickerState.currentIndex]

                            when (cameraMode.value) {
                              CameraMode.Photo -> {
                                  camera.controller.imageCaptureTargetSize = null
                              }
                              CameraMode.Video -> {
                                  camera.controller.imageCaptureTargetSize = null
                              }
                              CameraMode.Cartoon -> {
                                  camera.controller.imageCaptureTargetSize = CameraController.OutputSize(Size(512, 512))
                              }
                              CameraMode.Classify -> {
                                  camera.controller.imageCaptureTargetSize = CameraController.OutputSize(Size(300, 300))
                              }
                            }
                        }
                }

                if ((cameraMode.value == CameraMode.Classify || cameraMode.value == CameraMode.Video) && flashMode.intValue == ImageCapture.FLASH_MODE_ON) {
                    camera.controller.enableTorch(true)
                }
                else{
                    camera.controller.enableTorch(false)
                }


                val (_, width) = LocalConfiguration.current.run { screenHeightDp.dp to screenWidthDp.dp }

                var frame by remember {
                    mutableIntStateOf(30)
                }

                Surface (
                    modifier = Modifier.fillMaxSize()
                ){
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        CameraPreview(
                            controller = camera.controller,
                            modifier = Modifier
                                .fillMaxSize()
                        )

                        val animatedAlpha by animateFloatAsState(
                            targetValue = if (isShutterAnimationPlaying.value) 1.0f else 0f,
                            label = "alpha"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = animatedAlpha
                                }
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black)
                        ) {
                        }



                        fun formatMillisecondsToTime(milliseconds: Long): String {
                            val totalSeconds = milliseconds / 1000
                            val minutes = totalSeconds / 60
                            val seconds = totalSeconds % 60

                            return String.format("%02d:%02d", minutes, seconds)
                        }

                        if (isRecording.value)
                        {
                            Text(text = formatMillisecondsToTime(timePassed.longValue),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .scale(3f)
                                    .offset(y = 30.dp)
                            )
                        }

                        if (isTimerRunning.value)
                        {
                            Text(text = timerStringValue.value,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .scale(10f)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(red = 0f, green = 0f, blue = 0f, alpha = 0.5f))
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            if (cameraMode.value == CameraMode.Classify)
                            {
                                if (frame == 0)
                                {
                                    camera.takeClassifyPhoto(applicationContext, baseContext)
                                }

                                frame = (frame + 1) % 45
                                Text(text = camera.result.value,
                                    modifier = Modifier.padding(vertical = 15.dp),
                                    fontSize = 40.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 35.sp
                                )
                            }else
                            {
                                Box(
                                    modifier = Modifier
                                )
                                {

                                    if (cameraMode.value == CameraMode.Video)
                                    {
                                        IconButton(
                                            onClick = {

                                            },
                                            modifier = Modifier.rotate(currentRotation)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Timer,
                                                contentDescription = "Timer",
                                                tint = Color.Gray
                                            )
                                        }
                                    }else
                                    {
                                        IconButton(
                                            onClick = {
                                                timerMode.value = TimerMode.values()[(timerMode.value.ordinal + 1) % TimerMode.values().size]
                                            },
                                            modifier = Modifier.rotate(currentRotation)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Timer,
                                                contentDescription = "Timer",
                                            )
                                        }

                                        Icon(imageVector = Icons.Default.Circle,
                                            contentDescription = "Timer",
                                            modifier = Modifier
                                                .scale(0.7f)
                                                .align(Alignment.Center)
                                        )

                                        Text(
                                            text = "${timerMode.value.timerDuration/1000}",
                                            modifier = Modifier
                                                .align(alignment = Alignment.Center)
                                                .rotate(currentRotation),
                                            color = Color(0f,0f,0f, 0.5f),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize =10.sp
//                                    modifier = Modifier.offset(x = , y = )
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        flashMode.intValue = if (flashMode.intValue == ImageCapture.FLASH_MODE_ON) ImageCapture.FLASH_MODE_OFF else ImageCapture.FLASH_MODE_ON
                                    },
                                    modifier = Modifier.rotate(currentRotation)
                                ) {
                                    var flashIcon = Icons.Default.FlashOff
                                    if (flashMode.intValue == ImageCapture.FLASH_MODE_ON)
                                    {
                                        flashIcon = Icons.Default.FlashOn
                                    }
                                    Icon(
                                        imageVector = flashIcon,
                                        contentDescription = "Flash"
                                    )
                                }
                            }

                        }

                        Column (
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .fillMaxWidth()
                                .background(Color(red = 0f, green = 0f, blue = 0f, alpha = 0.5f))
                        ) {
                            if (!isRecording.value && !isTimerRunning.value) {
                                FHorizontalWheelPicker(
                                    modifier = Modifier
                                        .height(60.dp)
                                        .fillMaxWidth(),

                                    // Specified item count.
                                    count = CameraMode.values().size,
                                    // DO NOT TOUCH THIS UNDER ANY CIRCUMSTANCES
                                    itemWidth = width / 3,
                                    state = cameraModePickerState,
                                    focus = {
                                        // Custom divider.
                                        FWheelPickerFocusHorizontal(
                                            dividerColor = Color.Transparent,
                                            dividerSize = 2.dp
                                        )
                                    },

                                    ) { index ->
                                    Text(
                                        CameraMode.values()[index].ukrString,
                                        fontSize = 20.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .width(150.dp)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .height(75.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if(cameraMode.value == CameraMode.Classify)
                                {
                                    IconButton(
                                        onClick = {
                                            flashMode.intValue = if (flashMode.intValue == ImageCapture.FLASH_MODE_ON) ImageCapture.FLASH_MODE_OFF else ImageCapture.FLASH_MODE_ON
                                        },
                                        modifier = Modifier.rotate(currentRotation)
                                    ) {
                                        var flashIcon = Icons.Default.FlashOff
                                        if (flashMode.intValue == ImageCapture.FLASH_MODE_ON)
                                        {
                                            flashIcon = Icons.Default.FlashOn
                                        }
                                        Icon(
                                            imageVector = flashIcon,
                                            contentDescription = "Flash"
                                        )
                                    }
                                }else
                                {
                                    IconButton(
                                        onClick = {
                                            openGallery()
                                        },
                                        modifier = Modifier.rotate(currentRotation)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Photo,
                                            contentDescription = "Gallery",
                                        )
                                    }
                                }


                                if (isRecording.value)
                                {
                                    Row(
                                        modifier = Modifier,
                                        verticalAlignment = Alignment.CenterVertically

                                    ){
                                        Button(
                                            onClick = {
                                                if (!isPaused.value)
                                                {
                                                    camera.recording?.pause()
                                                    isPaused.value = true
                                                }else
                                                {
                                                    camera.recording?.resume()
                                                    isPaused.value = false
                                                }
                                            },
                                            shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                                        ) {
                                            if (!isPaused.value)
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Pause,
                                                    contentDescription = "Pause video capture"
                                                )
                                            }
                                            else
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Pause video capture"
                                                )
                                            }

                                        }

                                        Divider(
                                            color = Color.Black,
                                            modifier = Modifier  //fill the max height
                                                .width(1.dp)
                                        )

                                        Button(
                                            onClick = {
                                                videoTimer?.cancel()
                                                videoTimer?.purge()
                                                camera.recordVideo(
                                                    applicationContext = applicationContext,
                                                    baseContext = baseContext,
                                                    contentResolver = contentResolver
                                                )
                                                isRecording.value = false
                                                timePassed.longValue = 0L
                                                isPaused.value = false
                                            },
                                            shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Stop,
                                                contentDescription = "Stop video capture"
                                            )
                                        }
                                    }
                                }
                                else
                                {
                                    if (cameraMode.value != CameraMode.Classify)
                                    {
                                        IconButton(
                                            onClick = {
                                                if (cameraMode.value == CameraMode.Photo && timerMode.value != TimerMode.ZERO)
                                                {

                                                    val timer = object: CountDownTimer(timerMode.value.timerDuration, 1000) {
                                                        override fun onTick(millisUntilFinished: Long) {
                                                            isTimerRunning.value = true
                                                            timerStringValue.value = "${(millisUntilFinished + 1000) / 1000}"
                                                        }

                                                        override fun onFinish()
                                                        {
                                                            isTimerRunning.value = false
                                                            isShutterAnimationPlaying.value = true
                                                            GlobalScope.launch {
                                                                suspend {
                                                                    delay(250)
                                                                    isShutterAnimationPlaying.value = false
                                                                }.invoke()
                                                            }
                                                            camera.takePhoto(
                                                                applicationContext = applicationContext,
                                                                baseContext = baseContext,
                                                                contentResolver = contentResolver,
                                                                flashMode = flashMode.intValue
                                                            )
                                                            timerStringValue.value = ""
                                                        }
                                                    }

                                                    if (!isTimerRunning.value)
                                                    {
                                                        timer.start()
                                                    }
                                                }else if (cameraMode.value == CameraMode.Photo)
                                                {
                                                    isShutterAnimationPlaying.value = true
                                                    GlobalScope.launch {
                                                        suspend {
                                                            delay(250)
                                                            isShutterAnimationPlaying.value = false
                                                        }.invoke()
                                                    }
                                                    camera.takePhoto(
                                                        applicationContext = applicationContext,
                                                        baseContext = baseContext,
                                                        contentResolver = contentResolver,
                                                        flashMode = flashMode.intValue
                                                    )
                                                }else if (cameraMode.value == CameraMode.Cartoon && !cartoonizerState.value){
                                                    val timer = object: CountDownTimer(timerMode.value.timerDuration, 1000) {
                                                        override fun onTick(millisUntilFinished: Long) {
                                                            isTimerRunning.value = true
                                                            timerStringValue.value = "${(millisUntilFinished + 1000) / 1000}"
                                                        }

                                                        override fun onFinish()
                                                        {
                                                            isTimerRunning.value = false
                                                            isShutterAnimationPlaying.value = true
                                                            GlobalScope.launch {
                                                                suspend {
                                                                    delay(250)
                                                                    isShutterAnimationPlaying.value = false
                                                                }.invoke()
                                                            }
                                                            camera.takeCartoonPhoto(
                                                                applicationContext = applicationContext,
                                                                baseContext = baseContext,
                                                                contentResolver = contentResolver,
                                                                flashMode = flashMode.intValue,
                                                                cartoonizerState = cartoonizerState
                                                            )
                                                            timerStringValue.value = ""
                                                        }
                                                    }

                                                    if (!isTimerRunning.value)
                                                    {
                                                        timer.start()
                                                    }
                                                }
                                                else if (cameraMode.value == CameraMode.Video)
                                                {
                                                    videoTimer = timer(
                                                        period = 1000,
                                                        action = {
                                                            if (!isPaused.value)
                                                            {
                                                                timePassed.longValue += 1000
                                                            }
                                                        }
                                                    )
                                                    Log.i("CAMERA_STATE_CHECK", "${cameraMode.value}")
                                                    camera.recordVideo(
                                                        applicationContext = applicationContext,
                                                        baseContext = baseContext,
                                                        contentResolver = contentResolver
                                                    )

                                                    isRecording.value = true
                                                }

                                            },
                                            modifier = Modifier.size(100.dp)
                                        ) {
                                            Box (
                                                modifier = Modifier,

                                                ){
                                                Icon(
                                                    imageVector = Icons.Default.Circle,
                                                    contentDescription = "Take photo",
                                                    modifier = Modifier.size(85.dp),
                                                )

                                                if (cameraMode.value == CameraMode.Video)
                                                {
                                                    Icon(
                                                        imageVector = Icons.Default.Circle,
                                                        contentDescription = "Take photo",
                                                        modifier = Modifier
                                                            .size(30.dp)
                                                            .align(alignment = Alignment.Center),
                                                        tint = Color.Red
                                                    )
                                                }

                                                if (cartoonizerState.value && cameraMode.value == CameraMode.Cartoon)
                                                {
                                                    Icon(
                                                        imageVector = Icons.Default.Cached,
                                                        contentDescription = "Take photo",
                                                        modifier = Modifier
                                                            .size(70.dp)
                                                            .align(alignment = Alignment.Center)
                                                            .rotate(cartoonizerAngle),
                                                        tint = Color.Gray
                                                    )
                                                }

                                            }

                                        }
                                    }

                                }
                                IconButton(
                                    onClick = {
                                        camera.controller.cameraSelector =
                                            if (camera.controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                                CameraSelector.DEFAULT_FRONT_CAMERA
                                            } else CameraSelector.DEFAULT_BACK_CAMERA
                                    },
                                    modifier = Modifier.rotate(currentRotation)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cameraswitch,
                                        contentDescription = "Switch camera"
                                    )
                                }
                            }

                        }
                    }
                }
            }
        }
    }



    private fun openGallery() {
        val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(gallery)
    }

    private fun editImage(imageUri: Uri) {
        val intent = Intent(this, EditorActivity::class.java)
        intent.putExtra("imageUri", imageUri.toString())
        startActivity(intent)
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(camera.REQUIRED_PERMISSIONS)
    }

}