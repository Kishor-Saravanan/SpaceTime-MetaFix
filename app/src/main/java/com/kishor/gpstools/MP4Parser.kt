package com.kishor.gpstools

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.charset.Charset

/**
 * MP4 file structure parser
 * Finds atoms (boxes) in MP4/MOV/M4V files
 */
class MP4Parser {
    
    data class AtomInfo(
        val type: String,
        val offset: Long,
        val size: Long
    )
    
    /**
     * Find moov atom (main container for metadata)
     */
    fun findMoov(file: RandomAccessFile): AtomInfo? {
        return findAtomInFile(file, "moov", 0, file.length())
    }
    
    /**
     * Find udta atom inside moov (user data container)
     */
    fun findUdta(file: RandomAccessFile, moovInfo: AtomInfo): AtomInfo? {
        val searchStart = moovInfo.offset + 8  // Skip moov header
        val searchEnd = moovInfo.offset + moovInfo.size
        return findAtomInFile(file, "udta", searchStart, searchEnd)
    }
    
    /**
     * Find ©xyz atom inside udta (GPS location data)
     */
    fun findXyz(file: RandomAccessFile, udtaInfo: AtomInfo): AtomInfo? {
        val searchStart = udtaInfo.offset + 8  // Skip udta header
        val searchEnd = udtaInfo.offset + udtaInfo.size
        
        // ©xyz is encoded as 0xA9 0x78 0x79 0x7A
        return findAtomInFile(file, "©xyz", searchStart, searchEnd)
    }
    
    /**
     * Generic atom finder
     */
    private fun findAtomInFile(
        file: RandomAccessFile,
        atomType: String,
        startOffset: Long,
        endOffset: Long
    ): AtomInfo? {
        file.seek(startOffset)
        
        while (file.filePointer < endOffset) {
            val currentOffset = file.filePointer
            
            // Read atom header (8 bytes: 4 size + 4 type)
            if (file.filePointer + 8 > endOffset) break
            
            val sizeBytes = ByteArray(4)
            file.read(sizeBytes)
            val size = ByteBuffer.wrap(sizeBytes).int.toLong()
            
            val typeBytes = ByteArray(4)
            file.read(typeBytes)
            val type = String(typeBytes, Charset.forName("ISO-8859-1"))
            
            // Check if this is the atom we're looking for
            if (type == atomType || matchesXyzAtom(typeBytes, atomType)) {
                return AtomInfo(type, currentOffset, size)
            }
            
            // Skip to next atom
            if (size > 8 && currentOffset + size <= endOffset) {
                file.seek(currentOffset + size)
            } else {
                break
            }
        }
        
        return null
    }
    
    /**
     * Check if atom type matches ©xyz (special encoding)
     */
    private fun matchesXyzAtom(typeBytes: ByteArray, searchType: String): Boolean {
        if (searchType != "©xyz") return false
        
        // ©xyz is 0xA9 0x78 0x79 0x7A
        return typeBytes.size == 4 &&
               typeBytes[0] == 0xA9.toByte() &&
               typeBytes[1] == 0x78.toByte() &&
               typeBytes[2] == 0x79.toByte() &&
               typeBytes[3] == 0x7A.toByte()
    }
}
