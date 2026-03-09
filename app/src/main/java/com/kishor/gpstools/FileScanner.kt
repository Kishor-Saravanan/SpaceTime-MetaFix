package com.kishor.gpstools

import java.io.File

object FileScanner {
    
    private val supportedExtensions = setOf(
        "jpg", "jpeg", "png", "heic",  // Photos
        "mp4", "mov", "m4v"             // Videos
    )
    
    fun scanForMediaFiles(directory: File, includeSubfolders: Boolean = false): List<File> {
        if (!directory.exists() || !directory.isDirectory) {
            return emptyList()
        }
        
        val mediaFiles = mutableListOf<File>()
        
        if (includeSubfolders) {
            scanRecursively(directory, mediaFiles)
        } else {
            // Only scan files in THIS directory - no subdirectories
            directory.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val extension = file.extension.lowercase()
                    if (extension in supportedExtensions) {
                        mediaFiles.add(file)
                    }
                }
            }
        }
        
        return mediaFiles.sortedBy { it.name }
    }
    
    private fun scanRecursively(directory: File, mediaFiles: MutableList<File>) {
        directory.listFiles()?.forEach { file ->
            when {
                file.isDirectory -> scanRecursively(file, mediaFiles)
                file.isFile -> {
                    val extension = file.extension.lowercase()
                    if (extension in supportedExtensions) {
                        mediaFiles.add(file)
                    }
                }
            }
        }
    }
}
