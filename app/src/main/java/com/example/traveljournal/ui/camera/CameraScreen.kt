package com.example.traveljournal.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SwitchCamera
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.traveljournal.util.MediaSaver
import com.example.traveljournal.viewmodel.CameraViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.Executor
import java.net.URLEncoder
import androidx.compose.ui.graphics.Color

// Camera UI color constants - independent of theme
private val CameraOverlay = Color(0xCC000000)  // Semi-transparent black
private val CameraBand = Color(0xFF121212)      // Dark gray letterbox
private val CameraBar = Color(0xE61A1A1A)       // Dark gray bar with transparency
private val CameraText = Color.White
private val CameraIcon = Color.White

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CameraScreen(
    navController: NavController, 
    cameraViewModel: CameraViewModel,
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
    scope: CoroutineScope = rememberCoroutineScope()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor: Executor = remember { ContextCompat.getMainExecutor(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionsNeeded = mutableListOf(Manifest.permission.CAMERA).apply {
        if (CameraViewModel.ENABLE_VIDEO_CAPTURE) add(Manifest.permission.RECORD_AUDIO)
    }.toTypedArray()

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            hasCameraPermission = perms[Manifest.permission.CAMERA] == true
            if (CameraViewModel.ENABLE_VIDEO_CAPTURE) {
                hasAudioPermission = perms[Manifest.permission.RECORD_AUDIO] == true
            }
        }

    var previewView: PreviewView? by remember { mutableStateOf(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var maxZoomRatio by remember { mutableStateOf(4f) }

    var pressed by remember { mutableStateOf(false) }
    var pressDownTime by remember { mutableStateOf(0L) }
    var isRecording by remember { mutableStateOf(false) }
    var recordStartMillis by remember { mutableStateOf(0L) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    
    // Zoom state (1x to 4x)
    var currentZoom by remember { mutableStateOf(1f) }
    var isScreenDisposed by remember { mutableStateOf(false) }

    val lensFacing by cameraViewModel.lensFacing.collectAsState()

    LaunchedEffect(isRecording) {
        if (!isRecording) {
            elapsedSeconds = 0
            return@LaunchedEffect
        }
        while (isRecording) {
            val now = System.currentTimeMillis()
            elapsedSeconds = ((now - recordStartMillis) / 1000L).toInt().coerceAtLeast(0)
            delay(250)
        }
    }

    // ✅ NON-BLOCKING provider: évite ANR au démarrage
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val recordingToStop by rememberUpdatedState(activeRecording)

    DisposableEffect(lifecycleOwner) {
        isScreenDisposed = false
        onDispose {
            isScreenDisposed = true
            recordingToStop?.stop()
            cameraViewModel.setRecording(false)
            cameraProviderFuture.addListener({
                runCatching { cameraProviderFuture.get().unbindAll() }
            }, mainExecutor)
        }
    }

    LaunchedEffect(hasCameraPermission, previewView, lensFacing) {
        if (!hasCameraPermission) return@LaunchedEffect
        val pv = previewView ?: return@LaunchedEffect

        cameraProviderFuture.addListener({
            if (isScreenDisposed) return@addListener
            try {
                val cameraProvider = cameraProviderFuture.get()
                if (isScreenDisposed) return@addListener
                cameraProvider.unbindAll()

                val preview = Preview.Builder().build().also { p ->
                    p.setSurfaceProvider(pv.surfaceProvider)
                }

                val ic = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val recorder = Recorder.Builder().build()
                val vc = VideoCapture.withOutput(recorder)

                val selector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                val boundCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    ic,
                    vc
                )

                imageCapture = ic
                videoCapture = vc
                camera = boundCamera
                maxZoomRatio = boundCamera.cameraInfo.zoomState.value?.maxZoomRatio ?: 4f
                currentZoom = 1f
            } catch (e: Exception) {
                Log.e("CameraScreen", "Camera bind failed", e)
            }
        }, mainExecutor)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CameraOverlay)
    ) {
        if (!hasCameraPermission) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Permission caméra requise")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    permissionLauncher.launch(permissionsNeeded)
                }) { Text("Accorder les permissions") }
            }
            return@Box
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Top letterbox band (fixed height)
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(CameraBand)
                .align(Alignment.TopStart))
            
            // Camera preview centered with gesture support
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { pv ->
                        pv.scaleType = PreviewView.ScaleType.FILL_CENTER
                        previewView = pv
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    // 🎯 Double-tap to flip camera
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                cameraViewModel.toggleLensFacing()
                            }
                        )
                    }
                    // 📌 Pinch-to-zoom (1x to maxZoomRatio)
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid: Offset, pan: Offset, gestureZoom: Float, rotation: Float ->
                            var newZoom = (currentZoom * gestureZoom).coerceIn(1f, maxZoomRatio)
                            currentZoom = newZoom
                            camera?.cameraControl?.setZoomRatio(newZoom)
                        }
                    }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = {
                    scope.launch { drawerState.open() }
                }) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = CameraIcon
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            if (CameraViewModel.ENABLE_VIDEO_CAPTURE && isRecording) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(androidx.compose.ui.graphics.Color.Red, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("REC  ${elapsedSeconds}s", style = MaterialTheme.typography.labelLarge)
                }
            }

            // Bottom control bar (fixed height, dark camera style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(CameraBar)
                    .align(Alignment.BottomCenter),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    // Left: Carnet button (navigate directly)
                    Text(
                        text = "Carnet",
                        style = MaterialTheme.typography.labelMedium,
                        color = CameraText,
                        modifier = Modifier.clickable { 
                            navController.navigate("voyages")
                        }
                    )

                        // Center: Capture button
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .pointerInteropFilter { ev ->
                                        when (ev.action) {
                                            MotionEvent.ACTION_DOWN -> {
                                                pressed = true
                                                pressDownTime = System.currentTimeMillis()
                                                if (CameraViewModel.ENABLE_VIDEO_CAPTURE) {
                                                    scope.launch {
                                                        delay(350)
                                                        if (pressed && !isRecording) {
                                                            startVideoRecording(
                                                                context = context,
                                                                mainExecutor = mainExecutor,
                                                                hasAudioPermission = hasAudioPermission,
                                                                videoCapture = videoCapture,
                                                                onStarted = {
                                                                    isRecording = true
                                                                    recordStartMillis = System.currentTimeMillis()
                                                                    cameraViewModel.setRecording(true)
                                                                },
                                                                onSaved = { savedPath ->
                                                                    isRecording = false
                                                                    cameraViewModel.setRecording(false)
                                                                    cameraViewModel.setLastMedia(savedPath, "VIDEO")
                                                                    val encodedPath = URLEncoder.encode(savedPath, "UTF-8")
                                                                    navController.navigate("capturePreview/$encodedPath/VIDEO")
                                                                },
                                                                onError = { err ->
                                                                    isRecording = false
                                                                    cameraViewModel.setRecording(false)
                                                                    Log.e("Camera", err)
                                                                },
                                                                setActiveRecording = { rec -> activeRecording = rec }
                                                            )
                                                        }
                                                    }
                                                }
                                                true
                                            }

                                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                                if (CameraViewModel.ENABLE_VIDEO_CAPTURE && isRecording) {
                                                    try {
                                                        activeRecording?.stop()
                                                    } catch (e: Exception) {
                                                        Log.e("Camera", "Error stopping recording: ${e.message}")
                                                    } finally {
                                                        activeRecording = null
                                                        pressed = false
                                                    }
                                                } else if (!isRecording) {
                                                    val pressDuration = System.currentTimeMillis() - pressDownTime
                                                    if (pressDuration < 350) {
                                                        takePhoto(
                                                            imageCapture = imageCapture,
                                                            context = context,
                                                            mainExecutor = mainExecutor,
                                                            onSaved = { savedPath ->
                                                                cameraViewModel.setLastMedia(savedPath, "PHOTO")
                                                                val encodedPath = URLEncoder.encode(savedPath, "UTF-8")
                                                                navController.navigate("capturePreview/$encodedPath/PHOTO")
                                                            },
                                                            onError = { err -> Log.e("Camera", err) }
                                                        )
                                                    }
                                                    pressed = false
                                                }
                                                true
                                            }

                                            else -> false
                                        }
                                    }
                            ) {
                                Surface(shape = CircleShape, tonalElevation = 2.dp) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .background(
                                                if (isRecording) androidx.compose.ui.graphics.Color.Red
                                                else CameraIcon,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(shape = CircleShape, tonalElevation = 4.dp) {
                                            Box(
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .background(CameraBar, CircleShape)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Right: Flip Camera button
                        IconButton(onClick = { cameraViewModel.toggleLensFacing() }) {
                            Icon(
                                Icons.Default.SwitchCamera,
                                contentDescription = "Flip camera",
                                tint = CameraIcon
                            )
                        }
                    }
                }
        }
    }
}

