package com.kishor.gpstools

import androidx.lifecycle.AndroidViewModel
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

class MetadataViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    
    private val _status = MutableStateFlow("Ready")
    val status: StateFlow<String> = _status
    
    private val _progress = MutableStateFlow("Select folders to begin")
    val progress: StateFlow<String> = _progress
    
    private val _statistics = MutableStateFlow(Statistics())
    val statistics: StateFlow<Statistics> = _statistics
    
    private var databasePath: String? = null
    var timeWindowMinutes: Int = 15
    
    @Volatile
    var isCancelled: Boolean = false
    
    private val context = application.applicationContext
    
    fun setTimeWindow(minutes: Int) {
        timeWindowMinutes = minutes
    }
    
    fun cancelOperation() {
        isCancelled = true
        _status.value = "Operation cancelled by user"
        _progress.value = "Check database file for processed files"
    }
    
    suspend fun buildDatabase(
        sourceFolders: List<String>,
        destinationFolder: String,
        fileAction: FileAction,
        includeSubfolders: Boolean = false
    ) = withContext(Dispatchers.IO) {
        try {
            isCancelled = false
            _status.emit("Starting database build...")
            _statistics.emit(Statistics())
            
            // Scan all files
            _progress.emit("Scanning folders...")
            val allFiles = mutableListOf<File>()
            
            sourceFolders.forEach { folder ->
                if (isCancelled) return@withContext
                val files = FileScanner.scanForMediaFiles(File(folder), includeSubfolders)
                allFiles.addAll(files)
                _progress.emit("Found ${allFiles.size} files so far...")
            }
            
            if (isCancelled) return@withContext
            
            val totalFiles = allFiles.size
            _progress.emit("Found $totalFiles total files")
            _statistics.emit(Statistics(total = totalFiles))
            
            // Extract metadata
            val metadataExtractor = MetadataExtractor()
            val fileMetadataList = mutableListOf<FileMetadata>()
            val filesWithGPS = mutableListOf<Pair<File, FileMetadata>>()
            val filesWithoutGPS = mutableListOf<Pair<File, FileMetadata>>()
            var filesProcessed = 0
            
            allFiles.forEach { file ->
                if (isCancelled) {
                    _progress.emit("Stopped at file ${filesProcessed + 1}/$totalFiles")
                    return@withContext
                }
                
                filesProcessed++
                
                // Update UI every 5 files for performance
                if (filesProcessed % 5 == 0 || filesProcessed == totalFiles) {
                    val location = file.parent ?: "Unknown"
                    _progress.emit("Processing $filesProcessed/$totalFiles: ${file.name}\n📍 $location")
                }
                
                val metadata = metadataExtractor.extractMetadata(file)
                
                // Simplified remarks
                if (!metadata.hasGPS) {
                    metadata.remarks = "Recipient"
                    filesWithoutGPS.add(Pair(file, metadata))
                } else {
                    metadata.remarks = "Donor"
                    filesWithGPS.add(Pair(file, metadata))
                }
                
                fileMetadataList.add(metadata)
            }
            
            if (isCancelled) {
                // Save partial database
                val csvWriter = CSVDatabaseWriter(destinationFolder)
                databasePath = csvWriter.writeDatabase(fileMetadataList)
                _progress.emit("Partial database saved: $databasePath")
                return@withContext
            }
            
            // Create timestamp for folders
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            
            // Write database - SINGLE FILE
            _status.emit("Writing database...")
            val csvWriter = CSVDatabaseWriter(destinationFolder)
            val dbPath = csvWriter.writeDatabase(fileMetadataList)
            databasePath = dbPath
            _progress.emit("Database saved: $dbPath")
            
            var movedCount = 0
            var keptCount = filesWithGPS.size  // Files with GPS stay in source
            
            // FILES WITH GPS: Stay in original location
            if (filesWithGPS.isNotEmpty()) {
                _status.emit("Files with GPS remain in source location")
                _progress.emit("${filesWithGPS.size} files with valid GPS staying in source")
            }
            
            // FILES WITHOUT GPS: Move/Copy to destination NOGPS folder
            if (filesWithoutGPS.isNotEmpty() && fileAction != FileAction.NOTHING) {
                if (isCancelled) return@withContext
                
                val actionText = when (fileAction) {
                    FileAction.MOVE -> "Moving"
                    FileAction.COPY -> "Copying"
                    else -> "Processing"
                }
                
                _status.emit("$actionText files without GPS to destination...")
                val destDir = File(destinationFolder)
                val noGpsFolder = File(destDir, "NOGPS_$timestamp")
                noGpsFolder.mkdirs()
                val fileHandler = FileHandler
                
                filesWithoutGPS.forEachIndexed { index, (file, _) ->
                    if (isCancelled) return@withContext
                    
                    // Update every 5 files
                    if (index % 5 == 0 || index == filesWithoutGPS.size - 1) {
                        _progress.emit("$actionText ${index + 1}/${filesWithoutGPS.size}")
                    }
                    
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
                    
                    // Update stats every 5 files
                    if (index % 5 == 0) {
                        _statistics.emit(Statistics(
                            total = totalFiles,
                            moved = keptCount,
                            kept = movedCount,
                            tagged = 0
                        ))
                    }
                }
            }
            
            _statistics.emit(Statistics(
                total = totalFiles,
                moved = keptCount,
                kept = movedCount,
                tagged = 0
            ))
            
            _status.emit("Database build complete!")
            _progress.emit("Total: $totalFiles | With GPS: $keptCount | Without GPS: $movedCount")
            
        } catch (e: Exception) {
            _status.emit("Error: ${e.message}")
            _progress.emit("Check log for details")
        }
    }
    
    suspend fun startAutoTag(destinationFolder: String) = withContext(Dispatchers.IO) {
        try {
            if (databasePath == null) {
                _status.emit("Error: No database found. Build database first.")
                return@withContext
            }
            
            _status.emit("Starting AutoTag...")
            
            // Find NOGPS folder
            val destDir = File(destinationFolder)
            val noGpsFolders = destDir.listFiles { file ->
                file.isDirectory && file.name.startsWith("NOGPS_")
            }?.sortedByDescending { it.name }
            
            if (noGpsFolders.isNullOrEmpty()) {
                _status.emit("No NOGPS folder found")
                return@withContext
            }
            
            val noGpsFolder = noGpsFolders.first()
            
            // Check if folder has files
            val filesInFolder = FileScanner.scanForMediaFiles(noGpsFolder)
            if (filesInFolder.isEmpty()) {
                _status.emit("NOGPS folder is empty")
                return@withContext
            }
            
            _progress.emit("Found ${filesInFolder.size} files in NOGPS folder")
            
            // Read existing database - SAME FILE
            val database = CSVDatabaseReader.readDatabase(databasePath!!).toMutableList()
            
            // Filter valid GPS donors: only photos with valid GPS
            val validDonors = database.filter { 
                it.hasGPS && it.isPhoto
            }
            
            if (validDonors.isEmpty()) {
                _status.emit("No valid GPS donors found")
                return@withContext
            }
            
            _progress.emit("Found ${validDonors.size} valid photo donors")
            
            // Create AutoTagged folder
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val autoTaggedFolder = File(noGpsFolder.parentFile, "AutoTagged_$timestamp")
            autoTaggedFolder.mkdirs()
            
            // Process files
            val noGpsFiles = FileScanner.scanForMediaFiles(noGpsFolder, false)
            var tagged = 0
            var noMatch = 0
            var failed = 0
            val metadataWriter = MetadataWriter(context)
            
            _progress.emit("Processing ${noGpsFiles.size} files from NOGPS folder...")
            
            noGpsFiles.forEachIndexed { index, file ->
                if (index % 5 == 0 || index == noGpsFiles.size - 1) {
                    _progress.emit("AutoTagging ${index + 1}/${noGpsFiles.size}: ${file.name}")
                }
                
                // Verify file exists and is readable
                if (!file.exists() || !file.canRead()) {
                    _progress.emit("✗ Cannot access ${file.name}")
                    failed++
                    return@forEachIndexed
                }
                
                val metadataExtractor = MetadataExtractor()
                val fileMetadata = metadataExtractor.extractMetadata(file)
                
                if (fileMetadata.timestamp != null) {
                    val match = AutoTagMatcher.findBestMatch(
                        fileMetadata,
                        validDonors,
                        timeWindowMinutes
                    )
                    
                    if (match != null) {
                        // Log the attempt
                        _progress.emit("Attempting to tag ${file.name} from ${match.fileName}")
                        
                        // NEW APPROACH: Create new file with GPS directly in AutoTagged folder
                        // Original file in NOGPS stays UNTOUCHED
                        val newFile = metadataWriter.writeGPSToNewFile(
                            sourceFile = file,
                            outputFolder = autoTaggedFolder,
                            latitude = match.gpsLatitude,
                            longitude = match.gpsLongitude,
                            altitude = match.gpsAltitude
                        )
                        
                        if (newFile != null) {
                            // Success! New file created in AutoTagged with GPS
                            val dbEntry = database.find { it.fileName == file.name }
                            if (dbEntry != null) {
                                dbEntry.remarks = match.fileName
                            }
                            tagged++
                            
                            if (tagged % 10 == 0) {
                                _progress.emit("✓ Tagged $tagged files so far...")
                            }
                        } else {
                            _progress.emit("✗ Failed to write GPS to ${file.name}")
                            val dbEntry = database.find { it.fileName == file.name }
                            if (dbEntry != null) {
                                dbEntry.remarks = "GPS write failed - ${match.fileName}"
                            }
                            failed++
                        }
                    } else {
                        val dbEntry = database.find { it.fileName == file.name }
                        if (dbEntry != null) {
                            dbEntry.remarks = "No match found within ±${timeWindowMinutes}min"
                        }
                        noMatch++
                    }
                } else {
                    val dbEntry = database.find { it.fileName == file.name }
                    if (dbEntry != null) {
                        dbEntry.remarks = "No timestamp - cannot AutoTag"
                    }
                    noMatch++
                }
                
                if (index % 10 == 0) {
                    val currentStats = _statistics.value
                    _statistics.emit(currentStats.copy(tagged = tagged))
                }
            }
            
            // Update SAME database file
            val originalDbFile = File(databasePath!!)
            CSVDatabaseWriter(destinationFolder).writeDatabaseToFile(database, originalDbFile)
            
            val currentStats = _statistics.value
            _statistics.emit(currentStats.copy(tagged = tagged))
            
            _status.emit("AutoTag complete!")
            _progress.emit("Tagged: $tagged | No match: $noMatch | Failed: $failed")
            
        } catch (e: Exception) {
            _status.emit("AutoTag error: ${e.message}")
        }
    }
}
