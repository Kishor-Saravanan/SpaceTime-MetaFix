package com.kishor.gpstools

import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MetadataExtractor {
    
    fun extractMetadata(file: File): FileMetadata {
        val extension = file.extension.lowercase()
        
        return when (extension) {
            "jpg", "jpeg" -> extractJpegMetadata(file)
            "png" -> extractPngMetadata(file)
            "heic" -> extractHeicMetadata(file)
            "mp4", "mov", "m4v" -> extractVideoMetadata(file)
            else -> createEmptyMetadata(file)
        }
    }
    
    private fun extractJpegMetadata(file: File): FileMetadata {
        try {
            val exif = ExifInterface(file.absolutePath)
            
            val latLong = FloatArray(2)
            val hasGPS = exif.getLatLong(latLong)
            
            val gpsLat = if (hasGPS) latLong[0].toDouble() else null
            val gpsLon = if (hasGPS) latLong[1].toDouble() else null
            val gpsAlt = exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE)?.toDoubleOrNull()
            
            val dateTimeOriginal = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            val timestamp = parseDate(dateTimeOriginal)
            
            return FileMetadata(
                fileName = file.name,
                filePath = file.parent ?: "Unknown",
                fileSize = file.length(),
                fileType = file.extension.lowercase(),
                make = exif.getAttribute(ExifInterface.TAG_MAKE),
                model = exif.getAttribute(ExifInterface.TAG_MODEL),
                gpsLatitude = gpsLat,
                gpsLongitude = gpsLon,
                gpsAltitude = gpsAlt,
                dateTimeOriginal = normalizeDate(dateTimeOriginal),
                createDate = normalizeDate(exif.getAttribute(ExifInterface.TAG_DATETIME)),
                modifyDate = null,
                mediaCreateDate = normalizeDate(dateTimeOriginal),
                mediaModifyDate = null,
                trackCreateDate = null,
                trackModifyDate = null,
                offsetTime = exif.getAttribute(ExifInterface.TAG_OFFSET_TIME),
                offsetTimeOriginal = exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL),
                offsetTimeDigitized = exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_DIGITIZED),
                timestamp = timestamp
            )
        } catch (e: Exception) {
            return createEmptyMetadata(file)
        }
    }
    
    private fun extractPngMetadata(file: File): FileMetadata {
        try {
            val exif = ExifInterface(file.absolutePath)
            
            val latLong = FloatArray(2)
            val hasGPS = exif.getLatLong(latLong)
            
            val gpsLat = if (hasGPS) latLong[0].toDouble() else null
            val gpsLon = if (hasGPS) latLong[1].toDouble() else null
            
            val dateTimeOriginal = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            val timestamp = parseDate(dateTimeOriginal)
            
            return FileMetadata(
                fileName = file.name,
                filePath = file.parent ?: "Unknown",
                fileSize = file.length(),
                fileType = file.extension.lowercase(),
                make = exif.getAttribute(ExifInterface.TAG_MAKE),
                model = exif.getAttribute(ExifInterface.TAG_MODEL),
                gpsLatitude = gpsLat,
                gpsLongitude = gpsLon,
                gpsAltitude = exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE)?.toDoubleOrNull(),
                dateTimeOriginal = normalizeDate(dateTimeOriginal),
                createDate = normalizeDate(exif.getAttribute(ExifInterface.TAG_DATETIME)),
                modifyDate = null,
                mediaCreateDate = normalizeDate(dateTimeOriginal),
                mediaModifyDate = null,
                trackCreateDate = null,
                trackModifyDate = null,
                offsetTime = exif.getAttribute(ExifInterface.TAG_OFFSET_TIME),
                offsetTimeOriginal = exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL),
                offsetTimeDigitized = exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_DIGITIZED),
                timestamp = timestamp
            )
        } catch (e: Exception) {
            return createEmptyMetadata(file)
        }
    }
    
    private fun extractHeicMetadata(file: File): FileMetadata {
        try {
            val exif = ExifInterface(file.absolutePath)
            
            val latLong = FloatArray(2)
            val hasGPS = exif.getLatLong(latLong)
            
            val gpsLat = if (hasGPS) latLong[0].toDouble() else null
            val gpsLon = if (hasGPS) latLong[1].toDouble() else null
            
            val dateTimeOriginal = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            val timestamp = parseDate(dateTimeOriginal)
            
            return FileMetadata(
                fileName = file.name,
                filePath = file.parent ?: "Unknown",
                fileSize = file.length(),
                fileType = file.extension.lowercase(),
                make = exif.getAttribute(ExifInterface.TAG_MAKE),
                model = exif.getAttribute(ExifInterface.TAG_MODEL),
                gpsLatitude = gpsLat,
                gpsLongitude = gpsLon,
                gpsAltitude = exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE)?.toDoubleOrNull(),
                dateTimeOriginal = normalizeDate(dateTimeOriginal),
                createDate = normalizeDate(exif.getAttribute(ExifInterface.TAG_DATETIME)),
                modifyDate = null,
                mediaCreateDate = normalizeDate(dateTimeOriginal),
                mediaModifyDate = null,
                trackCreateDate = null,
                trackModifyDate = null,
                offsetTime = exif.getAttribute(ExifInterface.TAG_OFFSET_TIME),
                offsetTimeOriginal = exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL),
                offsetTimeDigitized = exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_DIGITIZED),
                timestamp = timestamp
            )
        } catch (e: Exception) {
            return createEmptyMetadata(file)
        }
    }
    
    private fun extractVideoMetadata(file: File): FileMetadata {
        val videoMetadata = VideoMetadataExtractor.extract(file)
        val timestamp = parseDate(videoMetadata.createDate)
        
        return FileMetadata(
            fileName = file.name,
            filePath = file.parent ?: "Unknown",
            fileSize = file.length(),
            fileType = file.extension.lowercase(),
            make = videoMetadata.make,
            model = videoMetadata.model,
            gpsLatitude = videoMetadata.gpsLatitude,
            gpsLongitude = videoMetadata.gpsLongitude,
            gpsAltitude = videoMetadata.gpsAltitude,
            dateTimeOriginal = videoMetadata.createDate,
            createDate = videoMetadata.createDate,
            modifyDate = videoMetadata.modifyDate,
            mediaCreateDate = videoMetadata.mediaCreateDate,
            mediaModifyDate = videoMetadata.mediaModifyDate,
            trackCreateDate = videoMetadata.trackCreateDate,
            trackModifyDate = videoMetadata.trackModifyDate,
            offsetTime = null,
            offsetTimeOriginal = null,
            offsetTimeDigitized = null,
            timestamp = timestamp
        )
    }
    
    private fun createEmptyMetadata(file: File): FileMetadata {
        val fileDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(file.lastModified()))
        val timestamp = parseDate(fileDate)
        
        return FileMetadata(
            fileName = file.name,
            filePath = file.parent ?: "Unknown",
            fileSize = file.length(),
            fileType = file.extension.lowercase(),
            make = null,
            model = null,
            gpsLatitude = null,
            gpsLongitude = null,
            gpsAltitude = null,
            dateTimeOriginal = fileDate,
            createDate = fileDate,
            modifyDate = null,
            mediaCreateDate = null,
            mediaModifyDate = null,
            trackCreateDate = null,
            trackModifyDate = null,
            offsetTime = null,
            offsetTimeOriginal = null,
            offsetTimeDigitized = null,
            timestamp = timestamp
        )
    }
    
    private fun parseDate(dateString: String?): Date? {
        if (dateString.isNullOrBlank()) return null
        
        val formats = arrayOf(
            "yyyy:MM:dd HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy:MM:dd HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        )
        
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                return sdf.parse(dateString)
            } catch (e: Exception) {
                continue
            }
        }
        
        return null
    }
    
    private fun formatDate(date: Date?): String? {
        if (date == null) return null
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return sdf.format(date)
    }
    
    private fun normalizeDate(dateString: String?): String? {
        if (dateString.isNullOrBlank()) return null
        val date = parseDate(dateString)
        return formatDate(date)
    }
}