private fun takePhoto(
    imageCapture: ImageCapture?,
    context: Context,
    mainExecutor: Executor,
    onSaved: (String) -> Unit,
    onError: (String) -> Unit
) {
    val ic = imageCapture ?: run {
        onError("ImageCapture not ready")
        return
    }
    try {
        val file = MediaSaver.createImageFile(context)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        ic.takePicture(
            outputOptions,
            mainExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onSaved(file.absolutePath)
                }

                override fun onError(exception: ImageCaptureException) {
                    onError("Photo failed: ${exception.message}")
                }
            }
        )
    } catch (e: Exception) {
        onError("Photo failed: ${e.message}")
    }
}

/**
 * ✅ CameraX vidéo = API expérimentale -> opt-in requis.
 * C’est exactement pour prepareRecording / withAudioEnabled / start.
 */
@RequiresPermission(Manifest.permission.RECORD_AUDIO)
// @OptIn(androidx.camera.video.ExperimentalVideo::class)
private fun startVideoRecording(
    context: Context,
    mainExecutor: Executor,
    hasAudioPermission: Boolean,
    videoCapture: VideoCapture<Recorder>?,
    onStarted: () -> Unit,
    onSaved: (String) -> Unit,
    onError: (String) -> Unit,
    setActiveRecording: (Recording?) -> Unit
) {
    val vc = videoCapture ?: run {
        onError("VideoCapture not ready")
        return
    }

    try {
        val file = MediaSaver.createVideoFile(context)
        val options = FileOutputOptions.Builder(file).build()

        val pending: PendingRecording = vc.output.prepareRecording(context, options)
        val configured = if (hasAudioPermission) pending.withAudioEnabled() else pending

        val recording = configured.start(mainExecutor) { event ->
            when (event) {
                is VideoRecordEvent.Start -> onStarted()
                is VideoRecordEvent.Finalize -> {
                    setActiveRecording(null)
                    if (!event.hasError()) onSaved(file.absolutePath)
                    else onError("Video finalize error: ${event.error} / ${event.cause?.message}")
                }
            }
        }

        setActiveRecording(recording)
    } catch (e: Exception) {
        setActiveRecording(null)
        onError("Failed to start recording: ${e.message}")
    }
}
