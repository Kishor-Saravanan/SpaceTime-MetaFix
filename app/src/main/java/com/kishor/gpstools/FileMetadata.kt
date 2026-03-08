package com.kishor.gpstools

import java.util.Date

data class FileMetadata(
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val fileType: String,
    val make: String?,
    val model: String?,
    val gpsLatitude: Double?,
    val gpsLongitude: Double?,
    val gpsAltitude: Double?,
    val dateTimeOriginal: String?,
    val createDate: String?,
    val modifyDate: String?,
    val mediaCreateDate: String?,
    val mediaModifyDate: String?,
    val trackCreateDate: String?,
    val trackModifyDate: String?,
    val offsetTime: String?,
    val offsetTimeOriginal: String?,
    val offsetTimeDigitized: String?,
    val timestamp: Date?,
    var remarks: String = "" // For debug/status information
) {
    val hasGPS: Boolean
        get() = isValidGPS(gpsLatitude, gpsLongitude)
    
    val isPhoto: Boolean
        get() = fileType in listOf("jpg", "jpeg", "png", "heic")
    
    val isVideo: Boolean
        get() = fileType in listOf("mp4", "mov", "m4v")
    
    fun toCsvRow(): List<String> {
        return listOf(
            fileName,
            filePath,
            fileSize.toString(),
            fileType,
            make ?: "",
            model ?: "",
            gpsLatitude?.toString() ?: "",
            gpsLongitude?.toString() ?: "",
            gpsAltitude?.toString() ?: "",
            dateTimeOriginal ?: "",
            createDate ?: "",
            modifyDate ?: "",
            mediaCreateDate ?: "",
            mediaModifyDate ?: "",
            trackCreateDate ?: "",
            trackModifyDate ?: "",
            offsetTime ?: "",
            offsetTimeOriginal ?: "",
            offsetTimeDigitized ?: "",
            remarks
        )
    }
    
    companion object {
        fun isValidGPS(lat: Double?, lon: Double?): Boolean {
            if (lat == null || lon == null) return false
            // Check for dummy/invalid GPS coordinates
            if (lat == 0.0 && lon == 0.0) return false // Null Island
            if (lat < -90.0 || lat > 90.0) return false
            if (lon < -180.0 || lon > 180.0) return false
            return true
        }
        
        fun getCsvHeaders(): List<String> {
            return listOf(
                "FileName",
                "FilePath",
                "FileSize",
                "FileType",
                "Make",
                "Model",
                "GPSLatitude",
                "GPSLongitude",
                "GPSAltitude",
                "DateTimeOriginal",
                "CreateDate",
                "ModifyDate",
                "MediaCreateDate",
                "MediaModifyDate",
                "TrackCreateDate",
                "TrackModifyDate",
                "OffsetTime",
                "OffsetTimeOriginal",
                "OffsetTimeDigitized",
                "Remarks"
            )
        }
    }
}
