package com.example.data.scanner

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.data.database.AudioEntity

class MediaStoreAudioScanner(private val context: Context) {

    fun scanAudioOnDevice(): List<AudioEntity> {
        val audioList = mutableListOf<AudioEntity>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Audio.Media.DURATION} > 1000"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Track_$id"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val album = cursor.getString(albumColumn) ?: "Unknown Album"
                    val path = cursor.getString(dataColumn)
                    val duration = cursor.getLong(durationColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)

                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val extension = path?.substringAfterLast('.', "mp3")?.lowercase() ?: "mp3"
                    
                    // Mark FLAC/WAV as lossless, other files with standard formats
                    val sampleRate = if (extension in listOf("flac", "wav")) 48000 else 44100
                    val bitDepth = if (extension in listOf("flac", "wav")) 24 else 16

                    audioList.add(
                        AudioEntity(
                            uriString = contentUri.toString(),
                            title = title,
                            artist = artist,
                            album = album,
                            path = path,
                            duration = duration,
                            size = size,
                            addedDate = dateAdded * 1000L,
                            format = extension,
                            sampleRate = sampleRate,
                            bitDepth = bitDepth
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback or generic entries if empty to showcase the music player formats
        if (audioList.isEmpty()) {
            audioList.addAll(getDemoAudios())
        }

        return audioList
    }

    private fun getDemoAudios(): List<AudioEntity> {
        return listOf(
            AudioEntity(
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                title = "Ethereal Echoes (Hi-Res Special)",
                artist = "Symphonix",
                album = "Acoustic Landscapes",
                path = "demo_song_1.flac",
                duration = 372000L,
                size = 45201092L,
                addedDate = System.currentTimeMillis(),
                format = "flac",
                sampleRate = 96000,
                bitDepth = 24
            ),
            AudioEntity(
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                title = "Chamber Orchestra (Master Quality)",
                artist = "Vivaldi Ensemble",
                album = "Classic Redefined",
                path = "demo_song_2.wav",
                duration = 423000L,
                size = 84891102L,
                addedDate = System.currentTimeMillis() - 86400000L,
                format = "wav",
                sampleRate = 192000,
                bitDepth = 24
            ),
            AudioEntity(
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                title = "Neon Nights (Lossless Studio)",
                artist = "Pixelated Wave",
                album = "Retro Futuristic",
                path = "demo_song_3.flac",
                duration = 302000L,
                size = 31201092L,
                addedDate = System.currentTimeMillis() - 172800000L,
                format = "flac",
                sampleRate = 48000,
                bitDepth = 16
            ),
            AudioEntity(
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                title = "Midnight Coffee (High Res PCM)",
                artist = "Lo-Fi Beats Collective",
                album = "Café Vibrations",
                path = "demo_song_4.wav",
                duration = 245000L,
                size = 49102432L,
                addedDate = System.currentTimeMillis() - 259200000L,
                format = "wav",
                sampleRate = 96000,
                bitDepth = 24
            ),
            AudioEntity(
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                title = "Summer Breeze (Standard AAC)",
                artist = "Sunny Side",
                album = "Beach Party",
                path = "demo_song_5.m4a",
                duration = 189000L,
                size = 6201092L,
                addedDate = System.currentTimeMillis() - 345600000L,
                format = "m4a",
                sampleRate = 44100,
                bitDepth = 16
            )
        )
    }
}
