package social.vyb.app.features.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.core.graphics.createBitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Composable
internal fun StoryBuilderScreen(
    media: SelectedMedia,
    intent: MediaPublishIntent,
    onApply: (SelectedMedia) -> Unit,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var snapshot by remember(media.uri) { mutableStateOf(StoryEditorSnapshot()) }
    val undo = remember(media.uri) { mutableStateListOf<StoryEditorSnapshot>() }
    val redo = remember(media.uri) { mutableStateListOf<StoryEditorSnapshot>() }
    var tool by remember { mutableStateOf(StoryTool.Transform) }
    var preview by remember { mutableStateOf(false) }
    var confirmClose by remember { mutableStateOf(false) }
    var textEditor by remember { mutableStateOf<StoryTextOverlay?>(null) }
    var stickerPicker by remember { mutableStateOf(false) }
    var cropPicker by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun commit(next: StoryEditorSnapshot) {
        if (next == snapshot) return
        undo += snapshot
        if (undo.size > 50) undo.removeAt(0)
        redo.clear()
        snapshot = next
    }
    fun requestClose() {
        if (undo.isNotEmpty() || snapshot != StoryEditorSnapshot()) confirmClose = true
        else onDismiss()
    }

    Dialog(
        onDismissRequest = ::requestClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Box(Modifier.fillMaxSize()) {
                StoryCanvas(
                    media = media,
                    intent = intent,
                    snapshot = snapshot,
                    drawingEnabled = tool == StoryTool.Draw && !preview,
                    onTransform = { zoom, panX, panY, rotation ->
                        // Gesture callbacks can fire every frame. Updating the live
                        // snapshot directly avoids creating dozens of undo entries
                        // and restarting the active pointer-input coroutine.
                        snapshot = snapshot.copy(
                            scale = (snapshot.scale * zoom).coerceIn(.5f, 5f),
                            offsetX = (snapshot.offsetX + panX).coerceIn(-1f, 1f),
                            offsetY = (snapshot.offsetY + panY).coerceIn(-1f, 1f),
                            rotation = snapshot.rotation + rotation
                        )
                    },
                    onTextTransformed = { id, x, y, size ->
                        snapshot = snapshot.copy(
                            texts = snapshot.texts.map {
                                if (it.id == id) it.copy(x = x, y = y, size = size) else it
                            }
                        )
                    },
                    onTextEdit = { overlay -> textEditor = overlay },
                    onStickerMoved = { id, x, y ->
                        snapshot = snapshot.copy(
                            stickers = snapshot.stickers.map {
                                if (it.id == id) it.copy(x = x, y = y) else it
                            }
                        )
                    },
                    onStroke = { points ->
                        commit(
                            snapshot.copy(
                                strokes = snapshot.strokes + StoryStroke(
                                    color = snapshot.drawColor,
                                    width = snapshot.drawWidth,
                                    points = points
                                )
                            )
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (!preview) {
                    Row(
                        modifier = Modifier
                            .statusBarsPadding()
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EditorCircleButton(
                            Icons.Default.Close,
                            "Close",
                            onClick = ::requestClose
                        )
                        Row {
                            EditorCircleButton(
                                Icons.AutoMirrored.Filled.Undo,
                                "Undo",
                                enabled = undo.isNotEmpty()
                            ) {
                                redo += snapshot
                                snapshot = undo.removeAt(undo.lastIndex)
                            }
                            EditorCircleButton(
                                Icons.AutoMirrored.Filled.Redo,
                                "Redo",
                                enabled = redo.isNotEmpty()
                            ) {
                                undo += snapshot
                                snapshot = redo.removeAt(redo.lastIndex)
                            }
                            EditorCircleButton(Icons.Default.Visibility, "Preview") {
                                preview = true
                            }
                            EditorCircleButton(
                                Icons.Default.Check,
                                "Apply",
                                enabled = !exporting
                            ) {
                                exporting = true
                                scope.launch {
                                    runCatching {
                                        exportStoryComposition(context, media, intent, snapshot)
                                    }.onSuccess(onApply).onFailure {
                                        error = it.message ?: "Could not export story."
                                        exporting = false
                                    }
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = .72f))
                            .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 28.dp)
                    ) {
                        if (tool == StoryTool.Draw) {
                            DrawControls(snapshot) { color, width ->
                                snapshot = snapshot.copy(drawColor = color, drawWidth = width)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            EditorToolButton("Move", Icons.Default.FitScreen, tool == StoryTool.Transform) {
                                tool = StoryTool.Transform
                            }
                            EditorToolButton("Text", Icons.Default.TextFields, false) {
                                textEditor = StoryTextOverlay(
                                    id = UUID.randomUUID().toString(),
                                    text = "Your text",
                                    x = .5f,
                                    y = .45f
                                )
                            }
                            EditorToolButton("Emojis", Icons.Default.EmojiEmotions, false) {
                                stickerPicker = true
                            }
                            EditorToolButton("Draw", Icons.Default.Draw, tool == StoryTool.Draw) {
                                tool = if (tool == StoryTool.Draw) StoryTool.Transform else StoryTool.Draw
                            }
                            EditorToolButton("Rotate", Icons.AutoMirrored.Filled.RotateRight, false) {
                                commit(snapshot.copy(rotation = snapshot.rotation + 90f))
                            }
                            EditorToolButton("Crop", Icons.Default.Crop, snapshot.fill) {
                                cropPicker = true
                            }
                        }
                        Spacer(Modifier.height(28.dp))
                    }
                } else {
                    TextButton(
                        onClick = { preview = false },
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(10.dp)
                            .align(Alignment.TopStart)
                            .background(Color.Black.copy(alpha = .55f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, "Exit preview", tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Back to edit", color = Color.White)
                    }
                }
                if (exporting) {
                    Text(
                        "Preparing media…",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = .72f), RoundedCornerShape(16.dp))
                            .padding(18.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    textEditor?.let { editing ->
        StoryTextDialog(
            initial = editing,
            onDismiss = { textEditor = null },
            onSave = { overlay ->
                commit(
                    snapshot.copy(
                        texts = snapshot.texts.filterNot { it.id == overlay.id } + overlay
                    )
                )
                textEditor = null
            }
        )
    }
    if (stickerPicker) {
        StickerPickerDialog(
            onDismiss = { stickerPicker = false },
            onSticker = { emoji ->
                commit(
                    snapshot.copy(
                        stickers = snapshot.stickers + StorySticker(
                            id = UUID.randomUUID().toString(),
                            emoji = emoji,
                            x = .5f,
                            y = .5f
                        )
                    )
                )
                stickerPicker = false
            }
        )
    }
    if (cropPicker) {
        val options = if (intent == MediaPublishIntent.Post) {
            listOf(
                "Original" to ("original" to false),
                "Fill original" to ("original" to true),
                "Square 1:1" to ("square" to true),
                "Portrait 4:5" to ("portrait" to true),
                "Landscape 16:9" to ("landscape" to true)
            )
        } else {
            listOf("Fit" to ("story" to false), "Fill 9:16" to ("story" to true))
        }
        AlertDialog(
            onDismissRequest = { cropPicker = false },
            title = { Text("Crop and framing") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose an output frame. You can then pinch and drag the media inside it.")
                    options.forEach { (label, option) ->
                        FilterChip(
                            selected = snapshot.cropAspect == option.first && snapshot.fill == option.second,
                            onClick = {
                                commit(snapshot.copy(cropAspect = option.first, fill = option.second))
                                cropPicker = false
                            },
                            label = { Text(label) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { cropPicker = false }) { Text("Close") } }
        )
    }
    if (confirmClose) {
        AlertDialog(
            onDismissRequest = { confirmClose = false },
            title = { Text("Discard story edits?") },
            text = { Text("Your text, stickers, drawing and framing changes will be lost.") },
            dismissButton = {
                TextButton(onClick = { confirmClose = false }) { Text("Keep editing") }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Discard") }
            }
        )
    }
    error?.let { message ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text("Media export failed") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { error = null }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun StoryCanvas(
    media: SelectedMedia,
    intent: MediaPublishIntent,
    snapshot: StoryEditorSnapshot,
    drawingEnabled: Boolean,
    onTransform: (Float, Float, Float, Float) -> Unit,
    onTextTransformed: (String, Float, Float, Float) -> Unit,
    onTextEdit: (StoryTextOverlay) -> Unit,
    onStickerMoved: (String, Float, Float) -> Unit,
    onStroke: (List<StoryPoint>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentOnTransform by rememberUpdatedState(onTransform)
    val currentOnTextTransformed by rememberUpdatedState(onTextTransformed)
    val currentOnTextEdit by rememberUpdatedState(onTextEdit)
    val currentOnStickerMoved by rememberUpdatedState(onStickerMoved)
    var bitmap by remember(media.uri) { mutableStateOf<Bitmap?>(null) }
    var activeStrokePoints by remember(media.uri) { mutableStateOf<List<StoryPoint>>(emptyList()) }
    LaunchedEffect(media.uri, media.mediaType) {
        bitmap = if (media.mediaType == "image") {
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(media.uri)?.use(BitmapFactory::decodeStream)
            }
        } else {
            null
        }
    }
    BoxWithConstraints(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val sourceAspect = bitmap
            ?.takeIf { it.width > 0 && it.height > 0 }
            ?.let { it.width.toFloat() / it.height.toFloat() }
            ?: (4f / 3f)
        val canvasAspect = when {
            intent != MediaPublishIntent.Post -> 9f / 16f
            snapshot.cropAspect == "square" -> 1f
            snapshot.cropAspect == "portrait" -> 4f / 5f
            snapshot.cropAspect == "landscape" -> 16f / 9f
            else -> sourceAspect
        }
        val idealWidth = maxHeight * canvasAspect
        val canvasWidth = if (maxWidth < idealWidth) maxWidth else idealWidth
        val canvasHeight = canvasWidth / canvasAspect
        val widthPx = with(density) { canvasWidth.toPx() }
        val heightPx = with(density) { canvasHeight.toPx() }
        Box(
            modifier = Modifier
                .width(canvasWidth)
                .height(canvasHeight)
                .background(Color(0xFF111111))
                .pointerInput(drawingEnabled) {
                    if (!drawingEnabled) {
                        detectTransformGestures { _, pan, zoom, rotation ->
                            currentOnTransform(
                                zoom,
                                pan.x / size.width,
                                pan.y / size.height,
                                rotation
                            )
                        }
                    }
                }
        ) {
            if (media.mediaType == "video") {
                key(snapshot.fill) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(media.uri)
                                setOnPreparedListener {
                                    it.isLooping = true
                                    it.setVideoScalingMode(
                                        if (snapshot.fill) {
                                            MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                                        } else {
                                            MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT
                                        }
                                    )
                                    start()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = snapshot.scale
                                scaleY = snapshot.scale
                                translationX = snapshot.offsetX * widthPx
                                translationY = snapshot.offsetY * heightPx
                                rotationZ = snapshot.rotation
                            }
                    )
                }
            } else {
                bitmap?.let { source ->
                    androidx.compose.foundation.Image(
                        bitmap = source.asImageBitmap(),
                        contentDescription = "Story media",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = snapshot.scale
                                scaleY = snapshot.scale
                                translationX = snapshot.offsetX * widthPx
                                translationY = snapshot.offsetY * heightPx
                                rotationZ = snapshot.rotation
                            },
                        contentScale = if (snapshot.fill) ContentScale.Crop else ContentScale.Fit
                    )
                }
            }
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(drawingEnabled, snapshot.drawColor, snapshot.drawWidth) {
                        if (drawingEnabled) {
                            var points = mutableListOf<StoryPoint>()
                            detectDragGestures(
                                onDragStart = { start ->
                                    points = mutableListOf(
                                        StoryPoint(start.x / size.width, start.y / size.height)
                                    )
                                    activeStrokePoints = points.toList()
                                },
                                onDragEnd = {
                                    if (points.size > 1) onStroke(points)
                                    points = mutableListOf()
                                    activeStrokePoints = emptyList()
                                },
                                onDragCancel = {
                                    points = mutableListOf()
                                    activeStrokePoints = emptyList()
                                }
                            ) { change, _ ->
                                change.consume()
                                points += StoryPoint(
                                    change.position.x / size.width,
                                    change.position.y / size.height
                                )
                                activeStrokePoints = points.toList()
                            }
                        }
                    }
            ) {
                snapshot.strokes.forEach { stroke ->
                    if (stroke.points.size > 1) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(
                                stroke.points.first().x * size.width,
                                stroke.points.first().y * size.height
                            )
                            stroke.points.drop(1).forEach {
                                lineTo(it.x * size.width, it.y * size.height)
                            }
                        }
                        drawPath(
                            path,
                            color = Color(stroke.color),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = stroke.width * size.width / 360f,
                                pathEffect = PathEffect.cornerPathEffect(8f)
                            )
                        )
                    }
                }
                if (activeStrokePoints.size > 1) {
                    val livePath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(activeStrokePoints.first().x * size.width, activeStrokePoints.first().y * size.height)
                        activeStrokePoints.drop(1).forEach { lineTo(it.x * size.width, it.y * size.height) }
                    }
                    drawPath(
                        livePath,
                        color = Color(snapshot.drawColor),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = snapshot.drawWidth * size.width / 360f,
                            pathEffect = PathEffect.cornerPathEffect(8f)
                        )
                    )
                }
            }
            snapshot.texts.forEach { overlay ->
                Text(
                    overlay.text,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (overlay.x * widthPx).roundToInt(),
                                (overlay.y * heightPx).roundToInt()
                            )
                        }
                        .graphicsLayer {
                            translationX = -size.width / 2f
                            translationY = -size.height / 2f
                        }
                        .background(
                            if (overlay.background) Color.Black.copy(alpha = .55f)
                            else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .pointerInput(overlay.id) {
                            detectTapGestures(
                                onDoubleTap = { currentOnTextEdit(overlay) }
                            )
                        }
                        // Keep this gesture coroutine stable while each pinch frame updates
                        // overlay.size; restarting it mid-gesture made live zoom appear stuck.
                        .pointerInput(overlay.id) {
                            var x = overlay.x
                            var y = overlay.y
                            var textSize = overlay.size
                            detectTransformGestures { _, pan, zoom, _ ->
                                x = (x + pan.x / widthPx).coerceIn(0f, 1f)
                                y = (y + pan.y / heightPx).coerceIn(0f, 1f)
                                textSize = (textSize * zoom).coerceIn(18f, 96f)
                                currentOnTextTransformed(overlay.id, x, y, textSize)
                            }
                        },
                    color = Color(overlay.color),
                    fontSize = overlay.size.sp,
                    fontWeight = if (overlay.bold) FontWeight.Bold else FontWeight.Normal,
                    textAlign = when (overlay.align) {
                        "left" -> TextAlign.Left
                        "right" -> TextAlign.Right
                        else -> TextAlign.Center
                    }
                )
            }
            snapshot.stickers.forEach { sticker ->
                Text(
                    sticker.emoji,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (sticker.x * widthPx).roundToInt(),
                                (sticker.y * heightPx).roundToInt()
                            )
                        }
                        .graphicsLayer {
                            translationX = -size.width / 2f
                            translationY = -size.height / 2f
                        }
                        .pointerInput(sticker.id) {
                            var x = sticker.x
                            var y = sticker.y
                            detectDragGestures { change, drag ->
                                change.consume()
                                x = (x + drag.x / widthPx).coerceIn(0f, 1f)
                                y = (y + drag.y / heightPx).coerceIn(0f, 1f)
                                currentOnStickerMoved(sticker.id, x, y)
                            }
                        },
                    fontSize = sticker.size.sp
                )
            }
        }
    }
}

@Composable
private fun EditorCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .padding(2.dp)
            .background(Color.Black.copy(alpha = .56f), CircleShape)
    ) {
        Icon(icon, description, tint = if (enabled) Color.White else Color.Gray)
    }
}

