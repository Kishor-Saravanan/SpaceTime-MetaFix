package com.kishor.gpstools

import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.charset.Charset

/**
 * Direct MP4 GPS writer - edits MP4 atom structure without re-encoding
 * Fast, preserves all metadata, timestamps, and orientation
 */
class MP4GpsWriter {
    
    private val parser = MP4Parser()
    
    /**
     * Write GPS coordinates directly to MP4 file
     * Returns true if successful
     */
    fun writeGPS(videoFile: File, lat: Double, lon: Double, alt: Double? = null): Boolean {
        val originalTime = videoFile.lastModified()
        
        try {
            RandomAccessFile(videoFile, "rw").use { file ->
                // Find moov atom
                val moovInfo = parser.findMoov(file)
                if (moovInfo == null) {
                    Log.e("MP4GpsWriter", "No moov atom found")
                    return false
                }
                
                // Find or create udta atom
                var udtaInfo = parser.findUdta(file, moovInfo)
                if (udtaInfo == null) {
                    Log.w("MP4GpsWriter", "No udta atom - creating one")
                    udtaInfo = createUdtaAtom(file, moovInfo)
                    if (udtaInfo == null) {
                        Log.e("MP4GpsWriter", "Failed to create udta")
                        return false
                    }
                }
                
                // Create GPS data
                val gpsString = formatISO6709(lat, lon, alt)
                val xyzAtom = createXyzAtom(gpsString)
                
                // Check if ©xyz already exists
                val xyzInfo = parser.findXyz(file, udtaInfo)
                
                if (xyzInfo != null && xyzInfo.size == xyzAtom.size.toLong()) {
                    // Same size - can update in place
                    updateXyzInPlace(file, xyzInfo, xyzAtom)
                } else {
                    // Different size or doesn't exist - append to udta
                    appendXyzToUdta(file, udtaInfo, xyzAtom)
                }
            }
            
            // Preserve original timestamp
            videoFile.setLastModified(originalTime)
            
            Log.i("MP4GpsWriter", "✓ GPS written to ${videoFile.name}")
            return true
            
        } catch (e: Exception) {
            Log.e("MP4GpsWriter", "Error writing GPS: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Format GPS coordinates in ISO 6709 format
     * Example: +25.1947+055.2833+010.0/
     */
    private fun formatISO6709(lat: Double, lon: Double, alt: Double?): String {
        val latStr = String.format("%+09.4f", lat)
        val lonStr = String.format("%+010.4f", lon)
        val altStr = if (alt != null) String.format("%+07.1f", alt) else "+000.0"
        return "$latStr$lonStr$altStr/"
    }
    
    /**
     * Create ©xyz atom with GPS data
     */
    private fun createXyzAtom(gpsString: String): ByteArray {
        val gpsBytes = gpsString.toByteArray(Charset.forName("UTF-8"))
        val atomSize = 4 + 4 + 4 + gpsBytes.size  // size + type + version + data
        
        val buffer = ByteBuffer.allocate(atomSize)
        buffer.putInt(atomSize)
        buffer.put(byteArrayOf(0xA9.toByte(), 0x78, 0x79, 0x7A))  // ©xyz
        buffer.putInt(0)  // version and flags
        buffer.put(gpsBytes)
        
        return buffer.array()
    }
    
    /**
     * Update existing ©xyz atom in place (same size)
     */
    private fun updateXyzInPlace(
        file: RandomAccessFile,
        xyzInfo: MP4Parser.AtomInfo,
        newXyzAtom: ByteArray
    ) {
        file.seek(xyzInfo.offset)
        file.write(newXyzAtom)
        Log.d("MP4GpsWriter", "Updated ©xyz in place")
    }
    
    /**
     * Append new ©xyz atom to udta
     */
    private fun appendXyzToUdta(
        file: RandomAccessFile,
        udtaInfo: MP4Parser.AtomInfo,
        xyzAtom: ByteArray
    ) {
        // Position at end of udta content
        val insertPos = udtaInfo.offset + udtaInfo.size
        
        // Read everything after udta
        file.seek(insertPos)
        val remaining = file.length() - insertPos
        val restOfFile = ByteArray(remaining.toInt())
        file.read(restOfFile)
        
        // Write ©xyz atom
        file.seek(insertPos)
        file.write(xyzAtom)
        
        // Write back the rest
        file.write(restOfFile)
        
        // Update udta size
        val newUdtaSize = udtaInfo.size + xyzAtom.size
        file.seek(udtaInfo.offset)
        file.writeInt(newUdtaSize.toInt())
        
        Log.d("MP4GpsWriter", "Appended ©xyz to udta")
    }
    
    /**
     * Create new udta atom inside moov
     */
    private fun createUdtaAtom(
        file: RandomAccessFile,
        moovInfo: MP4Parser.AtomInfo
    ): MP4Parser.AtomInfo? {
        // Simple udta: just header (8 bytes)
        val udtaSize = 8L
        val insertPos = moovInfo.offset + moovInfo.size
        
        // Read everything after moov
        file.seek(insertPos)
        val remaining = file.length() - insertPos
        val restOfFile = ByteArray(remaining.toInt())
        file.read(restOfFile)
        
        // Write udta header
        file.seek(insertPos)
        file.writeInt(udtaSize.toInt())
        file.write("udta".toByteArray(Charset.forName("UTF-8")))
        
        // Write back the rest
        file.write(restOfFile)
        
        // Update moov size
        val newMoovSize = moovInfo.size + udtaSize
        file.seek(moovInfo.offset)
        file.writeInt(newMoovSize.toInt())
        
        return MP4Parser.AtomInfo("udta", insertPos, udtaSize)
    }
}
