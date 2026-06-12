package com.example.data.database

import kotlinx.coroutines.flow.Flow

class VideoRepository(private val videoDao: VideoDao) {
    val allVideos: Flow<List<VideoEntity>> = videoDao.getAllVideos()
    val recentVideos: Flow<List<VideoEntity>> = videoDao.getRecentVideos()
    val favoriteVideos: Flow<List<VideoEntity>> = videoDao.getFavoriteVideos()

    suspend fun insertVideos(videos: List<VideoEntity>) {
        videoDao.insertVideos(videos)
    }

    suspend fun insertOrReplace(video: VideoEntity) {
        videoDao.insertOrReplace(video)
    }

    suspend fun updateVideo(video: VideoEntity) {
        videoDao.updateVideo(video)
    }

    suspend fun updatePlaybackPosition(uri: String, position: Long) {
        videoDao.updatePlaybackPosition(uri, position, System.currentTimeMillis())
    }

    suspend fun updateFavorite(uri: String, isFav: Boolean) {
        videoDao.updateFavorite(uri, isFav)
    }

    suspend fun deleteVideo(uri: String) {
        videoDao.deleteVideo(uri)
    }

    suspend fun clearAll() {
        videoDao.clearAll()
    }
}
