package com.aboba.cameraxapp

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import ja.burhanrashid52.photoeditor.OnPhotoEditorListener
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.PhotoEditorView
import ja.burhanrashid52.photoeditor.PhotoFilter
import ja.burhanrashid52.photoeditor.SaveSettings
import ja.burhanrashid52.photoeditor.ViewType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.UUID

var whiteColor = 0xFFFFFFFF
class TextEdited
{
    var rootView = mutableStateOf<View?>(null)
    var text = mutableStateOf("")
    var color = mutableIntStateOf(whiteColor.toInt())
}



class PhotoEditor {
    var isEditorMenuShown = mutableStateOf(true)

    var isTextEditing = mutableStateOf(false)

    private var isEmojiAdding = mutableStateOf(false)

    private var isFilterAplying = mutableStateOf(false)

    private var isBrushCustomized = mutableStateOf(false)

    val scrollState: ScrollState = ScrollState(0)

    var textEdited = TextEdited()
    private var isNewText = mutableStateOf(false)

    private var filterMap = mapOf(
        "original" to PhotoFilter.NONE,
        "auto_fix" to PhotoFilter.AUTO_FIX,
        "brightness" to PhotoFilter.BRIGHTNESS,
        "contrast" to PhotoFilter.CONTRAST,
        "documentary" to PhotoFilter.DOCUMENTARY,
        "dual_tone" to PhotoFilter.DUE_TONE,
        "fill_light" to PhotoFilter.FILL_LIGHT,
        "fish_eye" to PhotoFilter.FISH_EYE,
        "grain" to PhotoFilter.GRAIN,
        "gray_scale" to PhotoFilter.GRAY_SCALE,
        "lomish" to PhotoFilter.LOMISH,
        "negative" to PhotoFilter.NEGATIVE,
        "posterize" to PhotoFilter.POSTERIZE,
        "saturate" to PhotoFilter.SATURATE,
        "sepia" to PhotoFilter.SEPIA,
        "sharpen" to PhotoFilter.SHARPEN,
        "temprature" to PhotoFilter.TEMPERATURE,
        "tint" to PhotoFilter.TINT,
        "vignette" to PhotoFilter.VIGNETTE,
        "cross_process" to PhotoFilter.CROSS_PROCESS,
        "flip_horizental" to PhotoFilter.FLIP_HORIZONTAL,
        "flip_vertical" to PhotoFilter.FLIP_VERTICAL,
        "rotate" to PhotoFilter.ROTATE,
        )

    private var filterUkr = mapOf(
        "original" to "Оригінал",
        "auto_fix" to "Авто-фікс",
        "brightness" to "Яскравість",
        "contrast" to "Контраст",
        "documentary" to "Документалка",
        "dual_tone" to "Подвійний тон",
        "fill_light" to "Висвітлення",
        "fish_eye" to "Риб'яче око",
        "grain" to "Висів",
        "gray_scale" to "Градації сірого",
        "lomish" to "Ломиш",
        "negative" to "Негатив",
        "posterize" to "Пастеризація",
        "saturate" to "Насичувати",
        "sepia" to "Сепія",
        "sharpen" to "Різкість",
        "temprature" to "Температура",
        "tint" to "Відтінок",
        "vignette" to "Віньєтка",
        "cross_process" to "Перехрестя",
        "flip_horizental" to "Перевернути\nпо\nгоризонталі",
        "flip_vertical" to "Перевернути\n по\nвертикалі",
        "rotate" to "Обертання",
    )

    private lateinit var photoEditorController: PhotoEditor

    private lateinit var editorContext: Context


