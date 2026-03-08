package com.kishor.gpstools

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.File
import java.io.FileReader
import java.text.SimpleDateFormat
import java.util.*

object CSVDatabaseReader {
    
    private val dateFormats = listOf(
        SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    )
    
    fun readDatabase(filePath: String): List<FileMetadata> {
        val metadataList = mutableListOf<FileMetadata>()
        
        FileReader(filePath).use { reader ->
            val csvParser = CSVParser(
                reader,
                CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build()
            )
            
            for (record in csvParser) {
                try {
                    val dateTimeOriginal = record.get("DateTimeOriginal")?.takeIf { it.isNotBlank() }
                    val timestamp = parseDate(dateTimeOriginal)
                    
                    val metadata = FileMetadata(
                        fileName = record.get("FileName"),
                        filePath = record.get("FilePath") ?: "Unknown",
                        fileSize = record.get("FileSize").toLongOrNull() ?: 0L,
                        fileType = record.get("FileType"),
                        make = record.get("Make")?.takeIf { it.isNotBlank() },
                        model = record.get("Model")?.takeIf { it.isNotBlank() },
                        gpsLatitude = record.get("GPSLatitude")?.toDoubleOrNull(),
                        gpsLongitude = record.get("GPSLongitude")?.toDoubleOrNull(),
                        gpsAltitude = record.get("GPSAltitude")?.toDoubleOrNull(),
                        dateTimeOriginal = dateTimeOriginal,
                        createDate = record.get("CreateDate")?.takeIf { it.isNotBlank() },
                        modifyDate = record.get("ModifyDate")?.takeIf { it.isNotBlank() },
                        mediaCreateDate = record.get("MediaCreateDate")?.takeIf { it.isNotBlank() },
                        mediaModifyDate = record.get("MediaModifyDate")?.takeIf { it.isNotBlank() },
                        trackCreateDate = record.get("TrackCreateDate")?.takeIf { it.isNotBlank() },
                        trackModifyDate = record.get("TrackModifyDate")?.takeIf { it.isNotBlank() },
                        offsetTime = record.get("OffsetTime")?.takeIf { it.isNotBlank() },
                        offsetTimeOriginal = record.get("OffsetTimeOriginal")?.takeIf { it.isNotBlank() },
                        offsetTimeDigitized = record.get("OffsetTimeDigitized")?.takeIf { it.isNotBlank() },
                        timestamp = timestamp,
                        remarks = record.get("Remarks")?.takeIf { it.isNotBlank() } ?: ""
                    )
                    
                    metadataList.add(metadata)
                } catch (e: Exception) {
                    // Skip malformed records
                    continue
                }
            }
        }
        
        return metadataList
    }
    
    private fun parseDate(dateString: String?): Date? {
        if (dateString.isNullOrBlank()) return null
        
        for (format in dateFormats) {
            try {
                return format.parse(dateString)
            } catch (e: Exception) {
                continue
            }
        }
        
        return null
    }
}
