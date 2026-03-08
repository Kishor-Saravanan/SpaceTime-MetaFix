package com.kishor.gpstools

import kotlin.math.abs

object AutoTagMatcher {
    
    fun findBestMatch(
        targetFile: FileMetadata,
        filesWithGps: List<FileMetadata>,
        timeWindowMinutes: Int
    ): FileMetadata? {
        
        val targetTimestamp = targetFile.timestamp ?: return null
        
        val timeWindowMillis = timeWindowMinutes * 60 * 1000L
        
        // Filter files within time window
        val candidates = filesWithGps.filter { candidate ->
            val candidateTimestamp = candidate.timestamp ?: return@filter false
            val timeDiff = abs(candidateTimestamp.time - targetTimestamp.time)
            timeDiff <= timeWindowMillis
        }
        
        if (candidates.isEmpty()) return null
        
        // Find closest match
        return candidates.minByOrNull { candidate ->
            val candidateTimestamp = candidate.timestamp!!
            abs(candidateTimestamp.time - targetTimestamp.time)
        }
    }
}
