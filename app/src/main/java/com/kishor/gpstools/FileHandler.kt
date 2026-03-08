package com.kishor.gpstools

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object FileHandler {
    
    fun createNoGpsFolder(destinationFolder: String): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val folderName = "NOGPS_$timestamp"
        val folder = File(destinationFolder, folderName)
        
        if (!folder.exists()) {
            folder.mkdirs()
        }
        
        return folder
    }
    
    fun findNoGpsFolder(destinationFolder: String): File? {
        val folder = File(destinationFolder)
        if (!folder.exists() || !folder.isDirectory) return null
        
        val noGpsFolders = folder.listFiles { file ->
            file.isDirectory && file.name.startsWith("NOGPS_")
        }
        
        return noGpsFolders?.maxByOrNull { it.lastModified() }
    }
    
    fun moveFile(sourceFile: File, destinationFolder: File): Boolean {
        return try {
            val destFile = File(destinationFolder, sourceFile.name)
            
            if (sourceFile.renameTo(destFile)) {
                true
            } else {
                // If rename fails, try copy and delete
                copyFile(sourceFile, destinationFolder)
                sourceFile.delete()
            }
        } catch (e: Exception) {
            false
        }
    }
    
    fun copyFile(sourceFile: File, destinationFolder: File): Boolean {
        return try {
            val destFile = File(destinationFolder, sourceFile.name)
            
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
}
