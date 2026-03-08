package com.kishor.gpstools

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class CSVDatabaseWriter(private val destinationFolder: String) {
    
    fun writeDatabase(metadataList: List<FileMetadata>): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val fileName = "database_$timestamp.csv"
        val filePath = File(destinationFolder, fileName).absolutePath
        
        FileWriter(filePath).use { writer ->
            val csvPrinter = CSVPrinter(
                writer,
                CSVFormat.DEFAULT.builder()
                    .setHeader(*FileMetadata.getCsvHeaders().toTypedArray())
                    .build()
            )
            
            metadataList.forEach { metadata ->
                csvPrinter.printRecord(metadata.toCsvRow())
            }
            
            csvPrinter.flush()
        }
        
        return filePath
    }
    
    companion object {
        fun findLatestDatabase(destinationFolder: String): String? {
            val folder = File(destinationFolder)
            if (!folder.exists() || !folder.isDirectory) return null
            
            val databaseFiles = folder.listFiles { file ->
                file.name.startsWith("database_") && file.extension == "csv"
            }
            
            return databaseFiles?.maxByOrNull { it.lastModified() }?.absolutePath
        }
    }
}
