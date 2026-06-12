package com.example.data.database

import kotlinx.coroutines.flow.Flow

class VideoRepository(
    private val videoDao: VideoDao,
    private val audioDao: AudioDao
) {
    val allVideos: Flow<List<VideoEntity>> = videoDao.getAllVideos()
    val recentVideos: Flow<List<VideoEntity>> = videoDao.getRecentVideos()
    val favoriteVideos: Flow<List<VideoEntity>> = videoDao.getFavoriteVideos()

    val allAudios: Flow<List<AudioEntity>> = audioDao.getAllAudios()
    val recentAudios: Flow<List<AudioEntity>> = audioDao.getRecentAudios()
    val favoriteAudios: Flow<List<AudioEntity>> = audioDao.getFavoriteAudios()

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

    suspend fun insertAudios(audios: List<AudioEntity>) {
        audioDao.insertAudios(audios)
    }

    suspend fun insertOrReplaceAudio(audio: AudioEntity) {
        audioDao.insertOrReplace(audio)
    }

    suspend fun updateAudio(audio: AudioEntity) {
        audioDao.updateAudio(audio)
    }

    suspend fun updateAudioPlaybackPosition(uri: String, position: Long) {
        audioDao.updatePlaybackPosition(uri, position, System.currentTimeMillis())
    }

    suspend fun updateAudioFavorite(uri: String, isFav: Boolean) {
        audioDao.updateFavorite(uri, isFav)
    }

    suspend fun deleteAudio(uri: String) {
        audioDao.deleteAudio(uri)
    }

    suspend fun clearAll() {
        videoDao.clearAll()
        audioDao.clearAll()
    }
}
