package net.opendasharchive.openarchive.services.storacha.util

import java.security.MessageDigest

object CarUtils {
    
    fun extractCarCid(carData: ByteArray): String {
        // CAR CID is a "bag..." CID (multicodec 0x0202 for CAR)
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(carData)
        
        // Create multihash: 0x12 (SHA-256) + 0x20 (32 bytes) + hash
        val multiHash = byteArrayOf(0x12, 0x20) + hash
        
        // Create CID: version(1) + codec(0x0202 for CAR) + multihash
        // CAR multicodec is 0x0202, which encodes as [0x82, 0x04] in varint
        val cidBytes = byteArrayOf(0x01.toByte(), 0x82.toByte(), 0x04.toByte()) + multiHash
        
        return encodeCidAsString(cidBytes, "bag")
    }
    
    fun extractRootCid(carData: ByteArray): String {
        // Parse CAR header to extract root CID
        // This is a simplified version - in practice, you'd need full CAR parsing
        try {
            var offset = 0
            
            // Read header length (varint)
            val (headerLength, newOffset) = readVarInt(carData, offset)
            offset = newOffset
            
            // Skip to CBOR map parsing for roots array
            // This is simplified - actual CBOR parsing would be more complex
            val headerEnd = offset + headerLength
            
            // Look for the root CID in the CBOR structure
            // For now, return a placeholder that would come from proper CAR parsing
            return extractRootFromCborHeader(carData, offset, headerEnd)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid CAR file format", e)
        }
    }
    
    private fun extractRootFromCborHeader(data: ByteArray, start: Int, end: Int): String {
        // This is a simplified implementation
        // In practice, you'd need full CBOR parsing to extract the root CID
        // For now, we'll create a root CID from the first block's data
        
        var offset = end
        if (offset >= data.size) return ""
        
        // Read first block length
        val (blockLength, newOffset) = readVarInt(data, offset)
        offset = newOffset
        
        // First part of block is the CID
        if (offset + 36 > data.size) return ""
        
        val cidBytes = data.sliceArray(offset until offset + 36)
        return encodeCidAsString(cidBytes, "bafy")
    }
    
    private fun readVarInt(data: ByteArray, offset: Int): Pair<Int, Int> {
        var result = 0
        var shift = 0
        var currentOffset = offset
        
        while (currentOffset < data.size) {
            val byte = data[currentOffset].toInt() and 0xFF
            result = result or ((byte and 0x7F) shl shift)
            currentOffset++
            
            if ((byte and 0x80) == 0) {
                break
            }
            shift += 7
        }
        
        return Pair(result, currentOffset)
    }
    
    private fun encodeVarInt(value: Int): ByteArray {
        val result = mutableListOf<Byte>()
        var v = value
        while (v >= 0x80) {
            result.add(((v and 0x7F) or 0x80).toByte())
            v = v ushr 7
        }
        result.add(v.toByte())
        return result.toByteArray()
    }
    
    private fun encodeCidAsString(cidBytes: ByteArray, prefix: String): String {
        // Implement proper base32 encoding for CIDs
        return "$prefix${encodeBase32(cidBytes)}"
    }
    
    private fun encodeBase32(data: ByteArray): String {
        val alphabet = "abcdefghijklmnopqrstuvwxyz234567"
        val result = StringBuilder()
        
        var buffer = 0L
        var bitsLeft = 0
        
        for (byte in data) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF).toLong()
            bitsLeft += 8
            
            while (bitsLeft >= 5) {
                result.append(alphabet[((buffer shr (bitsLeft - 5)) and 0x1F).toInt()])
                bitsLeft -= 5
            }
        }
        
        if (bitsLeft > 0) {
            result.append(alphabet[((buffer shl (5 - bitsLeft)) and 0x1F).toInt()])
        }
        
        return result.toString()
    }
}