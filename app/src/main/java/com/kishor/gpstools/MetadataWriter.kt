package com.kishor.gpstools

import androidx.exifinterface.media.ExifInterface
import java.io.File

object MetadataWriter {
    
    fun writeGPSData(
        file: File,
        latitude: Double?,
        longitude: Double?,
        altitude: Double?
    ): Boolean {
        if (latitude == null || longitude == null) return false
        
        val fileType = file.extension.lowercase()
        
        return when (fileType) {
            "jpg", "jpeg", "png" -> writePhotoGPS(file, latitude, longitude, altitude)
            "mp4", "mov", "m4v" -> writeVideoGPS(file, latitude, longitude, altitude)
            else -> false
        }
    }
    
    private fun writePhotoGPS(
        file: File,
        latitude: Double,
        longitude: Double,
        altitude: Double?
    ): Boolean {
        return try {
            val exif = ExifInterface(file.absolutePath)
            
            // Set GPS latitude
            val latRef = if (latitude >= 0) "N" else "S"
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latRef)
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, convertToExifFormat(kotlin.math.abs(latitude)))
            
            // Set GPS longitude
            val lonRef = if (longitude >= 0) "E" else "W"
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, lonRef)
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, convertToExifFormat(kotlin.math.abs(longitude)))
            
            // Set GPS altitude if available
            if (altitude != null) {
                val altRef = if (altitude >= 0) "0" else "1"
                exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, altRef)
                exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, "${kotlin.math.abs(altitude)}/1")
            }
            
            // Save changes
            exif.saveAttributes()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun writeVideoGPS(
        file: File,
        latitude: Double,
        longitude: Double,
        altitude: Double?
    ): Boolean {
        // Video GPS writing is more complex and requires specialized libraries
        // For now, we'll use a simplified approach or external tool
        // This would typically require QuickTime metadata manipulation
        
        return try {
            VideoMetadataWriter.writeGPS(file, latitude, longitude, altitude)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun convertToExifFormat(coordinate: Double): String {
        val degrees = coordinate.toInt()
        val minutesDecimal = (coordinate - degrees) * 60
        val minutes = minutesDecimal.toInt()
        val seconds = (minutesDecimal - minutes) * 60
        
        return "$degrees/1,$minutes/1,${(seconds * 1000).toInt()}/1000"
    }
}

object VideoMetadataWriter {
    
    fun writeGPS(file: File, latitude: Double, longitude: Double, altitude: Double?): Boolean {
        // Video GPS writing requires specialized QuickTime atom manipulation
        // This is a placeholder - in production, use a library like mp4parser or exiftool
        
        // For now, we'll create a sidecar file with GPS data
        // or use system exec to call exiftool if available
        
        return try {
            // Attempt to write using basic MP4 box manipulation
            // This is simplified and may not work for all video formats
            writeQuickTimeGPS(file, latitude, longitude, altitude)
        } catch (e: Exception) {
            // Fall back to creating a sidecar file
            writeSidecarGPS(file, latitude, longitude, altitude)
        }
    }
    
    private fun writeQuickTimeGPS(file: File, latitude: Double, longitude: Double, altitude: Double?): Boolean {
        // This would require detailed MP4/QuickTime box manipulation
        // Implementation would use mp4parser or similar library
        // Placeholder for now
        return false
    }
    
    private fun writeSidecarGPS(file: File, latitude: Double, longitude: Double, altitude: Double?): Boolean {
        return try {
            val sidecarFile = File(file.parentFile, "${file.nameWithoutExtension}.gps")
            sidecarFile.writeText("GPS:Latitude=$latitude\nGPS:Longitude=$longitude\nGPS:Altitude=${altitude ?: ""}")
            true
        } catch (e: Exception) {
            false
        }
    }
}