@Composable
private fun EditorToolButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.background(
                if (selected) Color(0xFF6366F1) else Color.White.copy(alpha = .1f),
                CircleShape
            )
        ) {
            Icon(icon, label, tint = Color.White)
        }
        Text(label, color = Color.White, fontSize = 10.sp)
    }
}

@Composable
private fun DrawControls(
    snapshot: StoryEditorSnapshot,
    onChange: (Int, Float) -> Unit
) {
    val colors = listOf(
        android.graphics.Color.WHITE,
        android.graphics.Color.BLACK,
        android.graphics.Color.RED,
        android.graphics.Color.YELLOW,
        android.graphics.Color.CYAN,
        0xFF7C3AED.toInt()
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        colors.forEach { color ->
            Surface(
                onClick = { onChange(color, snapshot.drawWidth) },
                modifier = Modifier.padding(4.dp).size(28.dp),
                shape = CircleShape,
                color = Color(color),
                border = androidx.compose.foundation.BorderStroke(
                    if (snapshot.drawColor == color) 3.dp else 1.dp,
                    Color.White
                )
            ) {}
        }
        Slider(
            value = snapshot.drawWidth,
            onValueChange = { onChange(snapshot.drawColor, it) },
            valueRange = 2f..24f,
            modifier = Modifier.weight(1f).padding(start = 8.dp)
        )
    }
}

