package com.example.ui.main

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.VideoEntity
import com.example.ui.viewmodel.ScanState
import com.example.ui.viewmodel.VideoPlayerViewModel
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.BottomSheetDefaults
import com.example.ui.components.PlayerSettingsDialog
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.data.settings.PlayerSettings
import android.widget.Toast
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.BorderStroke

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MediaDashboardScreen(
    viewModel: VideoPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allVideos by viewModel.allVideos.collectAsStateWithLifecycle()
    val recentVideos by viewModel.recentVideos.collectAsStateWithLifecycle()
    val favoriteVideos by viewModel.favoriteVideos.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("All Videos", "Folders", "Recents", "Favorites")
    var showDashboardSettings by remember { mutableStateOf(false) }

    // Loading layout/sort preferences
    var displayViewMode by remember { mutableStateOf(PlayerSettings.getViewMode(context)) }
    var showOnlyFavoritesSetting by remember { mutableStateOf(PlayerSettings.getShowOnlyFavorites(context)) }
    var playbackActionSetting by remember { mutableStateOf(PlayerSettings.getPlaybackAction(context)) }
    var sortFieldSetting by remember { mutableStateOf(PlayerSettings.getSortField(context)) }
    var sortOrderSetting by remember { mutableStateOf(PlayerSettings.getSortOrder(context)) }
    var groupBySetting by remember { mutableStateOf(PlayerSettings.getGroupBySetting(context)) }
    var aiSummariesEnabled by remember { mutableStateOf(PlayerSettings.getAiSummariesEnabled(context)) }
    var quickPlayVideo by remember { mutableStateOf<VideoEntity?>(null) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    // Determine target storage API permissions
    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_VIDEO
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            viewModel.scanLocalVideos()
        }
    }

    // Document manual picker
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.handlePickedVideoUri(context, it)
        }
    }

    // Folder tree picker
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            viewModel.handlePickedFolderUri(context, it)
        }
    }

    // Active tab filtered source
    val rawList = when (selectedTab) {
        0 -> allVideos
        1 -> emptyList()
        2 -> recentVideos
        3 -> favoriteVideos
        else -> allVideos
    }

    // Filter by Show Only Favorites
    val favoritesFiltered = remember(rawList, showOnlyFavoritesSetting) {
        if (showOnlyFavoritesSetting) {
            rawList.filter { it.isFavorite }
        } else {
            rawList
        }
    }

    // Filter by search query
    val searchedList = remember(favoritesFiltered, searchQuery) {
        if (searchQuery.isBlank()) {
            favoritesFiltered
        } else {
            favoritesFiltered.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
    }

    // Sort according to user selection
    val filteredVideos = remember(searchedList, sortFieldSetting, sortOrderSetting) {
        val sorted = when (sortFieldSetting) {
            "name", "file_name" -> {
                if (sortOrderSetting == "asc") searchedList.sortedBy { it.title.lowercase() }
                else searchedList.sortedByDescending { it.title.lowercase() }
            }
            "length" -> {
                if (sortOrderSetting == "asc") searchedList.sortedBy { it.duration }
                else searchedList.sortedByDescending { it.duration }
            }
            "added_date" -> {
                if (sortOrderSetting == "asc") searchedList.sortedBy { it.addedDate }
                else searchedList.sortedByDescending { it.addedDate }
            }
            "size" -> {
                if (sortOrderSetting == "asc") searchedList.sortedBy { it.size }
                else searchedList.sortedByDescending { it.size }
            }
            else -> searchedList
        }
        sorted
    }

    val groupedVideos = remember(filteredVideos, groupBySetting) {
        when (groupBySetting) {
            "folder" -> filteredVideos.groupBy { it.folderName }
            "name" -> filteredVideos.groupBy { it.title.firstOrNull()?.uppercaseChar()?.toString() ?: "#" }
            else -> emptyMap()
        }
    }

    var selectedVideoForInfo by remember { mutableStateOf<VideoEntity?>(null) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(Color.Black)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Search Button (🔍)
                        IconButton(
                            onClick = {
                                isSearchExpanded = !isSearchExpanded
                                if (!isSearchExpanded) {
                                    viewModel.updateSearchQuery("")
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isSearchExpanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Toggle search panel",
                                tint = if (isSearchExpanded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        // App-level Settings Gear Button (⚙️) - launches the categorized settings dialog
                        IconButton(
                            onClick = {
                                showDashboardSettings = true
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configure player gestures, display, sorting, and scanning",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                if (showDashboardSettings) {
                    PlayerSettingsDialog(
                        displayViewMode = displayViewMode,
                        onViewModeChanged = { displayViewMode = it },
                        showOnlyFavoritesSetting = showOnlyFavoritesSetting,
                        onShowOnlyFavoritesChanged = { showOnlyFavoritesSetting = it },
                        playbackActionSetting = playbackActionSetting,
                        onPlaybackActionChanged = { playbackActionSetting = it },
                        sortFieldSetting = sortFieldSetting,
                        sortOrderSetting = sortOrderSetting,
                        onSortChanged = { field, order ->
                            sortFieldSetting = field
                            sortOrderSetting = order
                        },
                        groupBySetting = groupBySetting,
                        onGroupByChanged = { groupBySetting = it },
                        aiSummariesEnabled = aiSummariesEnabled,
                        onAiSummariesEnabledChanged = { aiSummariesEnabled = it },
                        onFolderPickerLaunch = { folderPickerLauncher.launch(null) },
                        onDocumentPickerLaunch = { documentPickerLauncher.launch(arrayOf("video/*")) },
                        onDeepScan = {
                            if (hasPermission) {
                                viewModel.scanDeepLocalVideos()
                            } else {
                                permissionLauncher.launch(storagePermission)
                            }
                        },
                        onAutomatedScan = {
                            if (hasPermission) {
                                viewModel.scanLocalVideos()
                            } else {
                                permissionLauncher.launch(storagePermission)
                            }
                        },
                        onDismiss = { showDashboardSettings = false }
                    )
                }

                // Filtering Search Bar (collapsible under 🔍 toggle)
                AnimatedVisibility(
                    visible = isSearchExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Search video list...", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear text")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("video_search_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF141416),
                            unfocusedContainerColor = Color(0xFF141416),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                }

                // Layout tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Black,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            },
                            modifier = Modifier.testTag("dashboard_tab_$index")
                        )
                    }
                }
            }
        },
        containerColor = Color.Black,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Scanning HUD indicator
                AnimatedVisibility(
                    visible = scanState == ScanState.SCANNING,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Scanning device storage for multimedia tracks...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (selectedTab == 1) {
                    val folders by viewModel.folders.collectAsStateWithLifecycle()
                    if (folders.isEmpty()) {
                        EmptyFoldersStateLayout(
                            onImportFolder = { folderPickerLauncher.launch(null) }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(folders, key = { it.name }) { folder ->
                                FolderPlaylistItemRow(
                                    folder = folder,
                                    viewModel = viewModel,
                                    onPlayFolder = {
                                        viewModel.playFolderPlaylist(folder)
                                    },
                                    onQueueFolder = {
                                        viewModel.queueFolderPlaylist(folder)
                                    },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                } else {
                    // If no videos list
                    if (filteredVideos.isEmpty()) {
                        EmptyStateLayout(
                            selectedTab = selectedTab,
                            searchQuery = searchQuery,
                            hasPermission = hasPermission,
                            onRequestPermission = { permissionLauncher.launch(storagePermission) },
                            onPickDocument = { documentPickerLauncher.launch(arrayOf("video/*")) }
                        )
                    } else {
                        val onItemClick = { video: VideoEntity ->
                            when (playbackActionSetting) {
                                "play" -> {
                                    viewModel.selectVideo(video, listOf(video))
                                }
                                "play_all" -> {
                                    viewModel.selectVideo(video, filteredVideos)
                                }
                                "add_queue" -> {
                                    viewModel.addToPlayQueue(video)
                                    Toast.makeText(context, "Added to play queue: ${video.title}", Toast.LENGTH_SHORT).show()
                                }
                                "insert_next" -> {
                                    viewModel.insertNext(video)
                                    Toast.makeText(context, "Inserted as next track: ${video.title}", Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    viewModel.selectVideo(video, filteredVideos)
                                }
                            }
                        }

                        if (groupBySetting != "none" && groupedVideos.isNotEmpty()) {
                            // Render beautifully Grouped layout
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                groupedVideos.forEach { (groupKey, groupVideos) ->
                                    item(key = "header_${groupKey}_${selectedTab}") {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (groupBySetting == "folder") Icons.Default.Folder else Icons.Default.SortByAlpha,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = groupKey,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                                Surface(
                                                    color = Color.Black,
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text(
                                                        text = "${groupVideos.size} items",
                                                        color = Color.Gray,
                                                        fontSize = 11.sp,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (displayViewMode == "grid") {
                                        val chunked = groupVideos.chunked(2)
                                        items(chunked) { chunk ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                chunk.forEach { video ->
                                                    VideoItemGrid(
                                                        video = video,
                                                        onClick = { onItemClick(video) },
                                                        onLongClick = { quickPlayVideo = video },
                                                        onFavoriteToggle = { viewModel.toggleFavorite(video) },
                                                        onInfoRequested = { selectedVideoForInfo = video },
                                                        aiSummariesEnabled = aiSummariesEnabled,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                if (chunk.size == 1) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    } else {
                                        items(groupVideos, key = { it.uriString }) { video ->
                                            VideoItemRow(
                                                video = video,
                                                onClick = { onItemClick(video) },
                                                onLongClick = { quickPlayVideo = video },
                                                onFavoriteToggle = { viewModel.toggleFavorite(video) },
                                                onInfoRequested = { selectedVideoForInfo = video },
                                                aiSummariesEnabled = aiSummariesEnabled
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Standard flat list layout (groupBySetting == "none")
                            if (displayViewMode == "grid") {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(filteredVideos, key = { it.uriString }) { video ->
                                        VideoItemGrid(
                                            video = video,
                                            onClick = { onItemClick(video) },
                                            onLongClick = { quickPlayVideo = video },
                                            onFavoriteToggle = { viewModel.toggleFavorite(video) },
                                            onInfoRequested = { selectedVideoForInfo = video },
                                            aiSummariesEnabled = aiSummariesEnabled,
                                            modifier = Modifier.animateItem()
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filteredVideos, key = { it.uriString }) { video ->
                                        VideoItemRow(
                                            video = video,
                                            onClick = { onItemClick(video) },
                                            onLongClick = { quickPlayVideo = video },
                                            onFavoriteToggle = { viewModel.toggleFavorite(video) },
                                            onInfoRequested = { selectedVideoForInfo = video },
                                            aiSummariesEnabled = aiSummariesEnabled,
                                            modifier = Modifier.animateItem()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal details description bottom dialog sheet
    if (selectedVideoForInfo != null) {
        VideoDetailSheet(
            video = selectedVideoForInfo!!,
            onDismiss = { selectedVideoForInfo = null },
            onDeleteSelected = {
                viewModel.deleteVideo(it.uriString)
                selectedVideoForInfo = null
            }
        )
    }

    // Modern Quick Play pop-up dialog
    if (quickPlayVideo != null) {
        QuickPlayPopupDialog(
            video = quickPlayVideo!!,
            onFullScreen = {
                val targetVideo = quickPlayVideo!!
                quickPlayVideo = null
                viewModel.selectVideo(targetVideo, filteredVideos)
            },
            onDismiss = { quickPlayVideo = null }
        )
    }


}

@Composable
fun EmptyStateLayout(
    selectedTab: Int,
    searchQuery: String,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onPickDocument: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = Color(0xFF141416),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val icon = when {
                        searchQuery.isNotEmpty() -> Icons.Default.SearchOff
                        selectedTab == 1 -> Icons.Outlined.History
                        selectedTab == 2 -> Icons.Outlined.FavoriteBorder
                        else -> Icons.Outlined.VideoLibrary
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val title = when {
                searchQuery.isNotEmpty() -> "No matching results"
                selectedTab == 1 -> "No playback history"
                selectedTab == 2 -> "No favorites added yet"
                !hasPermission -> "Storage permission required"
                else -> "No videos scanned"
            }

            val desc = when {
                searchQuery.isNotEmpty() -> "We couldn't check any videos representing \"$searchQuery\"."
                selectedTab == 1 -> "Play some video files, we'll track resume logs inside Recents."
                selectedTab == 2 -> "Tap the heart indicator on any item to save your shortcuts."
                !hasPermission -> "Permit scanning so MhdxBilal can list video indexes automatically."
                else -> "Select specific folders or run an automated Storage scan."
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (!hasPermission && searchQuery.isEmpty() && selectedTab == 0) {
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enable Access")
                }
            } else if (searchQuery.isEmpty()) {
                Button(
                    onClick = onPickDocument,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F1F24))
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = Color.LightGray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pick File Manually", color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoItemRow(
    video: VideoEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onInfoRequested: () -> Unit,
    aiSummariesEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val extension = remember(video.title) {
        val dotIndex = video.title.lastIndexOf('.')
        if (dotIndex != -1 && dotIndex < video.title.length - 1) {
            val ext = video.title.substring(dotIndex + 1).lowercase()
            if (ext == "mp4" || ext == "mkv" || ext == "avi" || ext == "webm" || ext == "mov" || ext == "3gp") {
                ext.uppercase()
            } else {
                "VIDEO"
            }
        } else {
            "MP4"
        }
    }

    var summaryState by remember { mutableStateOf("") }
    var isSummaryLoading by remember { mutableStateOf(false) }

    if (aiSummariesEnabled) {
        val context = LocalContext.current
        LaunchedEffect(video.title) {
            val cachedValue = com.example.data.gemini.VideoSummaryCache.getSummary(context, video.title)
            if (cachedValue != null) {
                summaryState = cachedValue
            } else {
                isSummaryLoading = true
                summaryState = "⚡ AI summarizing..."
                try {
                    summaryState = com.example.data.gemini.GeminiService.generateVideoAutoSummary(context, video.title)
                } catch (e: Exception) {
                    summaryState = "🎬 Play to view details"
                } finally {
                    isSummaryLoading = false
                }
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101012)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("video_row_card_${video.title}")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vertical video template layout indicator
                Surface(
                    color = Color(0xFF1C1C1F),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(width = 64.dp, height = 48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        // Top start: format badge pill
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.dp),
                            contentAlignment = Alignment.TopStart
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(3.dp)
                            ) {
                                Text(
                                    text = extension,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                        }

                        // Bottom end: duration badge
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.dp),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(3.dp)
                            ) {
                                Text(
                                    text = video.durationFormatted,
                                    fontSize = 8.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = video.sizeFormatted,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 11.sp
                        )

                        if (video.path != null) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Text(
                                text = "Local File",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    if (aiSummariesEnabled && summaryState.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Summary",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = summaryState,
                                color = Color(0xFFA8B2C4),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Favorite switch button
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite status toggle",
                        tint = if (video.isFavorite) Color.Red else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onInfoRequested) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Details",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Beautiful Resume progress line if video was played recently
            if (video.lastPlayedPosition > 0 && video.duration > 0) {
                val progressFraction = (video.lastPlayedPosition.toFloat() / video.duration.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color(0xFF1F1F1F))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailSheet(
    video: VideoEntity,
    onDismiss: () -> Unit,
    onDeleteSelected: (VideoEntity) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101012),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.DarkGray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Multimedia Properties",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            DetailRow(label = "Filename", value = video.title)
            DetailRow(label = "Length", value = video.durationFormatted)
            DetailRow(label = "File size", value = video.sizeFormatted)
            DetailRow(label = "Last Saved Playback Pos", value = if (video.lastPlayedPosition > 0L) formatTime(video.lastPlayedPosition) else "Unplayed / New Start")
            DetailRow(label = "Storage Scheme", value = if (video.path != null) "MediaStore Device" else "Scoped Document URI")
            if (video.path != null) {
                DetailRow(label = "Absolute Path", value = video.path)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onDeleteSelected(video) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("delete_source_btn")
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete from Index")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = Color.White, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun FolderPlaylistItemRow(
    folder: com.example.ui.viewmodel.FolderPlaylist,
    viewModel: VideoPlayerViewModel,
    onPlayFolder: () -> Unit,
    onQueueFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101012)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${folder.videos.size} videos",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                IconButton(onClick = onPlayFolder) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Folder",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onQueueFolder) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = "Queue Folder",
                        tint = Color.LightGray
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(bottom = 12.dp))
                    folder.videos.forEach { video ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.selectVideo(video, folder.videos)
                                }
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = video.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = video.durationFormatted,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyFoldersStateLayout(
    onImportFolder: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = Color(0xFF141416),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No folders identified",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Run an automated check or tap below to select and import any device folder as a playlist.",
                style = androidx.compose.ui.text.TextStyle(
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onImportFolder,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Folder Playlist")
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoItemGrid(
    video: VideoEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onInfoRequested: () -> Unit,
    aiSummariesEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val extension = remember(video.title) {
        val dotIndex = video.title.lastIndexOf('.')
        if (dotIndex != -1 && dotIndex < video.title.length - 1) {
            val ext = video.title.substring(dotIndex + 1).lowercase()
            if (ext == "mp4" || ext == "mkv" || ext == "avi" || ext == "webm" || ext == "mov" || ext == "3gp") {
                ext.uppercase()
            } else {
                "VIDEO"
            }
        } else {
            "MP4"
        }
    }

    var summaryState by remember { mutableStateOf("") }
    var isSummaryLoading by remember { mutableStateOf(false) }

    if (aiSummariesEnabled) {
        val context = LocalContext.current
        LaunchedEffect(video.title) {
            val cachedValue = com.example.data.gemini.VideoSummaryCache.getSummary(context, video.title)
            if (cachedValue != null) {
                summaryState = cachedValue
            } else {
                isSummaryLoading = true
                summaryState = "⚡ AI summarizing..."
                try {
                    summaryState = com.example.data.gemini.GeminiService.generateVideoAutoSummary(context, video.title)
                } catch (e: Exception) {
                    summaryState = "🎬 Play to view details"
                } finally {
                    isSummaryLoading = false
                }
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101012)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("video_grid_card_${video.title}")
    ) {
        Column {
            // Video Thumbnail template area
            Surface(
                color = Color(0xFF1C1C1F),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    
                    // Top start: format badge pill
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = extension,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Bottom end: duration badge
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = video.durationFormatted,
                                fontSize = 9.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Title and description row area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.heightIn(min = 36.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = video.sizeFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontSize = 11.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onFavoriteToggle,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite status toggle",
                                tint = if (video.isFavorite) Color.Red else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = onInfoRequested,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Details",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                if (aiSummariesEnabled && summaryState.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Summary",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = summaryState,
                            color = Color(0xFFA8B2C4),
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Progress bar
            if (video.lastPlayedPosition > 0 && video.duration > 0) {
                val progressFraction = (video.lastPlayedPosition.toFloat() / video.duration.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color(0xFF1F1F1F))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplaySettingsBottomSheet(
    displayViewMode: String,
    showOnlyFavorites: Boolean,
    playbackAction: String,
    sortField: String,
    sortOrder: String,
    groupBy: String,
    aiSummariesEnabled: Boolean,
    onViewModeChanged: (String) -> Unit,
    onShowOnlyFavoritesChanged: (Boolean) -> Unit,
    onPlaybackActionChanged: (String) -> Unit,
    onSortChanged: (String, String) -> Unit,
    onGroupByChanged: (String) -> Unit,
    onAiSummariesEnabledChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161618),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.DarkGray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // Section 1: Display settings
            Text(
                text = "Display settings",
                color = MaterialTheme.colorScheme.primary, // Orange/Accent
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Display in list / grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onViewModeChanged(if (displayViewMode == "list") "grid" else "list")
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (displayViewMode == "list") Icons.Default.List else Icons.Default.GridView,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = if (displayViewMode == "list") "Display in list" else "Display in grid",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Show only favourites
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onShowOnlyFavoritesChanged(!showOnlyFavorites)
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (showOnlyFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (showOnlyFavorites) Color.Red else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Show only favourites",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Checkbox(
                    checked = showOnlyFavorites,
                    onCheckedChange = { onShowOnlyFavoritesChanged(it) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = Color.LightGray
                    )
                )
            }

            // Playback action
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dropdownExpanded = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Playback action",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Videos",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val actionLabel = when (playbackAction) {
                            "play" -> "Play"
                            "play_all" -> "Play all"
                            "add_queue" -> "Add to play queue"
                            "insert_next" -> "Insert next"
                            else -> "Play all"
                        }
                        Text(
                            text = actionLabel,
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.background(Color(0xFF242426))
                ) {
                    val actions = listOf(
                        "play" to "Play",
                        "play_all" to "Play all",
                        "add_queue" to "Add to play queue",
                        "insert_next" to "Insert next"
                    )
                    actions.forEach { (actionKey, labelStr) ->
                        DropdownMenuItem(
                            text = { Text(labelStr, color = Color.White) },
                            onClick = {
                                onPlaybackActionChanged(actionKey)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(16.dp))

            // Section: Preferences
            Text(
                text = "Preferences",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Switch: AI thumbnail summaries
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAiSummariesEnabledChanged(!aiSummariesEnabled) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "AI Thumbnail summaries",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Generate capsular summaries automatically",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Switch(
                    checked = aiSummariesEnabled,
                    onCheckedChange = { onAiSummariesEnabledChanged(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                )
            }

            // Grouping settings list dropdown
            var groupDropdownExpanded by remember { mutableStateOf(false) }
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { groupDropdownExpanded = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GroupWork,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Group videos",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Organize library structure",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val groupLabel = when (groupBy) {
                            "none" -> "Don't Group"
                            "folder" -> "By Folder"
                            "name" -> "By Name (A-Z)"
                            else -> "Don't Group"
                        }
                        Text(
                            text = groupLabel,
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                }

                DropdownMenu(
                    expanded = groupDropdownExpanded,
                    onDismissRequest = { groupDropdownExpanded = false },
                    modifier = Modifier.background(Color(0xFF242426))
                ) {
                    val groupOptions = listOf(
                        "none" to "Don't Group",
                        "folder" to "By Folder",
                        "name" to "By Name (A-Z)"
                    )
                    groupOptions.forEach { (optionKey, labelStr) ->
                        DropdownMenuItem(
                            text = { Text(labelStr, color = Color.White) },
                            onClick = {
                                onGroupByChanged(optionKey)
                                groupDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(16.dp))

            // Section 2: Sort by...
            Text(
                text = "Sort by...",
                color = MaterialTheme.colorScheme.primary, // Orange/Accent
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Row helper
            SortOptionItem(
                icon = Icons.Default.SortByAlpha,
                title = "Name",
                fieldKey = "name",
                sortField = sortField,
                sortOrder = sortOrder,
                options = listOf(
                    "asc" to "A -> Z",
                    "desc" to "Z -> A"
                ),
                onSortChanged = onSortChanged
            )

            Divider(color = Color.White.copy(alpha = 0.05f))

            SortOptionItem(
                icon = Icons.Default.Description,
                title = "File name",
                fieldKey = "file_name",
                sortField = sortField,
                sortOrder = sortOrder,
                options = listOf(
                    "asc" to "A -> Z",
                    "desc" to "Z -> A"
                ),
                onSortChanged = onSortChanged
            )

            Divider(color = Color.White.copy(alpha = 0.05f))

            SortOptionItem(
                icon = Icons.Default.Timer,
                title = "Length",
                fieldKey = "length",
                sortField = sortField,
                sortOrder = sortOrder,
                options = listOf(
                    "asc" to "Shortest first",
                    "desc" to "Longest first"
                ),
                onSortChanged = onSortChanged
            )

            Divider(color = Color.White.copy(alpha = 0.05f))

            SortOptionItem(
                icon = Icons.Default.CalendarToday,
                title = "Insertion date",
                fieldKey = "added_date",
                sortField = sortField,
                sortOrder = sortOrder,
                options = listOf(
                    "asc" to "Oldest first",
                    "desc" to "Newest first"
                ),
                onSortChanged = onSortChanged
            )

            Divider(color = Color.White.copy(alpha = 0.05f))

            SortOptionItem(
                icon = Icons.Default.SdStorage,
                title = "Size",
                fieldKey = "size",
                sortField = sortField,
                sortOrder = sortOrder,
                options = listOf(
                    "asc" to "Smallest first",
                    "desc" to "Largest first"
                ),
                onSortChanged = onSortChanged
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun QuickPlayPopupDialog(
    video: VideoEntity,
    onFullScreen: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(true) }
    var isLooping by remember { mutableStateOf(true) }
    var playSpeed by remember { mutableStateOf(1.0f) }

    // local ExoPlayer instance for quick view with mute enabled by default
    val exoPlayer = remember {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
            val mediaItem = androidx.media3.common.MediaItem.fromUri(video.uriString)
            setMediaItem(mediaItem)
            prepare()
            volume = 0f
            playWhenReady = true
        }
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(isLooping) {
        exoPlayer.repeatMode = if (isLooping) {
            androidx.media3.common.Player.REPEAT_MODE_ALL
        } else {
            androidx.media3.common.Player.REPEAT_MODE_OFF
        }
    }

    LaunchedEffect(playSpeed) {
        exoPlayer.setPlaybackSpeed(playSpeed)
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) exoPlayer.play() else exoPlayer.pause()
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Quick Play Preview",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        androidx.compose.ui.viewinterop.AndroidView(
                            factory = { ctx ->
                                androidx.media3.ui.PlayerView(ctx).apply {
                                    player = exoPlayer
                                    useController = false
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Muted status watermark badge
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isMuted) "MUTED PREVIEW" else "SOUND ON",
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = video.title,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${video.sizeFormatted} • ${video.durationFormatted}",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Control action bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { isPlaying = !isPlaying }) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                            contentDescription = "Toggle play-pause state",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(onClick = { isMuted = !isMuted }) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Mute audio",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = { isLooping = !isLooping }) {
                        Icon(
                            imageVector = Icons.Default.Loop,
                            contentDescription = "Loop switcher",
                            tint = if (isLooping) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Play speed selector
                    IconButton(onClick = {
                        playSpeed = if (playSpeed == 1.0f) 1.5f else if (playSpeed == 1.5f) 2.0f else 1.0f
                    }) {
                        Surface(
                            color = Color(0xFF1E1E22),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${playSpeed}x",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onFullScreen,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Watch in Full Player", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SortOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    fieldKey: String,
    sortField: String,
    sortOrder: String,
    options: List<Pair<String, String>>,
    onSortChanged: (String, String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.width(140.dp)
        ) {
            options.forEach { (orderKey, labelStr) ->
                val isSelected = (sortField == fieldKey && sortOrder == orderKey)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSortChanged(fieldKey, orderKey) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = labelStr,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(24.dp))
                    }
                }
            }
        }
    }
}