    @SuppressLint("DiscouragedApi")
    @Composable
    fun PhotoEditorView(
        receivedUri: Uri,
        mTextRobotoTf: Typeface?,
        mEmojiTypeFace: Typeface,
        modifier: Modifier = Modifier,
        activityContext: Context,
        contentResolver: ContentResolver,
        activity: AppCompatActivity
    )
    {
        AndroidView(
            factory = { context ->
                val photoEditorLayout = LayoutInflater.from(context).inflate(R.layout.editor, null, false)
                val photoEditorView = photoEditorLayout.findViewById<PhotoEditorView>(R.id.photoEditorView)

                editorContext = context

                photoEditorView.source.setImageURI(receivedUri)

                photoEditorController = PhotoEditor.Builder(context, photoEditorView)
                    .setPinchTextScalable(true)
                    .setClipSourceImage(true)
                    .setDefaultTextTypeface(mTextRobotoTf)
                    .setDefaultEmojiTypeface(mEmojiTypeFace)
                    .build()


                photoEditorController.setOnPhotoEditorListener(
                    object : OnPhotoEditorListener
                    {
                        override fun onEditTextChangeListener(rootView: View?, text: String?, colorCode: Int)
                        {
                            isTextEditing.value = true
                            isEditorMenuShown.value = false
                            if (text != null)
                            {
                                textEdited.text.value = text
                            }

                            if (rootView != null)
                            {
                                textEdited.rootView.value = rootView
                            }
                            textEdited.color.value = colorCode
                        }

                        override fun onAddViewListener(viewType: ViewType?, numberOfAddedViews: Int)
                        {
                        }

                        override fun onRemoveViewListener(viewType: ViewType?, numberOfAddedViews: Int)
                        {
                        }

                        override fun onStartViewChangeListener(viewType: ViewType?)
                        {
                        }

                        override fun onStopViewChangeListener(viewType: ViewType?)
                        {
                        }

                        override fun onTouchSourceImage(event: MotionEvent?)
                        {
                        }
                    }
                )
                // do whatever you want...
                photoEditorLayout // return the view
            },
            update = { view ->
                val composeView: ComposeView = view.findViewById(R.id.compose_view)
                composeView.setContent {
                    val (_, width) = LocalConfiguration.current.run { screenHeightDp.dp to screenWidthDp.dp }

                    var brushColor by remember { mutableStateOf(Color.Black) }
                    var opacitySliderPosition by remember { mutableFloatStateOf(100f) }
                    var brushSliderPosition by remember { mutableFloatStateOf(0f) }

                    Box(
                            modifier = Modifier.fillMaxSize()
                            ){
                        if (isEditorMenuShown.value) {
                            Row (
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(horizontal = 10.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ){
                                Button(
                                    modifier = Modifier
                                        ,
                                    onClick =
                                    {
                                        activity.finish()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Black,
                                        contentColor = Color.White,
                                        disabledContainerColor = Color.Black,
                                        disabledContentColor = Color.White
                                    )
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    )
                                    {
                                        Icon(
                                            tint = Color.White,
                                            imageVector = Icons.Default.Cancel,
                                            contentDescription = "Undo"
                                        )
                                        Text(
                                            modifier = Modifier.padding(horizontal = 3.dp),
                                            text = "Скасувати")
                                    }
                                }

                                Button(
                                    modifier = Modifier,
                                    onClick =
                                    {
                                        var bitmap: Bitmap
                                        GlobalScope.launch {
                                            suspend {
                                                bitmap = photoEditorController.saveAsBitmap(saveSettings = SaveSettings.Builder()
                                                    .setClearViewsEnabled(true)
                                                    .setTransparencyEnabled(true)
                                                    .build())

                                                savePhotoToExternalStorage(UUID.randomUUID().toString(), bitmap, contentResolver = contentResolver)
                                            }.invoke()


                                        }

                                        activity.finish()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Black,
                                        contentColor = Color.White,
                                        disabledContainerColor = Color.Black,
                                        disabledContentColor = Color.White
                                    )
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    )
                                    {
                                        Text(
                                            modifier = Modifier.padding(horizontal = 3.dp),
                                            text = "Зберегти")
                                        Icon(
                                            tint = Color.White,
                                            imageVector = Icons.Default.Save,
                                            contentDescription = "save"
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(Color.Black)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .background(Color.Black)
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box{

                                    }
                                    Box (
                                        modifier = Modifier
                                            .width(width / 5),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row{
                                            IconButton(
                                                modifier = Modifier,
                                                onClick = {
                                                    photoEditorController.undo()
                                                }
                                            ) {
                                                Icon(
                                                    tint = Color.White,
                                                    imageVector = Icons.Default.Undo,
                                                    contentDescription = "Undo"
                                                )
                                            }

                                            IconButton(
                                                modifier = Modifier,
                                                onClick = {
                                                    photoEditorController.redo()
                                                }
                                            ) {
                                                Icon(
                                                    tint = Color.White,
                                                    imageVector = Icons.Default.Redo,
                                                    contentDescription = "Redo"
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.CenterHorizontally)
                                        .background(Color.White)
                                        .horizontalScroll(scrollState)
                                ) {


                                    val neededWidth = width / 4
                                    Button(
                                        modifier = Modifier
                                            .height(neededWidth)
                                            .width(neededWidth),
                                        onClick =
                                        {
                                            isEditorMenuShown.value = false
                                            photoEditorController.setBrushDrawingMode(true)
                                            isBrushCustomized.value = true
                                        },
                                        shape = RectangleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Black,
                                            contentColor = Color.White,
                                            disabledContainerColor = Color.Black,
                                            disabledContentColor = Color.White
                                        )
                                    )
                                    {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Brush,
                                                contentDescription = "Brush",
                                                modifier = Modifier.scale(1.5f)
                                            )
                                            Text(
                                                textAlign = TextAlign.Center,
                                                text = "Кисть",
                                                fontSize = 10.sp,
                                                modifier = Modifier
                                                    .width(175.dp)
                                                    .offset(y = 5.dp)
                                            )
                                        }
                                    }

                                    Button(
                                        modifier = Modifier
                                            .height(neededWidth)
                                            .width(neededWidth),
                                        onClick =
                                        {
                                            photoEditorController.brushEraser()
                                        },
                                        shape = RectangleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Black,
                                            contentColor = Color.White,
                                            disabledContainerColor = Color.Black,
                                            disabledContentColor = Color.White
                                        )
                                    )
                                    {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Brush,
                                                contentDescription = "Eraser",
                                                modifier = Modifier.scale(1.5f)
                                            )
                                            Text(
                                                textAlign = TextAlign.Center,
                                                text = "Гумка",
                                                fontSize = 10.sp,
                                                modifier = Modifier
                                                    .width(150.dp)
                                                    .offset(y = 5.dp)
                                            )
                                        }
                                    }

                                    Button(
                                        modifier = Modifier
                                            .height(neededWidth)
                                            .width(neededWidth),
                                        onClick =
                                        {
                                            isTextEditing.value = true
                                            isNewText.value = true
                                            isEditorMenuShown.value = false
                                        },
                                        shape = RectangleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Black,
                                            contentColor = Color.White,
                                            disabledContainerColor = Color.Black,
                                            disabledContentColor = Color.White
                                        )
                                    )
                                    {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.TextFields,
                                                contentDescription = "Text",
                                                modifier = Modifier.scale(1.5f)
                                            )
                                            Text(
                                                textAlign = TextAlign.Center,
                                                text = "Текст",
                                                fontSize = 10.sp,
                                                modifier = Modifier
                                                    .width(150.dp)
                                                    .offset(y = 5.dp)
                                            )
                                        }
                                    }
                                    Button(
                                        modifier = Modifier
                                            .height(neededWidth)
                                            .width(neededWidth),
                                        onClick =
                                        {
                                            isEditorMenuShown.value = false
                                            isFilterAplying.value = true
                                        },
                                        shape = RectangleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Black,
                                            contentColor = Color.White,
                                            disabledContainerColor = Color.Black,
                                            disabledContentColor = Color.White
                                        )
                                    )
                                    {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Image,
                                                contentDescription = "Filters",
                                                modifier = Modifier.scale(1.5f)
                                            )
                                            Text(
                                                textAlign = TextAlign.Center,
                                                text = "Фільтр",
                                                fontSize = 10.sp,
                                                modifier = Modifier
                                                    .width(150.dp)
                                                    .offset(y = 5.dp)
                                            )
                                        }
                                    }
                                    Button(
                                        modifier = Modifier
                                            .height(neededWidth)
                                            .width(neededWidth),
                                        onClick =
                                        {
                                            isEditorMenuShown.value = false
                                            isEmojiAdding.value = true
                                        },
                                        shape = RectangleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Black,
                                            contentColor = Color.White,
                                            disabledContainerColor = Color.Black,
                                            disabledContentColor = Color.White
                                        )
                                    )
                                    {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.EmojiEmotions,
                                                contentDescription = "Emoji",
                                                modifier = Modifier.scale(1.5f)
                                            )
                                            Text(
                                                textAlign = TextAlign.Center,
                                                text = "Емодзі",
                                                fontSize = 10.sp,
                                                modifier = Modifier
                                                    .width(150.dp)
                                                    .offset(y = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (isBrushCustomized.value)
                        {
                            Box(modifier = Modifier
                                .align(Alignment.BottomCenter)
                            )
                            {


                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                ) {
                                    Text(
                                        text = "Розмір кисті",
                                        color = Color.White
                                    )
                                    Slider(value = brushSliderPosition,
                                        onValueChange =
                                        {
                                            brushSliderPosition = it
                                        },
                                        valueRange = 0f..100f
                                    )
                                    Text(
                                        text = "Прозорість",
                                        color = Color.White
                                    )
                                    Slider(value = opacitySliderPosition,
                                        onValueChange =
                                        {
                                            opacitySliderPosition = it
                                        },
                                        valueRange = 0f..100f
                                    )

                                    Row(
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ){
                                        Button(
                                            modifier = Modifier
                                                .height(width / 10)
                                                .width(width / 10),
                                            onClick =
                                            {
                                                brushColor = Color.White
                                            },
                                            shape = RectangleShape,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.White,
                                                contentColor = Color.White,
                                                disabledContainerColor = Color.Black,
                                                disabledContentColor = Color.White
                                            )
                                        ) {
                                        }
                                        Button(modifier = Modifier
                                            .height(width / 10)
                                            .width(width / 10),
                                            onClick = {
                                                brushColor = Color.Black
                                            },
                                            shape = RectangleShape,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.Black,
                                                contentColor = Color.White,
                                                disabledContainerColor = Color.Black,
                                                disabledContentColor = Color.White
                                            )) {
                                        }
                                        Button(modifier = Modifier
                                            .height(width / 10)
                                            .width(width / 10),
                                            onClick = {
                                                brushColor = Color.Blue
                                            },
                                            shape = RectangleShape,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.Blue,
                                                contentColor = Color.White,
                                                disabledContainerColor = Color.Black,
                                                disabledContentColor = Color.White
                                            )) {
                                        }
                                        Button(modifier = Modifier
                                            .height(width / 10)
                                            .width(width / 10),
                                            onClick = {
                                                brushColor = Color(175, 89, 62)
                                            },
                                            shape = RectangleShape,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(175, 89, 62),
                                                contentColor = Color.White,
                                                disabledContainerColor = Color.Black,
                                                disabledContentColor = Color.White
                                            )) {
                                        }
                                        Button(modifier = Modifier
                                            .height(width / 10)
                                            .width(width / 10),
                                            onClick = {
                                               brushColor = Color.Green
                                            },
                                            shape = RectangleShape,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.Green,
                                                contentColor = Color.White,
                                                disabledContainerColor = Color.Black,
                                                disabledContentColor = Color.White
                                            )) {
                                        }
                                        Button(modifier = Modifier
                                            .height(width / 10)
                                            .width(width / 10),
                                            onClick = {
                                               brushColor = Color(255, 134, 31)
                                            },
                                            shape = RectangleShape,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(255, 134, 31),
                                                contentColor = Color.White,
                                                disabledContainerColor = Color.Black,
                                                disabledContentColor = Color.White
                                            )) {
                                        }
                                        Button(modifier = Modifier
                                            .height(width / 10)
                                            .width(width / 10),
                                            onClick = {
                                               brushColor = Color.Red
                                            },
                                            shape = RectangleShape,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.Red,
                                                contentColor = Color.White,
                                                disabledContainerColor = Color.Black,
                                                disabledContentColor = Color.White
                                            )) {
                                        }
                                        Button(modifier = Modifier
                                            .height(width / 10)
                                            .width(width / 10),
                                            onClick = {
                                               brushColor = Color(131, 89, 163)
                                            },
                                            shape = RectangleShape,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(131, 89, 163),
                                                contentColor = Color.White,
                                                disabledContainerColor = Color.Black,
                                                disabledContentColor = Color.White
                                            )) {
                                        }
                                        Button(modifier = Modifier
                                            .height(width / 10)
                                            .width(width / 10),
                                            onClick = {
                                               brushColor =  Color(118, 215, 234)
                                            },
                                            shape = RectangleShape,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(118, 215, 234),
                                                contentColor = Color.White,
                                                disabledContainerColor = Color.Black,
                                                disabledContentColor = Color.White
                                            )) {
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        IconButton(
                                            modifier = Modifier.scale(1.5f),
                                            onClick = {
                                                isEditorMenuShown.value = true
                                                isBrushCustomized.value = false
                                            },
                                        )
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Cancel,
                                                contentDescription = "",
                                                tint = Color.White
                                            )
                                        }

                                        IconButton(
                                            modifier = Modifier.scale(1.5f),
                                            onClick = {
                                                photoEditorController.brushSize = brushSliderPosition
                                                photoEditorController.setOpacity(opacitySliderPosition.toInt())
                                                photoEditorController.brushColor = brushColor.toArgb()

                                                isEditorMenuShown.value = true
                                                isBrushCustomized.value = false
                                            },
                                            )
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Done,
                                                contentDescription = "",
                                                tint = Color.White
                                            )
                                        }


                                    }

                                }
                            }
                        }

                        if (isTextEditing.value)
                        {
                            Box (
                                modifier = Modifier.fillMaxSize()
                            ){
                                Button(onClick = {
                                    isEditorMenuShown.value = true
                                    isTextEditing.value = false
                                    if (isNewText.value)
                                    {
                                        photoEditorController.addText(textEdited.text.value, textEdited.color.value)
                                        isNewText.value = false
                                        textEdited = TextEdited()
                                        isEditorMenuShown.value = true
                                    }
                                    else
                                    {
                                        textEdited.rootView.value?.let {
                                            photoEditorController.editText(
                                                it, textEdited.text.value, textEdited.color.value)
                                        }
                                        isEditorMenuShown.value = true
                                    }

                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(y = (width / 9) + 20.dp),
                                    shape = RectangleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Black,
                                        contentColor = Color.White,
                                        disabledContainerColor = Color.Black,
                                        disabledContentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = "Done"
                                    )
                                }
                                OutlinedTextField(
                                    value = textEdited.text.value,
                                    onValueChange =
                                    {
                                        textEdited.text.value = it
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(y = (width / 9) + 20.dp)
                                        .width(width / 2)
                                        .background(Color.White),
                                    shape = RectangleShape
                                )
                                Row (
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .fillMaxWidth()
                                        .height(width / 9)
                                ){
                                    Button(
                                        modifier = Modifier
                                            .height(width / 9)
                                            .width(width / 9),
                                        onClick =
                                        {
                                            textEdited.color.value = ContextCompat.getColor(activityContext, R.color.white)
                                        },
                                        shape = RectangleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = Color.White,
                                            disabledContainerColor = Color.Black,
                                            disabledContentColor = Color.White
                                        )
                                    ) {
                                    }
                                    Button(modifier = Modifier
                                        .height(width / 9)
                                        .width(width / 9),
                                        onClick = {
                                            textEdited.color.value = ContextCompat.getColor(activityContext, R.color.black)
                                        },
                                        shape = RectangleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Black,
                                            contentColor = Color.White,
                                            disabledContainerColor = Color.Black,
                                            disabledContentColor = Color.White
                                        )) {
                                    }
                                    Button(modifier = Modifier
                                        .height(width / 9)
                                        .width(width / 9),
                                        onClick = {
                                            textEdited.color.value = ContextCompat.getColor(activityContext, R.color.blue_color_picker)
                                        },
                                        shape = RectangleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Blue,
                                            contentColor = Color.White,
                                            disabledContainerColor = Color.Black,
                                            disabledContentColor = Color.White
                                        )) {
                                    }
                                    Button(modifier = Modifier
                                        .height(width / 9)
                                        .width(width / 9),
                                        onClick = {
                                            textEdited.color.value = ContextCompat.getColor(activityContext, R.color.brown_color_picker)
                                        },
                                        shape = RectangleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(175, 89, 62),
                                            contentColor = Color.White,
                                            disabledContainerColor = Color.Black,
                                            disabledContentColor = Color.White
                                        )) {
                                    }
                                    Button(modifier = Modifier
                                        .height(width / 9)
                                        .width(width / 9),
                                        onClick = {
                                            textEdited.color.value = ContextCompat.getColor(activityContext, R.color.green_color_picker)
                                        },
                                        shape = RectangleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Green,
                                            contentColor = Color.White,
                                            disabledContainerColor = Color.Black,
                                            disabledContentColor = Color.White
                                        )) {
                                    }
                                    Button(modifier = Modifier
                                        .height(width / 9)
                                        .width(width / 9),
                                        onClick = {
                                            textEdited.color.value = ContextCompat.getColor(activityContext, R.color.orange_color_picker)
                                        },
                                        shape = RectangleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(255, 134, 31),
                                            contentColor = Color.White,
                                            disabledContainerColor = Color.Black,
                                            disabledContentColor = Color.White
                                        )) {
                                    }
                                    Button(modifier = Modifier
                                        .height(width / 9)
                                        .width(width / 9),
                                        onClick = {
                                            textEdited.color.value = ContextCompat.getColor(activityContext, R.color.red_color_picker)
                                        },
                                        shape = RectangleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Red,
                                            contentColor = Color.White,
                                            disabledContainerColor = Color.Black,
                                            disabledContentColor = Color.White
                                        )) {
                                    }
                                    Button(modifier = Modifier
                                        .height(width / 9)
                                        .width(width / 9),
                                        onClick = {
                                            textEdited.color.value = ContextCompat.getColor(activityContext, R.color.violet_color_picker)
                                        },
                                        shape = RectangleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(131, 89, 163),
                                            contentColor = Color.White,
                                            disabledContainerColor = Color.Black,
                                            disabledContentColor = Color.White
                                        )) {
                                    }
                                    Button(modifier = Modifier
                                        .height(width / 9)
                                        .width(width / 9),
                                        onClick = {
                                            textEdited.color.value = ContextCompat.getColor(activityContext, R.color.sky_blue_color_picker)
                                        },
                                        shape = RectangleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(118, 215, 234),
                                            contentColor = Color.White,
                                            disabledContainerColor = Color.Black,
                                            disabledContentColor = Color.White
                                        )) {
                                    }
                                }
                            }

                        }

                        fun convertEmoji(emoji: String): String {
                            return try {
                                val convertEmojiToInt = emoji.substring(2).toInt(16)
                                String(Character.toChars(convertEmojiToInt))
                            } catch (e: NumberFormatException) {
                                ""
                            }
                        }

                        val emojis = activityContext.resources.getStringArray(R.array.photo_editor_emoji)

                        if (isEmojiAdding.value)
                        {
                            LazyVerticalGrid(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                columns =  GridCells.Adaptive(minSize = width / 4),
                                content = {
                                    items(emojis) {emoji ->
                                        IconButton(
                                            modifier = Modifier
                                                .fillMaxSize(),
                                            onClick = {
                                                photoEditorController.addEmoji(convertEmoji(emoji))
                                                isEmojiAdding.value = false
                                                isEditorMenuShown.value = true
                                            }
                                        ) {
                                            Text(
                                                text = convertEmoji(emoji),
                                                fontSize = 30.sp,
                                            )
                                        }
                                    }
                                }
                            )
                        }

                        if (isFilterAplying.value)
                        {
                            LazyRow(
                                modifier = Modifier
                                    .height(width / 3)
                                    .align(Alignment.BottomCenter),
                                content = {
                                    items(filterMap.keys.toTypedArray()) {filter ->
                                        Button(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .width(width / 3)
                                                .border(2.dp, Color.Gray)
                                                .padding(0.dp),
                                            onClick = {

                                                isFilterAplying.value = false
                                                isEditorMenuShown.value = true

                                                filterMap[filter]
                                                    ?.let { photoEditorController.setFilterEffect(it) }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.Black,
                                                contentColor = Color.White,
                                                disabledContainerColor = Color.Black,
                                                disabledContentColor = Color.White
                                            ),
                                            shape = RectangleShape
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize())
                                            {
                                                Image(
                                                    painter = painterResource(
                                                        activityContext.resources.getIdentifier(filter , "drawable", activityContext.packageName)
                                                    ),
                                                    contentDescription = "ABOBA",
                                                    contentScale = ContentScale.FillHeight,
                                                    modifier = Modifier.scale(3.1f),
                                                    alpha = 0.5f
                                                )

                                                filterUkr[filter]?.let {
                                                    Text(
                                                        text = it,
                                                        modifier = Modifier
                                                            .align(Alignment.Center)
                                                            .scale(3f),
                                                        textAlign = TextAlign.Center,
                                                        fontSize = 4.sp,
                                                        lineHeight = 5.sp,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                    // Update the view
                }
            },
            modifier = modifier
        )
    }
}


class EditorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mTextRobotoTf: Typeface? = ResourcesCompat.getFont(this, R.font.roboto)
        val mEmojiTypeFace: Typeface = Typeface.createFromAsset(assets, "emojione-android.ttf")
        val receivedUri = Uri.parse(intent.getStringExtra("imageUri"))

        val photoEditor = PhotoEditor()
        setContent {
            photoEditor.PhotoEditorView(
                receivedUri = receivedUri,
                mTextRobotoTf,
                mEmojiTypeFace,
                modifier = Modifier.fillMaxSize(),
                this,
                contentResolver,
                this
            )
        }
    }
}