@Composable
private fun StoryTextDialog(
    initial: StoryTextOverlay,
    onDismiss: () -> Unit,
    onSave: (StoryTextOverlay) -> Unit
) {
    var value by remember { mutableStateOf(initial.text) }
    var color by remember { mutableIntStateOf(initial.color) }
    var size by remember { mutableFloatStateOf(initial.size) }
    var bold by remember { mutableStateOf(initial.bold) }
    var background by remember { mutableStateOf(initial.background) }
    var align by remember { mutableStateOf(initial.align) }
    val colors = listOf(
        android.graphics.Color.WHITE,
        android.graphics.Color.BLACK,
        android.graphics.Color.YELLOW,
        android.graphics.Color.CYAN,
        android.graphics.Color.MAGENTA,
        android.graphics.Color.RED
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Text overlay") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.take(200) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Row {
                    colors.forEach { option ->
                        Surface(
                            onClick = { color = option },
                            modifier = Modifier.padding(3.dp).size(30.dp),
                            shape = CircleShape,
                            color = Color(option),
                            border = androidx.compose.foundation.BorderStroke(
                                if (color == option) 3.dp else 1.dp,
                                MaterialTheme.colorScheme.primary
                            )
                        ) {}
                    }
                }
                Text("Size ${size.roundToInt()}")
                Slider(value = size, onValueChange = { size = it }, valueRange = 18f..64f)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(bold, { bold = !bold }, { Text("Bold") })
                    FilterChip(background, { background = !background }, { Text("Backdrop") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("left", "center", "right").forEach {
                        FilterChip(align == it, { align = it }, { Text(it.replaceFirstChar(Char::titlecase)) })
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            text = value.ifBlank { "Text" },
                            color = color,
                            size = size,
                            bold = bold,
                            background = background,
                            align = align
                        )
                    )
                }
            ) { Text("Add") }
        }
    )
}

