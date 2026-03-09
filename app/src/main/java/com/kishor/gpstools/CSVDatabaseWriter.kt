package com.kishor.gpstools

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class CSVDatabaseWriter(private val destinationFolder: String) {
    
    fun writeDatabase(metadata: List<FileMetadata>): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val fileName = "database_$timestamp.csv"
        val file = File(destinationFolder, fileName)
        
        writeDatabaseToFile(metadata, file)
        
        return file.absolutePath
    }
    
    fun writeDatabaseToFile(metadata: List<FileMetadata>, file: File) {
        FileWriter(file).use { writer ->
            val csvPrinter = CSVPrinter(writer, CSVFormat.DEFAULT)
            
            // Write headers
            csvPrinter.printRecord(FileMetadata.getCsvHeaders())
            
            // Write data
            metadata.forEach { data ->
                csvPrinter.printRecord(data.toCsvRow())
            }
            
            csvPrinter.flush()
        }
    }
}
