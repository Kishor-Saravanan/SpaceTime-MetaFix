package com.kishor.gpstools

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class Statistics(
    val total: Int = 0,
    val moved: Int = 0,
    val kept: Int = 0,
    val tagged: Int = 0
)

enum class FileAction {
    MOVE, COPY, NOTHING
}

class MetadataViewModel : ViewModel() {
    
    private val _status = MutableStateFlow("Ready")
    val status: StateFlow<String> = _status
    
    private val _progress = MutableStateFlow("")
    val progress: StateFlow<String> = _progress
    
    private val _statistics = MutableStateFlow(Statistics())
    val statistics: StateFlow<Statistics> = _statistics
    
    private var timeWindowMinutes = 15
    private var databasePath: String? = null
    
    fun setTimeWindow(minutes: Int) {
        timeWindowMinutes = minutes
    }
    
    suspend fun buildDatabase(
        sourceFolders: List<String>,
        destinationFolder: String,
        fileAction: FileAction,
        includeSubfolders: Boolean = false
    ) = withContext(Dispatchers.IO) {
        try {
            _status.emit("Starting database build...")
            _statistics.emit(Statistics())
            
            // Scan all files
            _progress.emit("Scanning folders...")
            val allFiles = mutableListOf<File>()
            
            sourceFolders.forEach { folder ->
                val files = FileScanner.scanForMediaFiles(File(folder), includeSubfolders)
                allFiles.addAll(files)
                _progress.emit("Found ${allFiles.size} files so far...")
            }
            
            val totalFiles = allFiles.size
            _progress.emit("Found $totalFiles total files")
            
            // Extract metadata
            val metadataExtractor = MetadataExtractor()
            val fileMetadataList = mutableListOf<FileMetadata>()
            val filesWithGPS = mutableListOf<Pair<File, FileMetadata>>()
            val filesWithoutGPS = mutableListOf<Pair<File, FileMetadata>>()
            var filesProcessed = 0
            
            allFiles.forEach { file ->
                filesProcessed++
                val location = file.parent ?: "Unknown"
                _progress.emit("Processing $filesProcessed/$totalFiles: ${file.name}\n📍 $location")
                
                val metadata = metadataExtractor.extractMetadata(file)
                
                // Add remarks for GPS status
                if (!metadata.hasGPS) {
                    if (metadata.gpsLatitude == 0.0 && metadata.gpsLongitude == 0.0) {
                        metadata.remarks = "Invalid GPS (0,0) - Dummy location"
                    } else if (metadata.gpsLatitude != null && metadata.gpsLongitude != null) {
                        metadata.remarks = "Invalid GPS - Out of range"
                    } else {
                        metadata.remarks = "No GPS data"
                    }
                    filesWithoutGPS.add(Pair(file, metadata))
                } else {
                    metadata.remarks = "Valid GPS found"
                    filesWithGPS.add(Pair(file, metadata))
                }
                
                fileMetadataList.add(metadata)
            }
            
            // Create timestamp for folders
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            
            // Write database
            _status.emit("Writing database...")
            val csvWriter = CSVDatabaseWriter(destinationFolder)
            val dbPath = csvWriter.writeDatabase(fileMetadataList)
            databasePath = dbPath
            _progress.emit("Database saved: $dbPath")
            
            var movedCount = 0
            var keptCount = filesWithGPS.size  // Files with GPS stay in source
            
            // FILES WITH GPS: Stay in original location (DO NOT MOVE)
            if (filesWithGPS.isNotEmpty()) {
                _status.emit("Files with GPS will remain in source location")
                _progress.emit("${filesWithGPS.size} files with valid GPS staying in source")
            }
            
            // FILES WITHOUT GPS: Move/Copy to destination NOGPS folder
            if (filesWithoutGPS.isNotEmpty() && fileAction != FileAction.NOTHING) {
                _status.emit("Moving files without GPS to destination...")
                val destDir = File(destinationFolder)
                val noGpsFolder = File(destDir, "NOGPS_$timestamp")
                noGpsFolder.mkdirs()
                val fileHandler = FileHandler
                
                filesWithoutGPS.forEachIndexed { index, (file, _) ->
                    _progress.emit("${if (fileAction == FileAction.MOVE) "Moving" else "Copying"} ${index + 1}/${filesWithoutGPS.size}")
                    
                    when (fileAction) {
                        FileAction.MOVE -> {
                            fileHandler.moveFile(file, noGpsFolder)
                            movedCount++
                        }
                        FileAction.COPY -> {
                            fileHandler.copyFile(file, noGpsFolder)
                            movedCount++
                        }
                        else -> {}
                    }
                }
            }
            
            _statistics.emit(Statistics(
                total = totalFiles,
                moved = movedCount,
                kept = keptCount,
                tagged = 0
            ))
            
            _status.emit("Database build complete!")
            _progress.emit("Total: $totalFiles | With GPS (kept): $keptCount | Without GPS (moved): $movedCount")
            
        } catch (e: Exception) {
            _status.emit("Error: ${e.message}")
            _progress.emit("")
        }
    }
    
