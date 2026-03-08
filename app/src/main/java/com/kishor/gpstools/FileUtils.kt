package com.kishor.gpstools

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore

object FileUtils {
    
    fun getPathFromUri(context: Context, uri: Uri): String? {
        return try {
            when {
                DocumentsContract.isDocumentUri(context, uri) -> {
                    getPathFromDocumentUri(context, uri)
                }
                "content".equals(uri.scheme, ignoreCase = true) -> {
                    getPathFromContentUri(context, uri)
                }
                "file".equals(uri.scheme, ignoreCase = true) -> {
                    uri.path
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getPathFromDocumentUri(context: Context, uri: Uri): String? {
        val docId = DocumentsContract.getDocumentId(uri)
        
        return when {
            isExternalStorageDocument(uri) -> {
                val split = docId.split(":")
                if (split.size >= 2) {
                    val type = split[0]
                    if ("primary".equals(type, ignoreCase = true)) {
                        "/storage/emulated/0/${split[1]}"
                    } else {
                        "/storage/$type/${split[1]}"
                    }
                } else null
            }
            isDownloadsDocument(uri) -> {
                "/storage/emulated/0/Download"
            }
            isMediaDocument(uri) -> {
                val split = docId.split(":")
                if (split.size >= 2) {
                    val contentUri = when (split[0]) {
                        "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        else -> null
                    }
                    
                    contentUri?.let {
                        getPathFromContentUri(context, it)
                    }
                } else null
            }
            else -> null
        }
    }
    
    private fun getPathFromContentUri(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        
        return try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                    cursor.getString(columnIndex)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }
    
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }
    
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }
}
