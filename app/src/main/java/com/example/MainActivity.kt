package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.AppDatabase
import com.example.data.database.VideoRepository
import com.example.data.scanner.MediaStoreVideoScanner
import com.example.ui.main.MediaDashboardScreen
import com.example.ui.player.VideoPlayerView
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.VideoPlayerViewModel
import com.example.ui.viewmodel.VideoPlayerViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Initialize DB and classes
        val database = AppDatabase.getDatabase(this)
        val repository = VideoRepository(database.videoDao())
        val scanner = MediaStoreVideoScanner(this)
        val deepScanner = com.example.data.scanner.RecursiveDirectoryScanner()
        
        val viewModel = ViewModelProvider(
            this,
            VideoPlayerViewModelFactory(repository, scanner, deepScanner)
        )[VideoPlayerViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val playingVideo by viewModel.playingVideo.collectAsStateWithLifecycle()

                // Register back press handler during video playback to close player
                BackHandler(enabled = playingVideo != null) {
                    viewModel.closePlayer()
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (playingVideo != null) {
                        VideoPlayerView(
                            video = playingVideo!!,
                            onClose = { viewModel.closePlayer() },
                            onPlayNext = { viewModel.playNextVideo() },
                            onPlayPrevious = { viewModel.playPreviousVideo() },
                            videoRepository = repository,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        MediaDashboardScreen(
                            viewModel = viewModel,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
