package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import com.example.data.settings.PlayerSettings

@Composable
fun PlayerSettingsDialog(
    displayViewMode: String = "",
    onViewModeChanged: ((String) -> Unit)? = null,
    showOnlyFavoritesSetting: Boolean = false,
    onShowOnlyFavoritesChanged: ((Boolean) -> Unit)? = null,
    playbackActionSetting: String = "",
    onPlaybackActionChanged: ((String) -> Unit)? = null,
    sortFieldSetting: String = "",
    sortOrderSetting: String = "",
    onSortChanged: ((String, String) -> Unit)? = null,
    groupBySetting: String = "",
    onGroupByChanged: ((String) -> Unit)? = null,
    aiSummariesEnabled: Boolean = false,
    onAiSummariesEnabledChanged: ((Boolean) -> Unit)? = null,
    onFolderPickerLaunch: (() -> Unit)? = null,
    onDocumentPickerLaunch: (() -> Unit)? = null,
    onDeepScan: (() -> Unit)? = null,
    onAutomatedScan: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Controls, 1 = Gestures, 2 = Equalizer, 3 = Display/Sort, 4 = Library

    // Load Settings
    var seekButtons by remember { mutableStateOf(PlayerSettings.getSeekButtons(context)) }
    var fwdBwdDelay by remember { mutableIntStateOf(PlayerSettings.getFwdBwdDelay(context)) }
    var longTapDelay by remember { mutableIntStateOf(PlayerSettings.getLongTapDelay(context)) }
    var controlsHidingDelay by remember { mutableIntStateOf(PlayerSettings.getControlsHidingDelay(context)) }
    var videosTransition by remember { mutableStateOf(PlayerSettings.getVideosTransition(context)) }
    var lockWithSensor by remember { mutableStateOf(PlayerSettings.getLockWithSensor(context)) }
    
    var doubleTapDelay by remember { mutableIntStateOf(PlayerSettings.getDoubleTapDelay(context)) }
    var doubleTapPlayPause by remember { mutableStateOf(PlayerSettings.getDoubleTapPlayPause(context)) }
    var takeScreenshot by remember { mutableStateOf(PlayerSettings.getTakeScreenshot(context)) }
    var enableFastplay by remember { mutableStateOf(PlayerSettings.getEnableFastplay(context)) }
    var fastplaySpeed by remember { mutableFloatStateOf(PlayerSettings.getFastplaySpeed(context)) }

    // Gestures States
    var volumeGesture by remember { mutableStateOf(PlayerSettings.getVolumeGesture(context)) }
    var brightnessGesture by remember { mutableStateOf(PlayerSettings.getBrightnessGesture(context)) }
    var saveBrightness by remember { mutableStateOf(PlayerSettings.getSaveBrightness(context)) }
    var swipeToSeek by remember { mutableStateOf(PlayerSettings.getSwipeToSeek(context)) }
    var twoFingerZoom by remember { mutableStateOf(PlayerSettings.getTwoFingerZoom(context)) }
    var doubleTapToSeek by remember { mutableStateOf(PlayerSettings.getDoubleTapToSeek(context)) }

    // Library & Display local state mirrors for live visual changes
    var displayViewModeVal by remember { mutableStateOf(displayViewMode) }
    var showOnlyFavoritesVal by remember { mutableStateOf(showOnlyFavoritesSetting) }
    var playbackActionVal by remember { mutableStateOf(playbackActionSetting) }
    var sortFieldVal by remember { mutableStateOf(sortFieldSetting) }
    var sortOrderVal by remember { mutableStateOf(sortOrderSetting) }
    var groupByVal by remember { mutableStateOf(groupBySetting) }
    var aiSummariesVal by remember { mutableStateOf(aiSummariesEnabled) }
    var decoderModeVal by remember { mutableStateOf(PlayerSettings.getDecoderMode(context)) }

    // Dropdown Dialog state keys
    var showDelayPickerType by remember { mutableStateOf<String?>(null) } // "fwd", "long", "double", "screenshot", "view_mode", "sort_field", "sort_order", "group_by", "playback_action", "decoder_mode"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F0F11)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Row - Scrollable Minimalist Tabs, zero distraction
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Controls",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTab == 0) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier
                                .clickable { activeTab = 0 }
                                .padding(vertical = 4.dp)
                        )
                        Text(
                            text = "Gestures",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTab == 1) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier
                                .clickable { activeTab = 1 }
                                .padding(vertical = 4.dp)
                        )
                        Text(
                            text = "Equalizer",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTab == 2) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier
                                .clickable { activeTab = 2 }
                                .padding(vertical = 4.dp)
                        )
                        
                        if (onSortChanged != null) {
                            Text(
                                text = "Display & Sorting",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTab == 3) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier
                                    .clickable { activeTab = 3 }
                                    .padding(vertical = 4.dp)
                            )
                        }

                        if (onAutomatedScan != null) {
                            Text(
                                text = "Library & Scanning",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTab == 4) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier
                                    .clickable { activeTab = 4 }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Settings", tint = Color.LightGray)
                    }
                }

                Divider(color = Color(0xFF222225))

                // Scrollable Setting list
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        if (activeTab == 0) {
                            // Section: Player controls setting options
                            SettingCheckbox(
                                title = "Seek buttons",
                                description = "Show rewind and fast forward buttons on the video interface",
                                checked = seekButtons,
                                onCheckedChange = {
                                    seekButtons = it
                                    PlayerSettings.setSeekButtons(context, it)
                                }
                            )

                            SettingClickable(
                                title = "Forward/backward time delay",
                                value = "$fwdBwdDelay seconds",
                                onClick = { showDelayPickerType = "fwd" }
                            )

                            SettingClickable(
                                title = "Long tap forward/backward time delay",
                                value = "$longTapDelay seconds",
                                onClick = { showDelayPickerType = "long" }
                            )

                            SettingSlider(
                                title = "Video player controls hiding delay",
                                description = "${controlsHidingDelay}s",
                                value = controlsHidingDelay.toFloat(),
                                valueRange = 1f..10f,
                                steps = 8,
                                onValueChange = {
                                    controlsHidingDelay = it.toInt()
                                    PlayerSettings.setControlsHidingDelay(context, it.toInt())
                                }
                            )

                            SettingCheckbox(
                                title = "Videos transition",
                                description = "Show new video title on transition",
                                checked = videosTransition,
                                onCheckedChange = {
                                    videosTransition = it
                                    PlayerSettings.setVideosTransition(context, it)
                                }
                            )

                            SettingCheckbox(
                                title = "Lock with sensor",
                                description = "When the orientation is locked, use the sensor to allow the reverse orientation",
                                checked = lockWithSensor,
                                onCheckedChange = {
                                    lockWithSensor = it
                                    PlayerSettings.setLockWithSensor(context, it)
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            SettingClickable(
                                title = "Double tap time delay",
                                value = "$doubleTapDelay seconds",
                                onClick = { showDelayPickerType = "double" }
                            )

                            SettingCheckbox(
                                title = "Double tap to play/pause",
                                description = "Double tap on screen center to play or pause",
                                checked = doubleTapPlayPause,
                                onCheckedChange = {
                                    doubleTapPlayPause = it
                                    PlayerSettings.setDoubleTapPlayPause(context, it)
                                }
                            )

                            SettingClickable(
                                title = "Take a screenshot",
                                value = takeScreenshot,
                                onClick = { showDelayPickerType = "screenshot" }
                            )

                            SettingCheckbox(
                                title = "Enable Fastplay",
                                description = "Tap and hold to increase the playback speed",
                                checked = enableFastplay,
                                onCheckedChange = {
                                    enableFastplay = it
                                    PlayerSettings.setEnableFastplay(context, it)
                                }
                            )

                            SettingSlider(
                                title = "Fastplay speed",
                                description = "${"%.1f".format(fastplaySpeed)}x",
                                value = fastplaySpeed,
                                valueRange = 1.5f..4.0f,
                                steps = 4,
                                onValueChange = {
                                    fastplaySpeed = it
                                    PlayerSettings.setFastplaySpeed(context, it)
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            SettingClickable(
                                title = "Video Decoder Mode",
                                value = if (decoderModeVal == "hardware") "Hardware Decoding (Recommended)" else "Software Decoding",
                                onClick = { showDelayPickerType = "decoder_mode" }
                            )
                        } else if (activeTab == 1) {
                            // Section: Gestures
                            SettingCheckbox(
                                title = "Volume gesture",
                                description = "Control volume by gesture during video playback",
                                checked = volumeGesture,
                                onCheckedChange = {
                                    volumeGesture = it
                                    PlayerSettings.setVolumeGesture(context, it)
                                }
                            )

                            SettingCheckbox(
                                title = "Brightness gesture",
                                description = "Control brightness by gesture during video playback",
                                checked = brightnessGesture,
                                onCheckedChange = {
                                    brightnessGesture = it
                                    PlayerSettings.setBrightnessGesture(context, it)
                                }
                            )

                            SettingCheckbox(
                                title = "Save brightness level",
                                description = "Keep brightness level between media",
                                checked = saveBrightness,
                                onCheckedChange = {
                                    saveBrightness = it
                                    PlayerSettings.setSaveBrightness(context, it)
                                }
                            )

                            SettingCheckbox(
                                title = "Swipe to seek",
                                description = "Swipe your finger across the screen to seek",
                                checked = swipeToSeek,
                                onCheckedChange = {
                                    swipeToSeek = it
                                    PlayerSettings.setSwipeToSeek(context, it)
                                }
                            )

                            SettingCheckbox(
                                title = "Two finger zoom",
                                description = "Zoom in and out with two fingers",
                                checked = twoFingerZoom,
                                onCheckedChange = {
                                    twoFingerZoom = it
                                    PlayerSettings.setTwoFingerZoom(context, it)
                                }
                            )

                            SettingCheckbox(
                                title = "Double tap to seek",
                                description = "Double tap on screen edges to seek by 10 seconds",
                                checked = doubleTapToSeek,
                                onCheckedChange = {
                                    doubleTapToSeek = it
                                    PlayerSettings.setDoubleTapToSeek(context, it)
                                }
                            )
                        } else if (activeTab == 2) {
                            // Section 3: Audio Equalizer Settings
                            var eqEnabled by remember { mutableStateOf(PlayerSettings.getEqualizerEnabled(context)) }
                            var eqValues by remember {
                                mutableStateOf(
                                    listOf(
                                        PlayerSettings.getEqualizerBand(context, 0),
                                        PlayerSettings.getEqualizerBand(context, 1),
                                        PlayerSettings.getEqualizerBand(context, 2),
                                        PlayerSettings.getEqualizerBand(context, 3),
                                        PlayerSettings.getEqualizerBand(context, 4)
                                    )
                                )
                            }

                            SettingCheckbox(
                                title = "Enable Audio Equalizer",
                                description = "Apply custom audio frequency filters during media playback",
                                checked = eqEnabled,
                                onCheckedChange = {
                                    eqEnabled = it
                                    PlayerSettings.setEqualizerEnabled(context, it)
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Acoustic Equalizer Presets",
                                fontSize = 14.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Horizontally Scrollable Preset Selectors
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val presets = listOf(
                                    "Flat" to listOf(0f, 0f, 0f, 0f, 0f),
                                    "Bass Boost" to listOf(10f, 7f, 1f, 0f, -3f),
                                    "Vocal" to listOf(-4f, 1f, 8f, 7f, 2f),
                                    "Rock" to listOf(6f, 3f, -2f, 3f, 7f),
                                    "Jazz" to listOf(4f, 2f, -3f, 2f, 5f),
                                    "Classical" to listOf(4f, 2f, -1f, 3f, 4f)
                                )
                                presets.forEach { (presetName, presetVals) ->
                                    val isCurrentPreset = eqValues.map { "%.1f".format(it) } == presetVals.map { "%.1f".format(it) }
                                    Button(
                                        onClick = {
                                            if (eqEnabled) {
                                                eqValues = presetVals
                                                presetVals.forEachIndexed { idx, valDb ->
                                                    PlayerSettings.setEqualizerBand(context, idx, valDb)
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isCurrentPreset) MaterialTheme.colorScheme.primary else Color(0xFF1E1E22)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(32.dp),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text(presetName, fontSize = 12.sp, color = if (isCurrentPreset) Color.Black else Color.White, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "5-Band Graphic Equalizer",
                                fontSize = 14.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Graphic Equalizer row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0C0C0E), RoundedCornerShape(12.dp))
                                    .padding(vertical = 16.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                val frequencies = listOf("60 Hz", "230 Hz", "910 Hz", "4 kHz", "14 kHz")
                                frequencies.forEachIndexed { index, bandFreqLabel ->
                                    VerticalEqualizerSlider(
                                        value = eqValues[index],
                                        enabled = eqEnabled,
                                        onValueChange = { newVal ->
                                            val updated = eqValues.toMutableList()
                                            updated[index] = newVal
                                            eqValues = updated
                                            PlayerSettings.setEqualizerBand(context, index, newVal)
                                        },
                                        label = bandFreqLabel
                                    )
                                }
                            }
                        } else if (activeTab == 3 && onSortChanged != null) {
                            // Section 4: Display & Sorting Configurations
                            SettingClickable(
                                title = "Display Mode Layout",
                                value = if (displayViewModeVal == "list") "Standard List View" else "Sleek Grid View",
                                onClick = { showDelayPickerType = "view_mode" }
                            )

                            SettingCheckbox(
                                title = "Show Only Favorites",
                                description = "Filter and view only key-marked favorite media assets on the dashboard",
                                checked = showOnlyFavoritesVal,
                                onCheckedChange = {
                                    showOnlyFavoritesVal = it
                                    PlayerSettings.setShowOnlyFavorites(context, it)
                                    onShowOnlyFavoritesChanged?.invoke(it)
                                }
                            )

                            SettingClickable(
                                title = "Default Playback Tap Action",
                                value = when (playbackActionVal) {
                                    "play" -> "Play Selected Video Only"
                                    "play_all" -> "Play Entire List Sequentially"
                                    "add_queue" -> "Append Track to Play Queue"
                                    "insert_next" -> "Insert Track as Next to Play"
                                    else -> "Play Entire List Sequentially"
                                },
                                onClick = { showDelayPickerType = "playback_action" }
                            )

                            SettingClickable(
                                title = "Sort Media Assets By",
                                value = when (sortFieldVal) {
                                    "name" -> "Media Title / Alphabetical"
                                    "file_name" -> "File Name Representation"
                                    "length" -> "Media Duration"
                                    "added_date" -> "Date Added to Device"
                                    "size" -> "File Size"
                                    else -> sortFieldVal
                                },
                                onClick = { showDelayPickerType = "sort_field" }
                            )

                            SettingClickable(
                                title = "Sort Direction Order",
                                value = if (sortOrderVal == "asc") "Ascending" else "Descending",
                                onClick = { showDelayPickerType = "sort_order" }
                            )

                            SettingClickable(
                                title = "Group Media Items By",
                                value = when (groupByVal) {
                                    "none" -> "No Grouping"
                                    "folder" -> "Group by Folder"
                                    "name" -> "Group by Name Initial"
                                    else -> groupByVal
                                },
                                onClick = { showDelayPickerType = "group_by" }
                            )

                            SettingCheckbox(
                                title = "Real-time AI Video Summaries",
                                description = "Identify and cache concise context outlines dynamically using Server-Side Gemini API on video items",
                                checked = aiSummariesVal,
                                onCheckedChange = {
                                    aiSummariesVal = it
                                    PlayerSettings.setAiSummariesEnabled(context, it)
                                    onAiSummariesEnabledChanged?.invoke(it)
                                }
                            )
                        } else if (activeTab == 4 && onAutomatedScan != null) {
                            // Section 5: Library & Scanning Configurations
                            var autoRescanVal by remember { mutableStateOf(PlayerSettings.getAutoRescan(context)) }

                            Text(
                                text = "Media Library",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Media library folders
                            SettingClickable(
                                title = "Media library folders",
                                value = "Select directories to include in the media library",
                                onClick = {
                                    onFolderPickerLaunch?.invoke()
                                    onDismiss()
                                }
                            )

                            // Auto rescan
                            SettingCheckbox(
                                title = "Auto rescan",
                                description = "Automatically scan device for new or deleted media at application startup",
                                checked = autoRescanVal,
                                onCheckedChange = {
                                    autoRescanVal = it
                                    PlayerSettings.setAutoRescan(context, it)
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Manual Scanning Shortcuts",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            SettingClickable(
                                title = "Run Automated Storage Sync",
                                value = "Instantly scan local files on your internal storage",
                                onClick = {
                                    onAutomatedScan.invoke()
                                    onDismiss()
                                }
                            )

                            SettingClickable(
                                title = "Run Deep Local Sweep",
                                value = "Perform a thorough recursive file sweep of selected folders",
                                onClick = {
                                    onDeepScan?.invoke()
                                    onDismiss()
                                }
                            )
                        }
                    }
                }

                Divider(color = Color(0xFF222225))

                // Creator Signature Footer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F0F11))
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "- Created By - Muhammed Bilal C",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Using - Google AI Studio",
                        color = Color(0xFFA8B2C4),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✉ MhdxBilal@proton.me",
                            color = Color.Gray,
                            fontSize = 8.sp
                        )
                        Text(
                            text = "✈ @MHDXBILAL7",
                            color = Color.Gray,
                            fontSize = 8.sp
                        )
                        Text(
                            text = "📷 @mhdxbilal",
                            color = Color.Gray,
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }
    }

    // Modal Delay Check Options
    showDelayPickerType?.let { type ->
        val title = when (type) {
            "fwd" -> "Forward/Backward delay"
            "long" -> "Long Tap delay"
            "double" -> "Double Tap delay"
            "screenshot" -> "Screenshot option"
            "view_mode" -> "Select View Mode"
            "sort_field" -> "Sort Videos By"
            "sort_order" -> "Select Sort Order"
            "group_by" -> "Select Grouping"
            "playback_action" -> "Default Playback Action"
            "decoder_mode" -> "Select Decoder Mode"
            else -> ""
        }
        val currentSelected = when (type) {
            "fwd" -> fwdBwdDelay
            "long" -> longTapDelay
            "double" -> doubleTapDelay
            else -> 0
        }
        val options = when (type) {
            "screenshot" -> listOf("Disabled", "Enabled", "Quick Share")
            "view_mode" -> listOf("list", "grid")
            "sort_field" -> listOf("name", "file_name", "length", "added_date", "size")
            "sort_order" -> listOf("asc", "desc")
            "group_by" -> listOf("none", "folder", "name")
            "playback_action" -> listOf("play", "play_all", "add_queue", "insert_next")
            "decoder_mode" -> listOf("hardware", "software")
            else -> listOf("2", "3", "4", "5", "10", "15", "30")
        }

        val getLabel = { opt: String ->
            when (opt) {
                "list" -> "Standard List View"
                "grid" -> "Sleek Grid View"
                "name" -> "Media Title / Alphabetical"
                "file_name" -> "File Name Representation"
                "length" -> "Media Duration"
                "added_date" -> "Date Added to Device"
                "size" -> "File Size"
                "asc" -> "Ascending"
                "desc" -> "Descending"
                "none" -> "No Grouping"
                "folder" -> "Group by Folder"
                "play" -> "Play Selected Video Only"
                "play_all" -> "Play Entire List Sequentially"
                "add_queue" -> "Append Track to Play Queue"
                "insert_next" -> "Insert Track as Next to Play"
                "hardware" -> "Hardware Decoding (Recommended)"
                "software" -> "Software Decoding"
                else -> opt
            }
        }

        Dialog(onDismissRequest = { showDelayPickerType = null }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF161619),
                modifier = Modifier.width(280.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    options.forEach { opt ->
                        val isSel = when (type) {
                            "screenshot" -> takeScreenshot == opt
                            "view_mode" -> displayViewModeVal == opt
                            "playback_action" -> playbackActionVal == opt
                            "sort_field" -> sortFieldVal == opt
                            "sort_order" -> sortOrderVal == opt
                            "group_by" -> groupByVal == opt
                            "decoder_mode" -> decoderModeVal == opt
                            else -> currentSelected == opt.toInt()
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (type) {
                                        "screenshot" -> {
                                            takeScreenshot = opt
                                            PlayerSettings.setTakeScreenshot(context, opt)
                                        }
                                        "view_mode" -> {
                                            displayViewModeVal = opt
                                            PlayerSettings.setViewMode(context, opt)
                                            onViewModeChanged?.invoke(opt)
                                        }
                                        "playback_action" -> {
                                            playbackActionVal = opt
                                            PlayerSettings.setPlaybackAction(context, opt)
                                            onPlaybackActionChanged?.invoke(opt)
                                        }
                                        "sort_field" -> {
                                            sortFieldVal = opt
                                            PlayerSettings.setSortField(context, opt)
                                            onSortChanged?.invoke(opt, sortOrderVal)
                                        }
                                        "sort_order" -> {
                                            sortOrderVal = opt
                                            PlayerSettings.setSortOrder(context, opt)
                                            onSortChanged?.invoke(sortFieldVal, opt)
                                        }
                                        "group_by" -> {
                                            groupByVal = opt
                                            PlayerSettings.setGroupBySetting(context, opt)
                                            onGroupByChanged?.invoke(opt)
                                        }
                                        "decoder_mode" -> {
                                            decoderModeVal = opt
                                            PlayerSettings.setDecoderMode(context, opt)
                                        }
                                        else -> {
                                            val valInt = opt.toInt()
                                            when (type) {
                                                "fwd" -> {
                                                    fwdBwdDelay = valInt
                                                    PlayerSettings.setFwdBwdDelay(context, valInt)
                                                }
                                                "long" -> {
                                                    longTapDelay = valInt
                                                    PlayerSettings.setLongTapDelay(context, valInt)
                                                }
                                                "double" -> {
                                                    doubleTapDelay = valInt
                                                    PlayerSettings.setDoubleTapDelay(context, valInt)
                                                }
                                            }
                                        }
                                    }
                                    showDelayPickerType = null
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSel,
                                onClick = {
                                    when (type) {
                                        "screenshot" -> {
                                            takeScreenshot = opt
                                            PlayerSettings.setTakeScreenshot(context, opt)
                                        }
                                        "view_mode" -> {
                                            displayViewModeVal = opt
                                            PlayerSettings.setViewMode(context, opt)
                                            onViewModeChanged?.invoke(opt)
                                        }
                                        "playback_action" -> {
                                            playbackActionVal = opt
                                            PlayerSettings.setPlaybackAction(context, opt)
                                            onPlaybackActionChanged?.invoke(opt)
                                        }
                                        "sort_field" -> {
                                            sortFieldVal = opt
                                            PlayerSettings.setSortField(context, opt)
                                            onSortChanged?.invoke(opt, sortOrderVal)
                                        }
                                        "sort_order" -> {
                                            sortOrderVal = opt
                                            PlayerSettings.setSortOrder(context, opt)
                                            onSortChanged?.invoke(sortFieldVal, opt)
                                        }
                                        "group_by" -> {
                                            groupByVal = opt
                                            PlayerSettings.setGroupBySetting(context, opt)
                                            onGroupByChanged?.invoke(opt)
                                        }
                                        "decoder_mode" -> {
                                            decoderModeVal = opt
                                            PlayerSettings.setDecoderMode(context, opt)
                                        }
                                        else -> {
                                            val valInt = opt.toInt()
                                            when (type) {
                                                "fwd" -> {
                                                    fwdBwdDelay = valInt
                                                    PlayerSettings.setFwdBwdDelay(context, valInt)
                                                }
                                                "long" -> {
                                                    longTapDelay = valInt
                                                    PlayerSettings.setLongTapDelay(context, valInt)
                                                }
                                                "double" -> {
                                                    doubleTapDelay = valInt
                                                    PlayerSettings.setDoubleTapDelay(context, valInt)
                                                }
                                            }
                                        }
                                    }
                                    showDelayPickerType = null
                                }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = if (type == "screenshot" || type == "view_mode" || type == "playback_action" || type == "sort_field" || type == "sort_order" || type == "group_by" || type == "decoder_mode") getLabel(opt) else "$opt seconds", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun SettingCheckbox(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = description, color = Color.Gray, fontSize = 12.sp)
        }
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = Color.LightGray
            )
        )
    }
}

@Composable
fun SettingClickable(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
    }
}

@Composable
fun SettingSlider(
    title: String,
    description: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = description, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun VerticalEqualizerSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val density = LocalDensity.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 2.dp)
    ) {
        Text(
            text = "${if (value >= 0f) "+" else ""}${"%.1f".format(value)}",
            fontSize = 10.sp,
            color = if (enabled) MaterialTheme.colorScheme.primary else Color.Gray,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(42.dp)
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        BoxWithConstraints(
            modifier = Modifier
                .width(36.dp)
                .height(130.dp)
                .background(Color(0xFF141416), RoundedCornerShape(18.dp))
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            val maxHeightPx = constraints.maxHeight.toFloat()
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF222225), RoundedCornerShape(2.dp))
            )
            
            val normalizedVal = ((value + 15f) / 30f).coerceIn(0f, 1f)
            val thumbY = (1.0f - normalizedVal) * maxHeightPx
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(enabled) {
                        if (!enabled) return@pointerInput
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newY = (change.position.y).coerceIn(0f, maxHeightPx)
                            val newNormalized = 1.0f - (newY / maxHeightPx)
                            val newValue = -15f + (newNormalized * 30f)
                            onValueChange(newValue.coerceIn(-15f, 15f))
                        }
                    }
            ) {
                val centerY = maxHeightPx / 2f
                val activeTrackTop = minOf(centerY, thumbY)
                val activeTrackBottom = maxOf(centerY, thumbY)
                val activeTrackHeightDp = with(density) { (activeTrackBottom - activeTrackTop).toDp() }
                val activeTrackTopDp = with(density) { activeTrackTop.toDp() }
                
                if (activeTrackHeightDp > 0.dp) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = activeTrackTopDp)
                            .width(4.dp)
                            .height(activeTrackHeightDp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                    )
                }

                val thumbTopDp = with(density) { thumbY.toDp() }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = (thumbTopDp - 10.dp).coerceAtLeast(0.dp))
                        .size(20.dp)
                        .background(
                            color = if (enabled) MaterialTheme.colorScheme.primary else Color.Gray,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color.Black, RoundedCornerShape(3.dp))
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (enabled) Color.White else Color.Gray,
            fontWeight = FontWeight.Bold
        )
    }
}

