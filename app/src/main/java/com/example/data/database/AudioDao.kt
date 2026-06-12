package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioDao {
    @Query("SELECT * FROM audios ORDER BY title ASC")
    fun getAllAudios(): Flow<List<AudioEntity>>

    @Query("SELECT * FROM audios WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC")
    fun getRecentAudios(): Flow<List<AudioEntity>>

    @Query("SELECT * FROM audios WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteAudios(): Flow<List<AudioEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAudios(audios: List<AudioEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(audio: AudioEntity)

    @Update
    suspend fun updateAudio(audio: AudioEntity)

    @Query("UPDATE audios SET lastPlayedPosition = :position, lastPlayedTimestamp = :timestamp WHERE uriString = :uriString")
    suspend fun updatePlaybackPosition(uriString: String, position: Long, timestamp: Long)

    @Query("UPDATE audios SET isFavorite = :isFav WHERE uriString = :uriString")
    suspend fun updateFavorite(uriString: String, isFav: Boolean)

    @Query("DELETE FROM audios WHERE uriString = :uriString")
    suspend fun deleteAudio(uriString: String)

    @Query("DELETE FROM audios")
    suspend fun clearAll()
}
