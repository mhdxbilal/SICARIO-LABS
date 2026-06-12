package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.VideoEntity
import com.example.data.database.AudioEntity
import com.example.data.database.VideoRepository
import com.example.data.scanner.MediaStoreVideoScanner
import com.example.data.scanner.MediaStoreAudioScanner
import com.example.data.scanner.RecursiveDirectoryScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ScanState {
    IDLE, SCANNING, SUCCESS, ERROR
}

data class FolderPlaylist(
    val name: String,
    val videos: List<VideoEntity>
)

class VideoPlayerViewModel(
    private val repository: VideoRepository,
    private val scanner: MediaStoreVideoScanner,
    private val deepScanner: RecursiveDirectoryScanner,
    private val audioScanner: MediaStoreAudioScanner
) : ViewModel() {

    // Lists from Room (Videos)
    val allVideos: StateFlow<List<VideoEntity>> = repository.allVideos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val folders: StateFlow<List<FolderPlaylist>> = allVideos
        .map { list ->
            list.groupBy { it.folderName }
                .map { (name, videosList) -> FolderPlaylist(name, videosList) }
                .sortedBy { it.name }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentVideos: StateFlow<List<VideoEntity>> = repository.recentVideos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoriteVideos: StateFlow<List<VideoEntity>> = repository.favoriteVideos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Lists from Room (Audio / Songs)
    val allAudios: StateFlow<List<AudioEntity>> = repository.allAudios
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentAudios: StateFlow<List<AudioEntity>> = repository.recentAudios
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoriteAudios: StateFlow<List<AudioEntity>> = repository.favoriteAudios
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Scanning Status
    private val _scanState = MutableStateFlow(ScanState.IDLE)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _scannedCount = MutableStateFlow(0)
    val scannedCount: StateFlow<Int> = _scannedCount.asStateFlow()

    // Filter Query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Active playback selection
    private val _playingVideo = MutableStateFlow<VideoEntity?>(null)
    val playingVideo: StateFlow<VideoEntity?> = _playingVideo.asStateFlow()

    // Active queue for navigation (next/prev)
    private val _playbackQueue = MutableStateFlow<List<VideoEntity>>(emptyList())
    val playbackQueue: StateFlow<List<VideoEntity>> = _playbackQueue.asStateFlow()

    // Active AUDIO playback selection
    private val _playingAudio = MutableStateFlow<AudioEntity?>(null)
    val playingAudio: StateFlow<AudioEntity?> = _playingAudio.asStateFlow()

    private val _audioPlaybackQueue = MutableStateFlow<List<AudioEntity>>(emptyList())
    val audioPlaybackQueue: StateFlow<List<AudioEntity>> = _audioPlaybackQueue.asStateFlow()

    init {
        // Ready
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun scanLocalVideos() {
        _scanState.value = ScanState.SCANNING
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val videos = scanner.scanVideosOnDevice()
                repository.insertVideos(videos)
                
                val audios = audioScanner.scanAudioOnDevice()
                repository.insertAudios(audios)
                
                _scannedCount.value = videos.size + audios.size
                _scanState.value = ScanState.SUCCESS
            } catch (e: Exception) {
                e.printStackTrace()
                _scanState.value = ScanState.ERROR
            }
        }
    }

    fun scanDeepLocalVideos() {
        _scanState.value = ScanState.SCANNING
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val videos = deepScanner.scanVideosFromLocal()
                repository.insertVideos(videos)
                _scannedCount.value = videos.size
                _scanState.value = ScanState.SUCCESS
            } catch (e: Exception) {
                e.printStackTrace()
                _scanState.value = ScanState.ERROR
            }
        }
    }

    fun selectVideo(video: VideoEntity, queueList: List<VideoEntity> = emptyList()) {
        _playingVideo.value = video
        if (queueList.isNotEmpty()) {
            _playbackQueue.value = queueList
        } else {
            _playbackQueue.value = listOf(video)
        }
        
        // Save to Recent list immediately or on play start
        saveResumePosition(video.uriString, video.lastPlayedPosition)
    }

    fun addToPlayQueue(video: VideoEntity) {
        val current = _playbackQueue.value.toMutableList()
        if (!current.any { it.uriString == video.uriString }) {
            current.add(video)
            _playbackQueue.value = current
        }
    }

    fun insertNext(video: VideoEntity) {
        val current = _playbackQueue.value.toMutableList()
        val playing = _playingVideo.value
        if (playing != null) {
            // Find its index
            val index = current.indexOfFirst { it.uriString == playing.uriString }
            if (index != -1) {
                // If it is already in queue, let's remove it and insert it after playing video to avoid duplicates
                current.removeAll { it.uriString == video.uriString }
                // Recalculate playing index after removal
                val updatedIndex = current.indexOfFirst { it.uriString == playing.uriString }
                if (updatedIndex != -1) {
                    current.add(updatedIndex + 1, video)
                } else {
                    current.add(video)
                }
            } else {
                current.add(0, video)
            }
        } else {
            current.add(video)
        }
        _playbackQueue.value = current
    }

    fun closePlayer() {
        _playingVideo.value = null
    }

    fun saveResumePosition(uri: String, positionMs: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePlaybackPosition(uri, positionMs)
        }
    }

    fun toggleFavorite(video: VideoEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateFavorite(video.uriString, !video.isFavorite)
        }
    }

    fun deleteVideo(uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteVideo(uri)
            if (_playingVideo.value?.uriString == uri) {
                _playingVideo.value = null
            }
        }
    }

    fun handlePickedVideoUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            var title = "Picked_Video_${System.currentTimeMillis()}"
            var size = 0L
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx != -1) {
                            title = cursor.getString(nameIdx) ?: title
                        }
                        if (sizeIdx != -1) {
                            size = cursor.getLong(sizeIdx)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val pickedVideo = VideoEntity(
                uriString = uri.toString(),
                title = title,
                path = null,
                duration = 0L, // Will resolve inside ExoPlayer on prep
                size = size,
                addedDate = System.currentTimeMillis(),
                lastPlayedPosition = 0L,
                lastPlayedTimestamp = System.currentTimeMillis()
            )

            // Insert details or update if exists
            repository.insertOrReplace(pickedVideo)
            
            // Open screen
            _playingVideo.value = pickedVideo
            _playbackQueue.value = listOf(pickedVideo)
        }
    }

    fun handlePickedFolderUri(context: Context, treeUri: Uri) {
        _scanState.value = ScanState.SCANNING
        viewModelScope.launch(Dispatchers.IO) {
            try {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                var folderName = "Selected Folder"
                try {
                    val documentId = DocumentsContract.getTreeDocumentId(treeUri)
                    val treeDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                    context.contentResolver.query(treeDocUri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            folderName = cursor.getString(0) ?: folderName
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val videosList = mutableListOf<VideoEntity>()
                val documentId = DocumentsContract.getTreeDocumentId(treeUri)
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)

                val projection = arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED
                )

                context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                    val dateIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                    while (cursor.moveToNext()) {
                        val docId = cursor.getString(idIdx)
                        val mimeType = cursor.getString(mimeIdx) ?: ""
                        
                        if (mimeType.startsWith("video/")) {
                            val name = cursor.getString(nameIdx) ?: "Video_$docId"
                            val size = cursor.getLong(sizeIdx)
                            val lastMod = cursor.getLong(dateIdx)
                            
                            val videoUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            
                            videosList.add(
                                VideoEntity(
                                    uriString = videoUri.toString(),
                                    title = name,
                                    path = null,
                                    duration = 0L,
                                    size = size,
                                    addedDate = lastMod,
                                    folderName = folderName
                                )
                            )
                        }
                    }
                }

                if (videosList.isNotEmpty()) {
                    repository.insertVideos(videosList)
                    _playbackQueue.value = videosList
                    _playingVideo.value = videosList.first()
                    _scanState.value = ScanState.SUCCESS
                } else {
                    _scanState.value = ScanState.ERROR
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _scanState.value = ScanState.ERROR
            }
        }
    }

    fun playFolderPlaylist(folder: FolderPlaylist) {
        if (folder.videos.isNotEmpty()) {
            selectVideo(folder.videos.first(), folder.videos)
        }
    }

    fun queueFolderPlaylist(folder: FolderPlaylist) {
        val currentQueue = _playbackQueue.value.toMutableList()
        val newVideos = folder.videos.filter { video ->
            currentQueue.none { it.uriString == video.uriString }
        }
        currentQueue.addAll(newVideos)
        _playbackQueue.value = currentQueue
    }

    fun playNextVideo() {
        val current = _playingVideo.value ?: return
        val queue = _playbackQueue.value
        val currentIndex = queue.indexOfFirst { it.uriString == current.uriString }
        if (currentIndex != -1 && currentIndex < queue.size - 1) {
            selectVideo(queue[currentIndex + 1], queue)
        }
    }

    fun playPreviousVideo() {
        val current = _playingVideo.value ?: return
        val queue = _playbackQueue.value
        val currentIndex = queue.indexOfFirst { it.uriString == current.uriString }
        if (currentIndex != -1 && currentIndex > 0) {
            selectVideo(queue[currentIndex - 1], queue)
        }
    }

    // Audio Playback Controllers
    fun selectAudio(audio: AudioEntity, queueList: List<AudioEntity> = emptyList()) {
        _playingVideo.value = null // turn off full screen video player
        _playingAudio.value = audio
        if (queueList.isNotEmpty()) {
            _audioPlaybackQueue.value = queueList
        } else {
            _audioPlaybackQueue.value = listOf(audio)
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateAudioPlaybackPosition(audio.uriString, audio.lastPlayedPosition)
        }
    }

    fun closeAudioPlayer() {
        _playingAudio.value = null
    }

    fun toggleAudioFavorite(audio: AudioEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateAudioFavorite(audio.uriString, !audio.isFavorite)
        }
    }

    fun deleteAudio(uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAudio(uri)
            if (_playingAudio.value?.uriString == uri) {
                _playingAudio.value = null
            }
        }
    }

    fun playNextAudio() {
        val current = _playingAudio.value ?: return
        val queue = _audioPlaybackQueue.value
        val currentIndex = queue.indexOfFirst { it.uriString == current.uriString }
        if (currentIndex != -1 && currentIndex < queue.size - 1) {
            selectAudio(queue[currentIndex + 1], queue)
        }
    }

    fun playPreviousAudio() {
        val current = _playingAudio.value ?: return
        val queue = _audioPlaybackQueue.value
        val currentIndex = queue.indexOfFirst { it.uriString == current.uriString }
        if (currentIndex != -1 && currentIndex > 0) {
            selectAudio(queue[currentIndex - 1], queue)
        }
    }
}

class VideoPlayerViewModelFactory(
    private val repository: VideoRepository,
    private val scanner: MediaStoreVideoScanner,
    private val deepScanner: RecursiveDirectoryScanner,
    private val audioScanner: MediaStoreAudioScanner
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideoPlayerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VideoPlayerViewModel(repository, scanner, deepScanner, audioScanner) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