    suspend fun startAutoTag(destinationFolder: String) = withContext(Dispatchers.IO) {
        try {
            _status.emit("Starting AutoTag...")
            
            if (databasePath == null) {
                _status.emit("Error: No database found. Build database first.")
                return@withContext
            }
            
            // Find NOGPS folder
            val destDir = File(destinationFolder)
            val noGpsFolders = destDir.listFiles { file ->
                file.isDirectory && file.name.startsWith("NOGPS_")
            }?.sortedByDescending { it.name }
            
            if (noGpsFolders.isNullOrEmpty()) {
                _status.emit("Error: No NOGPS folder found. Make sure you selected Move or Copy action.")
                _progress.emit("AutoTag requires files in NOGPS folder. If you selected 'Log', files weren't moved.")
                return@withContext
            }
            
            val noGpsFolder = noGpsFolders.first()
            _progress.emit("Processing folder: ${noGpsFolder.name}")
            
            // Check if folder has files
            val filesInFolder = FileScanner.scanForMediaFiles(noGpsFolder)
            if (filesInFolder.isEmpty()) {
                _status.emit("Error: NOGPS folder is empty")
                _progress.emit("No files to process in ${noGpsFolder.name}")
                return@withContext
            }
            
            _progress.emit("Found ${filesInFolder.size} files in NOGPS folder")
            
            val database = CSVDatabaseReader.readDatabase(databasePath!!)
            
            // Filter valid GPS donors: only photos with valid GPS
            val validDonors = database.filter { 
                it.hasGPS && it.isPhoto // Videos cannot be donors
            }
            
            if (validDonors.isEmpty()) {
                _status.emit("Error: No valid GPS donors found in database")
                _progress.emit("Database has no photos with valid GPS coordinates")
                return@withContext
            }
            
            _progress.emit("Loaded ${validDonors.size} valid photo donors (videos excluded)")
            
            // Create AutoTagged folder INSIDE NOGPS folder's parent (destination)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val autoTaggedFolder = File(noGpsFolder.parentFile, "AutoTagged_$timestamp")
            autoTaggedFolder.mkdirs()
            _progress.emit("Created AutoTagged folder: ${autoTaggedFolder.name}")
            
            // Process files in NOGPS folder
            val noGpsFiles = FileScanner.scanForMediaFiles(noGpsFolder)
            var tagged = 0
            var noMatch = 0
            
            noGpsFiles.forEachIndexed { index, file ->
                _progress.emit("Processing ${index + 1}/${noGpsFiles.size}: ${file.name}")
                
                val metadataExtractor = MetadataExtractor()
                val fileMetadata = metadataExtractor.extractMetadata(file)
                
                if (fileMetadata.timestamp != null) {
                    val match = AutoTagMatcher.findBestMatch(
                        fileMetadata,
                        validDonors,
                        timeWindowMinutes
                    )
                    
                    if (match != null) {
                        // Write GPS to file
                        MetadataWriter.writeGPSData(
                            file,
                            match.gpsLatitude,
                            match.gpsLongitude,
                            match.gpsAltitude
                        )
                        
                        // Move to AutoTagged folder
                        val destFile = File(autoTaggedFolder, file.name)
                        file.renameTo(destFile)
                        
                        // Update remarks - just filename without prefix
                        fileMetadata.remarks = match.fileName
                        tagged++
                        _progress.emit("Tagged: ${file.name} from ${match.fileName}")
                    } else {
                        fileMetadata.remarks = "No match found within ±${timeWindowMinutes}min"
                        noMatch++
                    }
                } else {
                    fileMetadata.remarks = "No timestamp - cannot AutoTag"
                    noMatch++
                }
                
                // Update database with remarks
                database.find { it.fileName == file.name }?.remarks = fileMetadata.remarks
            }
            
            // Save updated database with remarks
            if (databasePath != null) {
                CSVDatabaseWriter(destinationFolder).writeDatabase(database)
            }
            
            // Update statistics
            val currentStats = _statistics.value
            _statistics.emit(currentStats.copy(tagged = tagged))
            
            _status.emit("AutoTag complete!")
            _progress.emit("Tagged: $tagged (moved to AutoTagged_$timestamp) | No match: $noMatch | Total: ${noGpsFiles.size}")
            
        } catch (e: Exception) {
            _status.emit("Error: ${e.message}")
            _progress.emit("AutoTag failed: ${e.localizedMessage}")
        }
    }
}