@Composable
private fun StickerPickerDialog(onDismiss: () -> Unit, onSticker: (String) -> Unit) {
    var showMore by remember { mutableStateOf(false) }
    val emojis = listOf(
        "😀", "😂", "😍", "🥳", "🔥", "❤️", "✨", "🎉",
        "👍", "🙌", "💯", "📍", "🎓", "☕", "🎵", "🏆"
    )
    val moreEmojis = listOf(
        "\uD83E\uDD29", "\uD83D\uDE0E", "\uD83E\uDD1D", "\uD83D\uDCAA",
        "\uD83C\uDF08", "\uD83C\uDF1F", "\uD83D\uDE80", "\uD83C\uDFAF",
        "\uD83C\uDFC0", "\u26BD", "\uD83C\uDFAE", "\uD83D\uDCBB",
        "\uD83D\uDCDA", "\uD83D\uDCF8", "\uD83D\uDCA1", "\u2705"
    )
    val visibleEmojis = if (showMore) emojis + moreEmojis else emojis
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose an emoji") },
        text = {
            Column {
                visibleEmojis.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { emoji ->
                            TextButton(onClick = { onSticker(emoji) }) {
                                Text(emoji, fontSize = 32.sp)
                            }
                        }
                    }
                }
                if (!showMore) {
                    TextButton(onClick = { showMore = true }, modifier = Modifier.align(Alignment.End)) {
                        Text("More emojis")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

private suspend fun exportStoryComposition(
    context: Context,
    media: SelectedMedia,
    intent: MediaPublishIntent,
    snapshot: StoryEditorSnapshot
): SelectedMedia = withContext(Dispatchers.IO) {
    val metadata = StoryCompositionCodec.encodeToString(snapshot.toComposition())
    if (media.mediaType == "video") {
        require(metadata.toByteArray().size <= 64 * 1024) {
            "Story composition is too complex. Remove a few drawing strokes."
        }
        return@withContext media.copy(compositionJson = metadata)
    }
    val source = context.contentResolver.openInputStream(media.uri)?.use(BitmapFactory::decodeStream)
        ?: error("The selected image could not be read.")
    val (width, height) = when {
        intent != MediaPublishIntent.Post -> 1080 to 1920
        snapshot.cropAspect == "square" -> 1080 to 1080
        snapshot.cropAspect == "portrait" -> 1080 to 1350
        snapshot.cropAspect == "landscape" -> 1600 to 900
        else -> {
            val scaleToLimit = min(1f, 2048f / max(source.width, source.height).toFloat())
            (source.width * scaleToLimit).roundToInt().coerceAtLeast(1) to
                (source.height * scaleToLimit).roundToInt().coerceAtLeast(1)
        }
    }
    val output = createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(output)
    canvas.drawColor(android.graphics.Color.BLACK)

    val fitScale = min(width.toFloat() / source.width, height.toFloat() / source.height)
    val fillScale = max(width.toFloat() / source.width, height.toFloat() / source.height)
    val scale = (if (snapshot.fill) fillScale else fitScale) * snapshot.scale
    val matrix = Matrix().apply {
        postTranslate(-source.width / 2f, -source.height / 2f)
        postScale(scale, scale)
        postRotate(snapshot.rotation)
        postTranslate(
            width / 2f + snapshot.offsetX * width,
            height / 2f + snapshot.offsetY * height
        )
    }
    canvas.drawBitmap(source, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))

    snapshot.strokes.forEach { stroke ->
        if (stroke.points.size > 1) {
            val path = Path().apply {
                moveTo(stroke.points.first().x * width, stroke.points.first().y * height)
                stroke.points.drop(1).forEach { lineTo(it.x * width, it.y * height) }
            }
            canvas.drawPath(
                path,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = stroke.color
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    strokeWidth = stroke.width * width / 360f
                }
            )
        }
    }
    snapshot.texts.forEach { overlay ->
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = overlay.color
            textSize = overlay.size * width / 360f
            textAlign = when (overlay.align) {
                "left" -> Paint.Align.LEFT
                "right" -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
            typeface = if (overlay.bold) android.graphics.Typeface.DEFAULT_BOLD
            else android.graphics.Typeface.DEFAULT
        }
        val x = overlay.x * width
        val y = overlay.y * height
        if (overlay.background) {
            val bounds = android.graphics.Rect()
            paint.getTextBounds(overlay.text, 0, overlay.text.length, bounds)
            canvas.drawRoundRect(
                x - bounds.width() / 2f - 24f,
                y - bounds.height() - 20f,
                x + bounds.width() / 2f + 24f,
                y + 20f,
                18f,
                18f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x99000000.toInt()
                }
            )
        }
        canvas.drawText(overlay.text, x, y, paint)
    }
    snapshot.stickers.forEach { sticker ->
        canvas.drawText(
            sticker.emoji,
            sticker.x * width,
            sticker.y * height,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = sticker.size * width / 360f
                textAlign = Paint.Align.CENTER
            }
        )
    }

    val directory = File(context.filesDir, "story_exports").apply { mkdirs() }
    val file = File(directory, "story-${UUID.randomUUID()}.jpg")
    FileOutputStream(file).use {
        check(output.compress(Bitmap.CompressFormat.JPEG, 92, it)) {
            "Could not encode the edited story."
        }
    }
    source.recycle()
    output.recycle()
    SelectedMedia(
        uri = Uri.fromFile(file),
        fileName = file.name,
        mimeType = "image/jpeg",
        sizeBytes = file.length(),
        mediaType = "image",
        compositionJson = null
    )
}

