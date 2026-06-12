package com.example.data.scanner

import android.os.Environment
import com.example.data.database.VideoEntity

class RecursiveDirectoryScanner {

    fun scanVideosFromLocal(): List<VideoEntity> {
        val rootPath = Environment.getExternalStorageDirectory()
        val videoExtensions = listOf("mp4", "mkv", "avi")
        val result = mutableListOf<VideoEntity>()

        try {
             rootPath.walkTopDown()
                 .onEnter { dir -> 
                     !dir.name.startsWith(".") && dir.name != "Android" 
                 }
                 .filter { it.isFile && it.extension.lowercase() in videoExtensions }
                 .forEach { file ->
                     result.add(
                         VideoEntity(
                             uriString = file.absolutePath, 
                             title = file.name,
                             path = file.absolutePath,
                             duration = 0L,
                             size = file.length(),
                             addedDate = file.lastModified(),
                             folderName = file.parentFile?.name ?: "Deep Scan"
                         )
                     )
                 }
        } catch(e: Exception) {
            e.printStackTrace()
        }

        return result
    }
}
