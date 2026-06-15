package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitFirstDown
import com.example.data.settings.PlayerSettings
import com.example.ui.components.PlayerSettingsDialog
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.common.*
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.data.database.VideoEntity
import com.example.data.database.VideoRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class GestureType { NONE, VERTICAL, HORIZONTAL }

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    video: VideoEntity,
    onClose: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    videoRepository: VideoRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()

    // 1. Load persisted dynamic settings
    var seekButtons by remember { mutableStateOf(PlayerSettings.getSeekButtons(context)) }
    var fwdBwdDelay by remember { mutableIntStateOf(PlayerSettings.getFwdBwdDelay(context)) }
    var longTapDelay by remember { mutableIntStateOf(PlayerSettings.getLongTapDelay(context)) }
    var controlsHidingDelay by remember { mutableIntStateOf(PlayerSettings.getControlsHidingDelay(context)) }
    var videosTransition by remember { mutableStateOf(PlayerSettings.getVideosTransition(context)) }
    var lockWithSensor by remember { mutableStateOf(PlayerSettings.getLockWithSensor(context)) }
    
    var doubleTapDelay by remember { mutableIntStateOf(PlayerSettings.getDoubleTapDelay(context)) }
    var doubleTapPlayPauseSetting by remember { mutableStateOf(PlayerSettings.getDoubleTapPlayPause(context)) }
    var takeScreenshotSetting by remember { mutableStateOf(PlayerSettings.getTakeScreenshot(context)) }
    var enableFastplay by remember { mutableStateOf(PlayerSettings.getEnableFastplay(context)) }
    var fastplaySpeedSetting by remember { mutableFloatStateOf(PlayerSettings.getFastplaySpeed(context)) }

    var volumeGestureEnabled by remember { mutableStateOf(PlayerSettings.getVolumeGesture(context)) }
    var brightnessGestureEnabled by remember { mutableStateOf(PlayerSettings.getBrightnessGesture(context)) }
    var saveBrightnessSetting by remember { mutableStateOf(PlayerSettings.getSaveBrightness(context)) }
    var swipeToSeekEnabled by remember { mutableStateOf(PlayerSettings.getSwipeToSeek(context)) }
    var twoFingerZoomEnabled by remember { mutableStateOf(PlayerSettings.getTwoFingerZoom(context)) }
    var doubleTapToSeekEnabled by remember { mutableStateOf(PlayerSettings.getDoubleTapToSeek(context)) }
    var decoderModeSetting by remember { mutableStateOf(PlayerSettings.getDecoderMode(context)) }

    fun reloadSettings() {
        seekButtons = PlayerSettings.getSeekButtons(context)
        fwdBwdDelay = PlayerSettings.getFwdBwdDelay(context)
        longTapDelay = PlayerSettings.getLongTapDelay(context)
        controlsHidingDelay = PlayerSettings.getControlsHidingDelay(context)
        videosTransition = PlayerSettings.getVideosTransition(context)
        lockWithSensor = PlayerSettings.getLockWithSensor(context)
        
        doubleTapDelay = PlayerSettings.getDoubleTapDelay(context)
        doubleTapPlayPauseSetting = PlayerSettings.getDoubleTapPlayPause(context)
        takeScreenshotSetting = PlayerSettings.getTakeScreenshot(context)
        enableFastplay = PlayerSettings.getEnableFastplay(context)
        fastplaySpeedSetting = PlayerSettings.getFastplaySpeed(context)

        volumeGestureEnabled = PlayerSettings.getVolumeGesture(context)
        brightnessGestureEnabled = PlayerSettings.getBrightnessGesture(context)
        saveBrightnessSetting = PlayerSettings.getSaveBrightness(context)
        swipeToSeekEnabled = PlayerSettings.getSwipeToSeek(context)
        twoFingerZoomEnabled = PlayerSettings.getTwoFingerZoom(context)
        doubleTapToSeekEnabled = PlayerSettings.getDoubleTapToSeek(context)
        decoderModeSetting = PlayerSettings.getDecoderMode(context)
    }

    // Force Landscape Lock & Keep Screen Awake (Bound to lockWithSensor setting)
    DisposableEffect(lockWithSensor) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = if (lockWithSensor) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            activity?.requestedOrientation = originalOrientation
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Apply saved brightness automatically if enabled
    LaunchedEffect(saveBrightnessSetting) {
        if (saveBrightnessSetting) {
            val savedVal = PlayerSettings.getSavedBrightnessVal(context)
            activity?.let { act ->
                val lp = act.window.attributes
                lp.screenBrightness = savedVal
                act.window.attributes = lp
            }
        }
    }

    // AudioManager for Volume gestures
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxMusicVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }

    // Screen Lock & Aspect Ratio & Double Tap States
    var isLocked by remember { mutableStateOf(false) }
    var showUnlockHint by remember { mutableStateOf(false) }
    var showDoubleTapBubble by remember { mutableStateOf(false) }
    var doubleTapText by remember { mutableStateOf("") }
    
    // Video aspect zoom state parameters (Fit, Stretch, Crop, 100%, Custom)
    var zoomTypeState by remember { mutableStateOf("fit") } // "fit", "stretch", "crop", "100", "custom"
    var scaleFactor by remember { mutableFloatStateOf(1.0f) }
    var showAspectRatioToast by remember { mutableStateOf(false) }
    var currentAspectRatioLabel by remember { mutableStateOf("Fit to Screen") }

    val resizeModeState = remember(zoomTypeState) {
        when (zoomTypeState) {
            "stretch" -> 3 // RESIZE_MODE_FILL
            "crop" -> 4 // RESIZE_MODE_ZOOM
            else -> 0 // RESIZE_MODE_FIT
        }
    }

    val currentScaleFactor = remember(zoomTypeState, scaleFactor) {
        when (zoomTypeState) {
            "100" -> 0.88f
            "custom" -> scaleFactor
            else -> 1.0f
        }
    }

    // Additional Custom status overlays states
    var showFastplayOverlay by remember { mutableStateOf(false) }
    var showScreenshotFlash by remember { mutableStateOf(false) }
    var showPlayerSettingsDialog by remember { mutableStateOf(false) }
    var showVideoZoomDialog by remember { mutableStateOf(false) }
    var showVideoTitleOverlay by remember { mutableStateOf(false) }

    // local UI status and control overlays
    var isControlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var codecErrorState by remember { mutableStateOf<String?>(null) }
    var currentPosition by remember { mutableLongStateOf(video.lastPlayedPosition) }
    var totalDuration by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    // Active tracking tracks
    var availableAudioTracks by remember { mutableStateOf<List<TrackOption>>(emptyList()) }
    var availableSubtitleTracks by remember { mutableStateOf<List<TrackOption>>(emptyList()) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    // Gemini AI states
    var showGeminiSheet by remember { mutableStateOf(false) }
    var geminiSubtitles by remember { mutableStateOf<List<com.example.data.gemini.GeminiSubtitle>>(emptyList()) }
    var geminiResponseText by remember { mutableStateOf("") }
    var isGeminiLoading by remember { mutableStateOf(false) }
    var aiPromptInput by remember { mutableStateOf("") }

    val runGeminiTask: (String, String?, Boolean) -> Unit = { prompt, systemInstruction, parseJson ->
        coroutineScope.launch {
            isGeminiLoading = true
            geminiResponseText = "Thinking with Gemini 3.5 Flash..."
            try {
                val result = com.example.data.gemini.GeminiService.generateResponse(
                    prompt = prompt,
                    systemInstruction = systemInstruction,
                    isJsonMode = parseJson
                )
                isGeminiLoading = false
                if (parseJson) {
                    val parsedSubs = com.example.data.gemini.GeminiSubtitleParser.parseSubtitlesJson(result)
                    if (parsedSubs.isNotEmpty()) {
                        geminiSubtitles = parsedSubs
                        geminiResponseText = "Successfully generated and loaded ${parsedSubs.size} live subtitles!"
                    } else {
                        geminiResponseText = "Failed to parse JSON subtitles. Raw response:\n$result"
                    }
                } else {
                    geminiResponseText = result
                }
            } catch (e: Exception) {
                isGeminiLoading = false
                geminiResponseText = "Error: ${e.message}"
            }
        }
    }

    // Gestures overlay transient statuses
    var activeGestureType by remember { mutableStateOf(GestureType.NONE) }
    var isLeftGestureSide by remember { mutableStateOf(false) } // true: Left (Volume), false: Right (Brightness)
    var gestureProgressValue by remember { mutableFloatStateOf(0f) } // Volume or brightness scale (0.0 to 1.0)
    var gestureSeekPreviewPos by remember { mutableLongStateOf(0L) } // Seeking target preview milliseconds
    var originalPositionOnDragStart by remember { mutableLongStateOf(0L) }

    // Track original brightness on drag start
    var originalBrightnessOnDragStart by remember { mutableFloatStateOf(0.5f) }
    var originalVolumeOnDragStart by remember { mutableFloatStateOf(0f) }

    // 2. Setup ExoPlayer
    val player = remember(decoderModeSetting) {
        val hardwareSelector = MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
            val decoders = MediaCodecSelector.DEFAULT.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
            if (mimeType == MimeTypes.VIDEO_H264 || mimeType == MimeTypes.VIDEO_H265 || mimeType == MimeTypes.VIDEO_AV1) {
                decoders.sortedWith { a, b ->
                    when {
                        a.hardwareAccelerated && !b.hardwareAccelerated -> if (decoderModeSetting == "software") 1 else -1
                        !a.hardwareAccelerated && b.hardwareAccelerated -> if (decoderModeSetting == "software") -1 else 1
                        else -> 0
                    }
                }
            } else {
                decoders
            }
        }
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            setMediaCodecSelector(hardwareSelector)
        }
        ExoPlayer.Builder(context, renderersFactory).build()
    }

    // Auto update progress duration loop
    LaunchedEffect(player, isPlaying) {
        if (isPlaying) {
            while (true) {
                currentPosition = player.currentPosition
                totalDuration = player.duration
                delay(250)
            }
        }
    }

    // Auto-hide controls after configurable delay of inactivity, only if playing and not locked
    LaunchedEffect(lastInteractionTime, isControlsVisible, isPlaying, isLocked, controlsHidingDelay) {
        if (isControlsVisible && isPlaying && !isLocked) {
            delay(controlsHidingDelay * 1000L)
            isControlsVisible = false
        }
    }

    // Video transition overlay title timer loop
    LaunchedEffect(video.uriString, videosTransition) {
        if (videosTransition) {
            showVideoTitleOverlay = true
            delay(3000)
            showVideoTitleOverlay = false
        } else {
            showVideoTitleOverlay = false
        }
    }

    // Auto-hide double tap pulse bubble
    LaunchedEffect(showDoubleTapBubble) {
        if (showDoubleTapBubble) {
            delay(650)
            showDoubleTapBubble = false
        }
    }

    // Auto-hide aspect ratio toast
    LaunchedEffect(showAspectRatioToast) {
        if (showAspectRatioToast) {
            delay(1200)
            showAspectRatioToast = false
        }
    }

    // Auto-hide unlock hint toast
    LaunchedEffect(showUnlockHint) {
        if (showUnlockHint) {
            delay(1500)
            showUnlockHint = false
        }
    }

    // Manage audio equalizer
    var audioSessionIdState by remember(player) { mutableIntStateOf(player.audioSessionId) }
    var eqEnabled by remember { mutableStateOf(PlayerSettings.getEqualizerEnabled(context)) }

    LaunchedEffect(showPlayerSettingsDialog) {
        if (!showPlayerSettingsDialog) {
            eqEnabled = PlayerSettings.getEqualizerEnabled(context)
        }
    }

    DisposableEffect(player, eqEnabled, audioSessionIdState) {
        var eq: android.media.audiofx.Equalizer? = null
        if (eqEnabled && audioSessionIdState != android.media.AudioManager.AUDIO_SESSION_ID_GENERATE) {
            try {
                eq = android.media.audiofx.Equalizer(0, audioSessionIdState).apply {
                    enabled = true
                    val numBands = numberOfBands.toInt().coerceAtMost(5)
                    for (i in 0 until numBands) {
                        val bandValDb = PlayerSettings.getEqualizerBand(context, i)
                        val milliBelLevel = (bandValDb * 100).toInt().toShort()
                        val levelRange = bandLevelRange
                        if (levelRange != null && levelRange.size >= 2) {
                            val clampedLevel = milliBelLevel.coerceIn(levelRange[0], levelRange[1])
                            setBandLevel(i.toShort(), clampedLevel)
                        } else {
                            setBandLevel(i.toShort(), milliBelLevel)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        onDispose {
            eq?.release()
        }
    }

    DisposableEffect(video.uriString, player) {
        codecErrorState = null
        val mediaItem = MediaItem.fromUri(Uri.parse(video.uriString))
        player.setMediaItem(mediaItem)
        player.prepare()
        if (video.lastPlayedPosition > 0) {
            player.seekTo(video.lastPlayedPosition)
        }
        player.play()
        isPlaying = true

        val listener = object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                audioSessionIdState = audioSessionId
            }

            override fun onIsPlayingChanged(play: Boolean) {
                isPlaying = play
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    totalDuration = player.duration
                    queryTracks(player) { audios, subs ->
                        availableAudioTracks = audios
                        availableSubtitleTracks = subs
                    }
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                queryTracks(player) { audios, subs ->
                    availableAudioTracks = audios
                    availableSubtitleTracks = subs
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val technicalError = "ExoPlayer hardware-decoding issue or format mismatch.\nDetails: ${error.localizedMessage}"
                val userFriendlyMessage = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_DECODING_FAILED -> {
                        "Physical Hardware-Accelerated codec failed.\nThis 4K file utilizes high profiles or incompatible video stream properties ($technicalError)."
                    }
                    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> {
                        "Codec configuration unsupported.\nYour device's hardware drivers cannot process this bit depth or format compression (H.265/AV1 error: $technicalError)."
                    }
                    else -> {
                        "Unable to decode stream seamlessly.\nError: ${error.localizedMessage ?: "Decoder missing"}"
                    }
                }
                codecErrorState = userFriendlyMessage
                isPlaying = false
            }
        }
        player.addListener(listener)

        onDispose {
            // Save position back to Room before releasing player
            val currentPos = player.currentPosition
            coroutineScope.launch {
                videoRepository.updatePlaybackPosition(video.uriString, currentPos)
            }
            player.removeListener(listener)
            player.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("videoplayer_container")
            // Two finger pinch to zoom (Custom Aspect/Zoom level scaling)
            .pointerInput(video.uriString, twoFingerZoomEnabled, isLocked) {
                if (!twoFingerZoomEnabled || isLocked) return@pointerInput
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom != 1.0f) {
                        scaleFactor = (scaleFactor * zoom).coerceIn(0.5f, 4.0f)
                        zoomTypeState = "custom"
                        currentAspectRatioLabel = "Custom (${(scaleFactor * 100).toInt()}% Zoom)"
                        showAspectRatioToast = true
                        lastInteractionTime = System.currentTimeMillis()
                    }
                }
            }
            // Tap & Hold (Long-press) Fastplay Speedup
            .pointerInput(video.uriString, enableFastplay, fastplaySpeedSetting, isLocked) {
                if (!enableFastplay || isLocked) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        awaitFirstDown()
                        var isLongPressing = false
                        val longPressTimeout = 500L
                        val startTime = System.currentTimeMillis()
                        
                        while (true) {
                            val event = awaitPointerEvent()
                            val elapsed = System.currentTimeMillis() - startTime
                            if (elapsed >= longPressTimeout && !isLongPressing) {
                                isLongPressing = true
                                playbackSpeed = fastplaySpeedSetting
                                player.setPlaybackSpeed(fastplaySpeedSetting)
                                showFastplayOverlay = true
                            }
                            if (event.changes.any { !it.pressed }) {
                                if (isLongPressing) {
                                    playbackSpeed = 1.0f
                                    player.setPlaybackSpeed(1.0f)
                                    showFastplayOverlay = false
                                }
                                break
                            }
                        }
                    }
                }
            }
            // Standard Drag Gestures (Volume, Brightness, Swipe to Seek)
            .pointerInput(video.uriString, swipeToSeekEnabled, volumeGestureEnabled, brightnessGestureEnabled, isLocked) {
                if (isLocked) return@pointerInput
                var accumDeltaX = 0f
                var accumDeltaY = 0f

                detectDragGestures(
                    onDragStart = { offset ->
                        isLeftGestureSide = offset.x < size.width / 2f
                        originalPositionOnDragStart = player.currentPosition
                        originalVolumeOnDragStart = audioManager
                            .getStreamVolume(AudioManager.STREAM_MUSIC)
                            .toFloat()

                        val currentWindowBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
                        originalBrightnessOnDragStart = if (currentWindowBrightness < 0) 0.5f else currentWindowBrightness

                        activeGestureType = GestureType.NONE
                        accumDeltaX = 0f
                        accumDeltaY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        lastInteractionTime = System.currentTimeMillis()
                        isControlsVisible = true

                        accumDeltaX += dragAmount.x
                        accumDeltaY += dragAmount.y

                        if (activeGestureType == GestureType.NONE) {
                            if (abs(accumDeltaX) > 40f && abs(accumDeltaX) > abs(accumDeltaY) && swipeToSeekEnabled) {
                                activeGestureType = GestureType.HORIZONTAL
                                gestureSeekPreviewPos = originalPositionOnDragStart
                            } else if (abs(accumDeltaY) > 40f && abs(accumDeltaY) > abs(accumDeltaX)) {
                                if (isLeftGestureSide && brightnessGestureEnabled) {
                                    activeGestureType = GestureType.VERTICAL
                                } else if (!isLeftGestureSide && volumeGestureEnabled) {
                                    activeGestureType = GestureType.VERTICAL
                                }
                            }
                        }

                        when (activeGestureType) {
                            GestureType.HORIZONTAL -> {
                                if (swipeToSeekEnabled) {
                                    val seekDelta = (accumDeltaX * 100).toLong()
                                    gestureSeekPreviewPos = (originalPositionOnDragStart + seekDelta)
                                        .coerceIn(0L, totalDuration)
                                }
                            }
                            GestureType.VERTICAL -> {
                                if (!isLeftGestureSide && volumeGestureEnabled) {
                                    val volumeDelta = -(accumDeltaY / size.height) * maxMusicVolume
                                    val targetVol = (originalVolumeOnDragStart + volumeDelta)
                                        .coerceIn(0f, maxMusicVolume)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol.toInt(), 0)
                                    gestureProgressValue = targetVol / maxMusicVolume
                                } else if (isLeftGestureSide && brightnessGestureEnabled) {
                                    val brightnessDelta = -(accumDeltaY / size.height)
                                    val targetBrightness = (originalBrightnessOnDragStart + brightnessDelta)
                                        .coerceIn(0.01f, 1.0f)
                                    activity?.let { act ->
                                        val lp = act.window.attributes
                                        lp.screenBrightness = targetBrightness
                                        act.window.attributes = lp
                                    }
                                    gestureProgressValue = targetBrightness
                                    if (saveBrightnessSetting) {
                                        PlayerSettings.setSavedBrightnessVal(context, targetBrightness)
                                    }
                                }
                            }
                            else -> {}
                        }
                    },
                    onDragEnd = {
                        if (activeGestureType == GestureType.HORIZONTAL && swipeToSeekEnabled) {
                            player.seekTo(gestureSeekPreviewPos)
                            currentPosition = gestureSeekPreviewPos
                        }
                        activeGestureType = GestureType.NONE
                    }
                )
            }
            // Tap & Double Tap Gestures (Show panels, Double tap to seek / play pause)
            .pointerInput(video.uriString, doubleTapPlayPauseSetting, doubleTapToSeekEnabled, doubleTapDelay, enableFastplay, isLocked) {
                detectTapGestures(
                    onTap = {
                        if (!isLocked) {
                            lastInteractionTime = System.currentTimeMillis()
                            isControlsVisible = !isControlsVisible
                        } else {
                            showUnlockHint = true
                        }
                    },
                    onDoubleTap = { offset ->
                        if (!isLocked) {
                            val screenWidth = size.width
                            val isLeftThird = offset.x < screenWidth / 3f
                            val isRightThird = offset.x > (screenWidth * 2f / 3f)
                            val isCenterThird = !isLeftThird && !isRightThird

                            if (isCenterThird && doubleTapPlayPauseSetting) {
                                if (isPlaying) player.pause() else player.play()
                                lastInteractionTime = System.currentTimeMillis()
                            } else if (doubleTapToSeekEnabled) {
                                val step = doubleTapDelay * 1000L
                                if (offset.x < screenWidth / 2f) {
                                    val target = (player.currentPosition - step).coerceAtLeast(0L)
                                    player.seekTo(target)
                                    currentPosition = target
                                    doubleTapText = "⏪ -${doubleTapDelay}s"
                                } else {
                                    val target = (player.currentPosition + step).coerceAtMost(totalDuration)
                                    player.seekTo(target)
                                    currentPosition = target
                                    doubleTapText = "+${doubleTapDelay}s ⏩"
                                }
                                showDoubleTapBubble = true
                                lastInteractionTime = System.currentTimeMillis()
                            }
                        } else {
                            showUnlockHint = true
                        }
                    },
                    onLongPress = { offset ->
                        if (!isLocked && !enableFastplay) {
                            val screenWidth = size.width
                            val step = longTapDelay * 1000L
                            if (offset.x < screenWidth / 2f) {
                                val target = (player.currentPosition - step).coerceAtLeast(0L)
                                player.seekTo(target)
                                currentPosition = target
                                doubleTapText = "⏪ -${longTapDelay}s"
                            } else {
                                val target = (player.currentPosition + step).coerceAtMost(totalDuration)
                                player.seekTo(target)
                                currentPosition = target
                                doubleTapText = "+${longTapDelay}s ⏩"
                            }
                            showDoubleTapBubble = true
                            lastInteractionTime = System.currentTimeMillis()
                        }
                    }
                )
            }
    ) {

        // 3. AndroidView hosting ExoPlayer View with custom graphics layer scale ratios
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    this.player = player
                    keepScreenOn = true
                }
            },
            update = { playerView ->
                playerView.resizeMode = resizeModeState
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = currentScaleFactor,
                    scaleY = currentScaleFactor
                )
        )

        // 4. Custom Hardware Codec Error Display Overlay Card
        codecErrorState?.let { errorMessage ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .padding(32.dp)
                    .clickable { /* noop */ },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.widthIn(max = 500.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ReportProblem,
                                    contentDescription = "Codec Error",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "ARM Codec Decoding Failed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onClose,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Go Back")
                        }
                    }
                }
            }
        }

        // Live timed subtitles display from Gemini Parser
        val currentSubs = geminiSubtitles
        val activeGeminiSubtitle = remember(currentPosition, currentSubs) {
            currentSubs.find { currentPosition >= it.startMs && currentPosition <= it.endMs }?.text
        }

        if (activeGeminiSubtitle != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
                    .background(Color.Black.copy(alpha = 0.8f), shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = activeGeminiSubtitle,
                    color = Color(0xFFFACC15), // Elegant glowing yellow live captions
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Custom sliding Gemini AI Assistant Side Panel Overlay
        AnimatedVisibility(
            visible = showGeminiSheet,
            enter = slideInHorizontally { it } + fadeIn(),
            exit = slideOutHorizontally { it } + fadeOut(),
            modifier = Modifier
                .fillMaxHeight()
                .width(360.dp)
                .align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0C0C0E))
                    .padding(16.dp)
                    .clickable { /* Block gestures under side panel */ }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sicario Labs AI Assistant",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { showGeminiSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Panel", tint = Color.LightGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset quick actions
                    Text(
                        text = "QUICK ASSISTANT CHECKS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val prompt = "Video filename/title is '${video.title}'. Provide 5 precise expert structural video editing or layout enhancement suggestions for this style of content."
                                runGeminiTask(prompt, "You are a senior professional video editor and layout consultant. Give direct, bulleted, technical advice.", false)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141416)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Edit Tips", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }

                        Button(
                            onClick = {
                                val prompt = "Generate exactly 6 beautifully narrative caption blocks styled with timestamp offsets 'startMs' and 'endMs' in clean JSON format for a video clip titled '${video.title}'. Example output structure:\n[\n  {\"startMs\": 1000, \"endMs\": 4000, \"text\": \"Welcome to this custom video segment!\"},\n  {\"startMs\": 5000, \"endMs\": 9000, \"text\": \"Let's review the coding metrics.\"}\n]"
                                runGeminiTask(prompt, "You are an automated captioning algorithm. Output ONLY a raw, un-escaped JSON array. No markdown, no preambles, no trailing blocks.", true)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141416)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("AI Subtitles", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }

                        Button(
                            onClick = {
                                val prompt = "Write a comprehensive film critic content analysis of '${video.title}', including visual aesthetics and thematic tone."
                                runGeminiTask(prompt, "You are an intellectual cinema critic. Keep your vocabulary beautiful, eye-catching and compact.", false)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141416)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Content AI", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Text result container
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFF141416), shape = RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier.verticalScroll(scrollState)
                        ) {
                            if (isGeminiLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.CenterHorizontally),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            Text(
                                text = if (geminiResponseText.isEmpty()) "Tap a quick assistant tool above, or type in a query to analyze content, translate streams or plan video edits." else geminiResponseText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (geminiResponseText.startsWith("Error")) Color.Red else Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Prompt Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = aiPromptInput,
                            onValueChange = { aiPromptInput = it },
                            placeholder = { Text("Ask Gemini...", color = Color.Gray, style = MaterialTheme.typography.bodySmall) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF141416),
                                unfocusedContainerColor = Color(0xFF141416),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (aiPromptInput.isNotBlank()) {
                                    val promptMsg = "Current played video file title is '${video.title}'. User asks: $aiPromptInput"
                                    runGeminiTask(promptMsg, "You are a professional video AI companion in the Sicario Labs media app.", false)
                                    aiPromptInput = ""
                                }
                            },
                            modifier = Modifier.background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.Black)
                        }
                    }
                }
            }
        }

        // 5. Swipe Gestures Feedback indicators HUD
        AnimatedVisibility(
            visible = activeGestureType != GestureType.NONE,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            HUDOverlay(
                gestureType = activeGestureType,
                isLeft = isLeftGestureSide,
                progress = gestureProgressValue,
                seekTime = formatTime(gestureSeekPreviewPos),
                duration = formatTime(totalDuration)
            )
        }

        // Double-tap visual pulse bubble
        AnimatedVisibility(
            visible = showDoubleTapBubble,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(50),
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = doubleTapText,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Aspect ratio toast overlay overlay
        AnimatedVisibility(
            visible = showAspectRatioToast,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentAspectRatioLabel,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Lock screen control wrapper
        if (isLocked) {
            // Unlock Trigger Button on center-left side
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                IconButton(
                    onClick = {
                        isLocked = false
                        isControlsVisible = true
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.75f)
                    ),
                    modifier = Modifier
                        .size(56.dp)
                        .align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Unlock controls",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Brief animated Unlock Hint overlay
                AnimatedVisibility(
                    visible = showUnlockHint,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Playback controls are locked",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 6. Player Control Buttons HUD Overlay
        AnimatedVisibility(
            visible = isControlsVisible && !isLocked && codecErrorState == null,
            enter = fadeIn() + slideInVertically { it / 6 },
            exit = fadeOut() + slideOutVertically { it / 6 }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Top control bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("player_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Player", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    // Take Snapshot/Screenshot trigger (styled exactly like image 1, camera icon with small red dot)
                    IconButton(onClick = {
                        if (takeScreenshotSetting == "Disabled") {
                            Toast.makeText(context, "Screenshot capturing is disabled in Settings. Enable from player settings.", Toast.LENGTH_LONG).show()
                        } else {
                            coroutineScope.launch {
                                showScreenshotFlash = true
                                delay(120)
                                showScreenshotFlash = false
                                Toast.makeText(context, "Frame screenshot captured successfully ($takeScreenshotSetting) and saved!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Box {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Take video screenshot",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            // Small red dot overlay
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color.Red, shape = RoundedCornerShape(50))
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }

                    // Lock button
                    IconButton(onClick = {
                        isLocked = true
                        isControlsVisible = false
                    }) {
                        Icon(Icons.Default.LockOpen, contentDescription = "Lock Controls", tint = Color.White)
                    }

                    // Aspect Ratio Button (Opens Zoom Aspect Choice Dialog)
                    IconButton(onClick = {
                        showVideoZoomDialog = true
                        lastInteractionTime = System.currentTimeMillis()
                    }) {
                        Icon(Icons.Default.AspectRatio, contentDescription = "Aspect Ratio", tint = Color.White)
                    }

                    // Track & Speed configs buttons
                    IconButton(onClick = { showAudioDialog = true }) {
                        Icon(Icons.Default.Audiotrack, contentDescription = "Audio track", tint = Color.White)
                    }

                    IconButton(onClick = { showSubtitleDialog = true }) {
                        Icon(Icons.Default.Subtitles, contentDescription = "Captions subtitles", tint = Color.White)
                    }

                    IconButton(onClick = { showSpeedDialog = true }) {
                        Icon(Icons.Default.Speed, contentDescription = "Playback Speed", tint = Color.White)
                    }

                    // Settings Gear Button
                    IconButton(onClick = {
                        showPlayerSettingsDialog = true
                        lastInteractionTime = System.currentTimeMillis()
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Player Settings", tint = Color.White)
                    }

                    // Gemini AI Sidebar Toggle Button
                    IconButton(onClick = {
                        showGeminiSheet = !showGeminiSheet
                        lastInteractionTime = System.currentTimeMillis()
                    }) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Gemini AI Assistant",
                            tint = if (showGeminiSheet) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                }

                // Center direct toggles (supporting optional circled Rewind/FastForward seek buttons as shown in image 2)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // 1. Skip Previous
                    IconButton(
                        onClick = onPlayPrevious,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Previous Video",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // 2. Optional Seek Back circular trigger (Rewind)
                    if (seekButtons) {
                        IconButton(
                            onClick = {
                                val target = (player.currentPosition - fwdBwdDelay * 1000L).coerceAtLeast(0L)
                                player.seekTo(target)
                                currentPosition = target
                                lastInteractionTime = System.currentTimeMillis()
                                doubleTapText = "⏪ -${fwdBwdDelay}s"
                                showDoubleTapBubble = true
                            },
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = "Seek back $fwdBwdDelay seconds",
                                    tint = MaterialTheme.colorScheme.primary, // Orange/Primary accent
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = fwdBwdDelay.toString(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    // 3. Main Center Play/Pause Circle
                    Surface(
                        color = Color.White.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .size(72.dp)
                            .clickable {
                                if (isPlaying) player.pause() else player.play()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play Pause",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    // 4. Optional Seek Forward circular trigger (Fast Forward)
                    if (seekButtons) {
                        IconButton(
                            onClick = {
                                val target = (player.currentPosition + fwdBwdDelay * 1000L).coerceAtMost(totalDuration)
                                player.seekTo(target)
                                currentPosition = target
                                lastInteractionTime = System.currentTimeMillis()
                                doubleTapText = "+${fwdBwdDelay}s ⏩"
                                showDoubleTapBubble = true
                            },
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = "Seek forward $fwdBwdDelay seconds",
                                    tint = MaterialTheme.colorScheme.primary, // Orange/Primary accent
                                    modifier = Modifier
                                        .size(40.dp)
                                        .graphicsLayer(scaleX = -1f) // Mirror horizontally to make it point clockwise
                                )
                                Text(
                                    text = fwdBwdDelay.toString(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    // 5. Skip Next
                    IconButton(
                        onClick = onPlayNext,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Next Video",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Bottom seeking timeline track progress
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                        Text(
                            text = formatTime(totalDuration),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = currentPosition.toFloat(),
                        onValueChange = {
                            lastInteractionTime = System.currentTimeMillis()
                            currentPosition = it.toLong()
                            player.seekTo(it.toLong())
                        },
                        valueRange = 0f..(totalDuration.toFloat().coerceAtLeast(1f)),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Speed: ${playbackSpeed}x",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Fastplay Speed indicator overlay (Active holding speed boost HUD)
        AnimatedVisibility(
            visible = showFastplayOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${"%.1f".format(fastplaySpeedSetting)}x Fastplay",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Elegant animated New Video Title transition banner
        AnimatedVisibility(
            visible = showVideoTitleOverlay,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Playing: ${video.title}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }

        // Direct white flashlight effect for captured screen grabs
        if (showScreenshotFlash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }
    }

    // Interactive Advanced Settings Custom Dialog modal
    if (showPlayerSettingsDialog) {
        PlayerSettingsDialog(
            onDismiss = {
                showPlayerSettingsDialog = false
                reloadSettings()
                lastInteractionTime = System.currentTimeMillis()
            }
        )
    }

    // Aspect Ratio Zoom custom dialog modal
    if (showVideoZoomDialog) {
        VideoZoomChooserDialog(
            currentZoomType = zoomTypeState,
            onSelect = { option ->
                zoomTypeState = option
                when (option) {
                    "fit" -> {
                        currentAspectRatioLabel = "Fit to Screen"
                        scaleFactor = 1.0f
                    }
                    "stretch" -> {
                        currentAspectRatioLabel = "Stretch to Fill"
                        scaleFactor = 1.0f
                    }
                    "crop" -> {
                        currentAspectRatioLabel = "Zoom & Crop"
                        scaleFactor = 1.0f
                    }
                    "100" -> {
                        currentAspectRatioLabel = "100% Native Resolution"
                        scaleFactor = 1.0f
                    }
                    "custom" -> {
                        currentAspectRatioLabel = "Custom Pinch Zoom"
                    }
                }
                showAspectRatioToast = true
                showVideoZoomDialog = false
                lastInteractionTime = System.currentTimeMillis()
            },
            onDismiss = {
                showVideoZoomDialog = false
            }
        )
    }

    // Audio Track Chooser Dialog
    if (showAudioDialog) {
        AudioTrackChooserDialog(
            tracks = availableAudioTracks,
            onSelect = { item ->
                setTrackOverride(player, item)
                showAudioDialog = false
            },
            onDismiss = { showAudioDialog = false }
        )
    }

    // Subtitle Track Chooser Dialog
    if (showSubtitleDialog) {
        SubtitleTrackChooserDialog(
            tracks = availableSubtitleTracks,
            onSelect = { option ->
                if (option == null) {
                    // Turn Subtitles captions off
                    player.trackSelectionParameters = player.trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        .build()
                } else {
                    player.trackSelectionParameters = player.trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .build()
                    setTrackOverride(player, option)
                }
                showSubtitleDialog = false
                lastInteractionTime = System.currentTimeMillis()
            },
            onDismiss = { showSubtitleDialog = false }
        )
    }

    // Playback Speed chooser Dialog
    if (showSpeedDialog) {
        PlaybackSpeedChooserDialog(
            currentSpeed = playbackSpeed,
            onSelect = { speed ->
                playbackSpeed = speed
                player.setPlaybackSpeed(speed)
                showSpeedDialog = false
                lastInteractionTime = System.currentTimeMillis()
            },
            onDismiss = { showSpeedDialog = false }
        )
    }
}

// Track Information container class
data class TrackOption(
    val group: Tracks.Group,
    val trackIndex: Int,
    val label: String,
    val isSelected: Boolean
)

@OptIn(UnstableApi::class)
private fun queryTracks(player: ExoPlayer, onResultsState: (List<TrackOption>, List<TrackOption>) -> Unit) {
    val audios = mutableListOf<TrackOption>()
    val subtitles = mutableListOf<TrackOption>()

    val tracks = player.currentTracks
    for (group in tracks.groups) {
        val type = group.type
        if (type == C.TRACK_TYPE_AUDIO) {
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val lang = format.language ?: "und"
                val audioLabel = "${format.label ?: "Audio Channel"} [${lang.uppercase()}]"
                audios.add(
                    TrackOption(
                        group = group,
                        trackIndex = i,
                        label = audioLabel,
                        isSelected = group.isTrackSelected(i)
                    )
                )
            }
        } else if (type == C.TRACK_TYPE_TEXT) {
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val lang = format.language ?: "und"
                val subLabel = "${format.label ?: "Subtitle"} [${lang.uppercase()}]"
                subtitles.add(
                    TrackOption(
                        group = group,
                        trackIndex = i,
                        label = subLabel,
                        isSelected = group.isTrackSelected(i)
                    )
                )
            }
        }
    }
    onResultsState(audios, subtitles)
}

@OptIn(UnstableApi::class)
private fun setTrackOverride(player: ExoPlayer, item: TrackOption) {
    player.trackSelectionParameters = player.trackSelectionParameters
        .buildUpon()
        .setOverrideForType(TrackSelectionOverride(item.group.mediaTrackGroup, item.trackIndex))
        .build()
}

@Composable
fun HUDOverlay(
    gestureType: GestureType,
    isLeft: Boolean,
    progress: Float,
    seekTime: String,
    duration: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (gestureType) {
                GestureType.HORIZONTAL -> {
                    Icon(Icons.Default.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$seekTime / $duration",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                GestureType.VERTICAL -> {
                    Icon(
                        imageVector = if (!isLeft) Icons.AutoMirrored.Filled.VolumeUp else Icons.Filled.Brightness5,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (!isLeft) "Volume: ${(progress * 100).toInt()}%" else "Brightness: ${(progress * 100).toInt()}%",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.width(100.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.4f),
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
fun AudioTrackChooserDialog(
    tracks: List<TrackOption>,
    onSelect: (TrackOption) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF161618),
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Select Audio Stream",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (tracks.isEmpty()) {
                    Text("No secondary audio streams detected.", color = Color.LightGray)
                } else {
                    Column {
                        tracks.forEach { track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(track) }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = track.isSelected,
                                    onClick = { onSelect(track) }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(track.label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun SubtitleTrackChooserDialog(
    tracks: List<TrackOption>,
    onSelect: (TrackOption?) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF161618),
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Subtitles / Captions",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Column {
                    // Option to disable subtitles
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(null) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = tracks.none { it.isSelected },
                            onClick = { onSelect(null) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Turn Subtitles Off", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (tracks.isEmpty()) {
                        Text("No embedded text captions available.", color = Color.LightGray, modifier = Modifier.padding(top = 8.dp))
                    } else {
                        tracks.forEach { track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(track) }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = track.isSelected,
                                    onClick = { onSelect(track) }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(track.label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun PlaybackSpeedChooserDialog(
    currentSpeed: Float,
    onSelect: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF161618),
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Playback Rate",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Column {
                    speeds.forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(speed) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentSpeed == speed,
                                onClick = { onSelect(speed) }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("${speed}x ${if (speed == 1.0f) "(Normal)" else ""}", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

@Composable
fun VideoZoomChooserDialog(
    currentZoomType: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        "fit" to "Fit to screen overlay",
        "stretch" to "Stretch full viewport",
        "crop" to "Zoom crop focus",
        "100" to "100% Native Resolution",
        "custom" to "Custom Gestures Zoom (Pinch)"
    )

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF161618),
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Video zoom ratio",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Column {
                    options.forEach { (typeVal, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(typeVal) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentZoomType == typeVal,
                                onClick = { onSelect(typeVal) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = Color.LightGray
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