private enum class StoryTool { Transform, Draw }

@Serializable
private data class StoryEditorSnapshot(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotation: Float = 0f,
    val fill: Boolean = true,
    val cropAspect: String = "original",
    val texts: List<StoryTextOverlay> = emptyList(),
    val stickers: List<StorySticker> = emptyList(),
    val strokes: List<StoryStroke> = emptyList(),
    val drawColor: Int = android.graphics.Color.WHITE,
    val drawWidth: Float = 8f
) {
    fun toComposition() = StoryCompositionJson(
        version = 1,
        canvas = StoryCanvasJson(),
        media = StoryMediaJson(
            fit = if (fill) "cover" else "contain",
            scale = scale,
            rotationDegrees = rotation,
            offsetX = offsetX.coerceIn(-1f, 1f),
            offsetY = offsetY.coerceIn(-1f, 1f)
        ),
        layers = buildList {
            texts.forEach {
                add(
                    StoryLayerJson(
                        type = "text",
                        id = it.id,
                        x = it.x.coerceIn(0f, 1f),
                        y = it.y.coerceIn(0f, 1f),
                        text = it.text,
                        color = it.color.toWireColor(),
                        fontSize = (it.size / 360f).coerceIn(.03f, .3f),
                        align = it.align,
                        style = when {
                            it.background -> "highlight"
                            it.bold -> "bold"
                            else -> "plain"
                        }
                    )
                )
            }
            stickers.forEach {
                add(
                    StoryLayerJson(
                        type = "sticker",
                        id = it.id,
                        x = it.x.coerceIn(0f, 1f),
                        y = it.y.coerceIn(0f, 1f),
                        value = it.emoji,
                        size = (it.size / 360f).coerceIn(.04f, .4f)
                    )
                )
            }
            strokes.forEachIndexed { index, stroke ->
                add(
                    StoryLayerJson(
                        type = "drawing",
                        id = "drawing-$index",
                        color = stroke.color.toWireColor(),
                        width = (stroke.width / 360f).coerceIn(.002f, .08f),
                        points = stroke.points.map {
                            StoryPointJson(it.x.coerceIn(0f, 1f), it.y.coerceIn(0f, 1f))
                        }
                    )
                )
            }
        }
    )
}

@Serializable
private data class StoryTextOverlay(
    val id: String,
    val text: String,
    val x: Float,
    val y: Float,
    val color: Int = android.graphics.Color.WHITE,
    val size: Float = 36f,
    val bold: Boolean = true,
    val background: Boolean = false,
    val align: String = "center"
)

@Serializable
private data class StorySticker(
    val id: String,
    val emoji: String,
    val x: Float,
    val y: Float,
    val size: Float = 48f
)

@Serializable
private data class StoryStroke(
    val color: Int,
    val width: Float,
    val points: List<StoryPoint>
)

@Serializable
private data class StoryPoint(val x: Float, val y: Float)

private fun Int.toWireColor(): String =
    "#%06X".format(this and 0xFFFFFF)
