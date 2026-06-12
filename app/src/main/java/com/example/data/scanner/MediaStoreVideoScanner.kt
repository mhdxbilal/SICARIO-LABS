package com.example.data.scanner

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.data.database.VideoEntity

class MediaStoreVideoScanner(private val context: Context) {

    fun scanVideosOnDevice(): List<VideoEntity> {
        val videosList = mutableListOf<VideoEntity>()
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        // Query only files that have some duration (to exclude corrupted/empty ones)
        val selection = "${MediaStore.Video.Media.DURATION} > 0"
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val folderColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Video_$id"
                    val path = cursor.getString(dataColumn)
                    val duration = cursor.getLong(durationColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    
                    var folder = cursor.getString(folderColumn)
                    if (folder.isNullOrBlank() && !path.isNullOrBlank()) {
                        val parts = path.split("/")
                        if (parts.size >= 2) {
                            folder = parts[parts.size - 2]
                        }
                    }
                    val folderNameValue = if (!folder.isNullOrBlank()) folder else "Internal Storage"

                    val contentUri = ContentUris.withAppendedId(collection, id)

                    videosList.add(
                        VideoEntity(
                            uriString = contentUri.toString(),
                            title = name,
                            path = path,
                            duration = duration,
                            size = size,
                            addedDate = dateAdded * 1000L, // convert to millis
                            folderName = folderNameValue
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return videosList
    }
}
