package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val uriString: String,
    val title: String,
    val path: String?,
    val duration: Long,
    val size: Long,
    val addedDate: Long,
    val lastPlayedPosition: Long = 0L,
    val lastPlayedTimestamp: Long = 0L,
    val isFavorite: Boolean = false,
    val folderName: String = "Internal Storage"
) {
    val durationFormatted: String
        get() {
            if (duration <= 0) return "00:00"
            val totalSeconds = duration / 1000
            val seconds = totalSeconds % 60
            val minutes = (totalSeconds / 60) % 60
            val hours = totalSeconds / 3600
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val sizeFormatted: String
        get() {
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
            return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
        }
}
