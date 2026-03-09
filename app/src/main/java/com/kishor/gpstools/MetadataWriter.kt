package com.kishor.gpstools

import android.content.Context
import android.media.MediaScannerConnection
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * v6.0 - Complete solution for both photos and videos
 * 
 * PHOTOS: FileDescriptor approach (Samsung's method) - works for ALL formats including HEIC
 * VIDEOS: Direct MP4 atom editing - fast, preserves everything
 */
class MetadataWriter(private val context: Context) {
    
    private val mp4GpsWriter = MP4GpsWriter()
    
    /**
     * Write GPS to new file in output folder
     * Automatically detects format and uses appropriate method
     */
    fun writeGPSToNewFile(
        sourceFile: File,
        outputFolder: File,
        latitude: Double?,
        longitude: Double?,
        altitude: Double?
    ): File? {
        if (latitude == null || longitude == null) {
            android.util.Log.e("MetadataWriter", "Null GPS coordinates")
            return null
        }
        
        val extension = sourceFile.extension.lowercase()
        
        return when (extension) {
            "jpg", "jpeg", "png", "heic" -> writePhotoWithGPS(sourceFile, outputFolder, latitude, longitude, altitude)
            "mp4", "mov", "m4v" -> writeVideoWithGPS(sourceFile, outputFolder, latitude, longitude)
            else -> {
                android.util.Log.e("MetadataWriter", "Unsupported format: $extension")
                null
            }
        }
    }
    
    /**
     * PHOTOS: FileDescriptor approach
     * This is Samsung's method - works for ALL formats including HEIC
     */
    private fun writePhotoWithGPS(
        source: File,
        outputFolder: File,
        lat: Double,
        lon: Double,
        alt: Double?
    ): File? {
        val output = File(outputFolder, source.name)
        val originalTime = source.lastModified()
        
        try {
            // Step 1: Copy file to output
            FileInputStream(source).use { input ->
                FileOutputStream(output).use { outputStream ->
                    input.copyTo(outputStream)
                }
            }
            
            android.util.Log.d("MetadataWriter", "Copied ${source.name}")
            
            // Step 2: Write GPS using FileDescriptor (THE KEY for HEIC!)
            FileInputStream(output).use { fis ->
                val exif = ExifInterface(fis.fd)
                
                // Set GPS coordinates
                exif.setLatLong(lat, lon)
                
                // Set altitude if provided
                if (alt != null) {
                    val altRef = if (alt >= 0) "0" else "1"
                    exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, "${Math.abs(alt)}")
                    exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, altRef)
                }
                
                // Save changes
                exif.saveAttributes()
            }
            
            android.util.Log.d("MetadataWriter", "GPS written via FileDescriptor")
            
            // Step 3: Verify GPS was written
            FileInputStream(output).use { fis ->
                val verify = ExifInterface(fis.fd)
                val coords = FloatArray(2)
                
                if (!verify.getLatLong(coords)) {
                    android.util.Log.e("MetadataWriter", "Verification failed - no GPS")
                    output.delete()
                    return null
                }
                
                val latOk = Math.abs(coords[0] - lat) < 0.001
                val lonOk = Math.abs(coords[1] - lon) < 0.001
                
                if (!latOk || !lonOk) {
                    android.util.Log.e("MetadataWriter", "GPS mismatch")
                    output.delete()
                    return null
                }
            }
            
            // Step 4: Preserve timestamp
            output.setLastModified(originalTime)
            
            // Step 5: Scan for gallery
            MediaScannerConnection.scanFile(context, arrayOf(output.absolutePath), null, null)
            
            android.util.Log.i("MetadataWriter", "✓ Photo GPS: ${source.name} (${source.extension})")
            return output
            
        } catch (e: Exception) {
            android.util.Log.e("MetadataWriter", "Photo error: ${e.message}", e)
            if (output.exists()) output.delete()
            return null
        }
    }
    
    /**
     * VIDEOS: Direct MP4 atom editing
     * Fast (seconds), preserves ALL metadata automatically
     */
    private fun writeVideoWithGPS(
        source: File,
        outputFolder: File,
        lat: Double,
        lon: Double
    ): File? {
        val output = File(outputFolder, source.name)
        val originalTime = source.lastModified()
        
        try {
            // Step 1: Copy file to output
            FileInputStream(source).use { input ->
                FileOutputStream(output).use { outputStream ->
                    input.copyTo(outputStream)
                }
            }
            
            android.util.Log.d("MetadataWriter", "Copied ${source.name}")
            
            // Step 2: Write GPS using direct MP4 editing (FAST!)
            val success = mp4GpsWriter.writeGPS(output, lat, lon, null)
            
            if (!success) {
                android.util.Log.e("MetadataWriter", "Failed to write GPS to video")
                output.delete()
                return null
            }
            
            // Step 3: Timestamp already preserved by MP4GpsWriter
            // Step 4: All metadata automatically preserved (no rebuild!)
            
            // Step 5: Verify file is valid
            if (!output.exists() || output.length() == 0L) {
                android.util.Log.e("MetadataWriter", "Output file invalid")
                output.delete()
                return null
            }
            
            // Step 6: Scan for gallery
            MediaScannerConnection.scanFile(context, arrayOf(output.absolutePath), null, null)
            
            android.util.Log.i("MetadataWriter", "✓ Video GPS: ${source.name} (direct edit)")
            return output
            
        } catch (e: Exception) {
            android.util.Log.e("MetadataWriter", "Video error: ${e.message}", e)
            if (output.exists()) output.delete()
            return null
        }
    }
}
