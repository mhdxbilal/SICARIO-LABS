package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY title ASC")
    fun getAllVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC")
    fun getRecentVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteVideos(): Flow<List<VideoEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(video: VideoEntity)

    @Update
    suspend fun updateVideo(video: VideoEntity)

    @Query("UPDATE videos SET lastPlayedPosition = :position, lastPlayedTimestamp = :timestamp WHERE uriString = :uriString")
    suspend fun updatePlaybackPosition(uriString: String, position: Long, timestamp: Long)

    @Query("UPDATE videos SET isFavorite = :isFav WHERE uriString = :uriString")
    suspend fun updateFavorite(uriString: String, isFav: Boolean)

    @Query("DELETE FROM videos WHERE uriString = :uriString")
    suspend fun deleteVideo(uriString: String)

    @Query("DELETE FROM videos")
    suspend fun clearAll()
}
