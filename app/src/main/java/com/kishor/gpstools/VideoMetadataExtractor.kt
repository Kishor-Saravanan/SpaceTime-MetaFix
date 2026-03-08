package com.kishor.gpstools

import android.media.MediaMetadataRetriever
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class VideoMetadata(
    val make: String?,
    val model: String?,
    val gpsLatitude: Double?,
    val gpsLongitude: Double?,
    val gpsAltitude: Double?,
    val createDate: String?,
    val modifyDate: String?,
    val mediaCreateDate: String?,
    val mediaModifyDate: String?,
    val trackCreateDate: String?,
    val trackModifyDate: String?
)

object VideoMetadataExtractor {
    
    fun extract(file: File): VideoMetadata {
        var gpsLatitude: Double? = null
        var gpsLongitude: Double? = null
        var gpsAltitude: Double? = null
        var createDate: String? = null
        var make: String? = null
        var model: String? = null
        
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            
            // Extract GPS location
            val location = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
            if (location != null) {
                val gpsData = parseGPSLocation(location)
                gpsLatitude = gpsData.first
                gpsLongitude = gpsData.second
                gpsAltitude = gpsData.third
            }
            
            // Extract creation date
            val dateString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
            createDate = if (dateString != null) {
                formatVideoDate(dateString)
            } else {
                // Fall back to file modification date
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                format.format(Date(file.lastModified()))
            }
            
            // Try to extract make/model (not always available)
            make = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
            
            retriever.release()
            
        } catch (e: Exception) {
            // If MediaMetadataRetriever fails, fall back to file date
            createDate = try {
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                format.format(Date(file.lastModified()))
            } catch (ex: Exception) {
                null
            }
        }
        
        return VideoMetadata(
            make = make,
            model = model,
            gpsLatitude = gpsLatitude,
            gpsLongitude = gpsLongitude,
            gpsAltitude = gpsAltitude,
            createDate = createDate,
            modifyDate = null,
            mediaCreateDate = createDate,
            mediaModifyDate = null,
            trackCreateDate = createDate,
            trackModifyDate = null
        )
    }
    
    private fun parseGPSLocation(location: String): Triple<Double?, Double?, Double?> {
        try {
            // Location format: "+37.5090+127.0620/" or "+37.5090+127.0620+100.5/"
            // ISO 6709 format
            val pattern = Regex("""([+-]\d+\.\d+)([+-]\d+\.\d+)([+-]\d+\.\d+)?""")
            val match = pattern.find(location)
            
            if (match != null) {
                val lat = match.groupValues[1].toDoubleOrNull()
                val lon = match.groupValues[2].toDoubleOrNull()
                val alt = match.groupValues.getOrNull(3)?.toDoubleOrNull()
                return Triple(lat, lon, alt)
            }
        } catch (e: Exception) {
            // Ignore parse errors
        }
        
        return Triple(null, null, null)
    }
    
    private fun formatVideoDate(dateString: String): String? {
        try {
            // Video dates can be in formats like:
            // "20240315T120530.000Z"
            // "20240315"
            // "2024-03-15T12:05:30Z"
            
            val formats = arrayOf(
                "yyyyMMdd'T'HHmmss.SSS'Z'",
                "yyyyMMdd'T'HHmmss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyyMMdd",
                "dd/MM/yyyy HH:mm"  // Handle existing format
            )
            
            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.US)
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    val date = sdf.parse(dateString)
                    if (date != null) {
                        // Always output in consistent format: YYYY-MM-DD HH:MM:SS
                        val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                        return outputFormat.format(date)
                    }
                } catch (e: Exception) {
                    continue
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        
        return null
    }
}
