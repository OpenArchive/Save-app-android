package net.opendasharchive.openarchive.services.storacha.util

import net.opendasharchive.openarchive.services.storacha.model.IpldBlock
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object CarFileCreator {
    private const val CHUNK_SIZE = 262144 // 256KB chunks for large files
    private const val LARGE_FILE_THRESHOLD = 5L * 1024 * 1024 * 1024 // 5GB

    fun createCarFile(file: File): ByteArray {
        val mimeType = detectMimeType(file)
        val fileSize = file.length()
        
        return if (fileSize > LARGE_FILE_THRESHOLD) {
            createLargeFileCar(file, mimeType)
        } else {
            createSmallFileCar(file, mimeType)
        }
    }

    private fun createSmallFileCar(file: File, mimeType: String): ByteArray {
        val fileData = file.readBytes()

        // Create raw block (codec 0x55) containing file data
        val rawBlock = createRawBlock(fileData)

        // Create DAG-PB block (codec 0x70) that wraps the raw block
        val dagPbBlock = createDagPbBlock(rawBlock.cid, file.name, fileData.size.toLong(), mimeType)

        // Create CAR with both blocks, header points to DAG-PB root
        return createCar(dagPbBlock.cid, listOf(rawBlock, dagPbBlock))
    }

    private fun createLargeFileCar(file: File, mimeType: String): ByteArray {
        val blocks = mutableListOf<IpldBlock>()
        val chunkCids = mutableListOf<ByteArray>()
        
        // Read file in chunks and create raw blocks
        FileInputStream(file).use { inputStream ->
            val buffer = ByteArray(CHUNK_SIZE)
            var bytesRead: Int
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                val chunkData = if (bytesRead < CHUNK_SIZE) {
                    buffer.copyOf(bytesRead)
                } else {
                    buffer.copyOf()
                }
                
                val rawBlock = createRawBlock(chunkData)
                blocks.add(rawBlock)
                chunkCids.add(rawBlock.cid)
            }
        }
        
        // Create DAG-PB block that links to all chunks
        val dagPbBlock = createChunkedDagPbBlock(chunkCids, file.name, file.length(), mimeType)
        blocks.add(dagPbBlock)
        
        // Create CAR with all blocks, header points to DAG-PB root
        return createCar(dagPbBlock.cid, blocks)
    }

    /**
     * Creates a raw IPLD block (codec 0x55) containing the file data
     */
    private fun createRawBlock(data: ByteArray): IpldBlock {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)

        val multiHash = byteArrayOf(0x12, 0x20) + hash
        val cidBytes = byteArrayOf(0x01.toByte(), 0x55.toByte()) + multiHash

        return IpldBlock(cidBytes, data)
    }

    /**
     * Creates a DAG-PB block (codec 0x70) that contains UnixFS data linking to the raw block
     */
    private fun createDagPbBlock(
        rawBlockCid: ByteArray,
        fileName: String,
        fileSize: Long,
        mimeType: String = "application/octet-stream",
    ): IpldBlock {
        // Create protobuf-encoded DAG-PB data with UnixFS
        val pbData = createUnixFsPbData(rawBlockCid, fileName, fileSize, mimeType)

        // Create CID for the DAG-PB block
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pbData)
        val multiHash = byteArrayOf(0x12, 0x20) + hash
        val cidBytes = byteArrayOf(0x01.toByte(), 0x70.toByte()) + multiHash

        return IpldBlock(cidBytes, pbData)
    }

    /**
     * Creates a DAG-PB block for chunked files with multiple raw block links
     */
    private fun createChunkedDagPbBlock(
        chunkCids: List<ByteArray>,
        fileName: String,
        fileSize: Long,
        mimeType: String,
    ): IpldBlock {
        // Create protobuf-encoded DAG-PB data with UnixFS for chunked file
        val pbData = createChunkedUnixFsPbData(chunkCids, fileName, fileSize, mimeType)

        // Create CID for the DAG-PB block
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pbData)
        val multiHash = byteArrayOf(0x12, 0x20) + hash
        val cidBytes = byteArrayOf(0x01.toByte(), 0x70.toByte()) + multiHash

        return IpldBlock(cidBytes, pbData)
    }

    /**
     * Creates protobuf-encoded DAG-PB data with UnixFS metadata
     */
    private fun createUnixFsPbData(
        rawBlockCid: ByteArray,
        fileName: String,
        fileSize: Long,
        mimeType: String = "application/octet-stream",
    ): ByteArray {
        val output = ByteArrayOutputStream()

        // Field 2: Links array with one link to raw block (DAG-PB field 2 = Links)
        output.write(0x12) // field 2, wire type 2 (length-delimited)

        val linkData = createPbLink(rawBlockCid, fileName, fileSize)
        output.write(encodeVarInt(linkData.size))
        output.write(linkData)

        // Field 1: UnixFS data (DAG-PB field 1 = Data)
        val unixfsData = createUnixFsData(fileSize, mimeType)
        output.write(0x0A) // field 1, wire type 2 (length-delimited)
        output.write(encodeVarInt(unixfsData.size))
        output.write(unixfsData)

        return output.toByteArray()
    }

    /**
     * Creates protobuf-encoded DAG-PB data for chunked files
     */
    private fun createChunkedUnixFsPbData(
        chunkCids: List<ByteArray>,
        fileName: String,
        fileSize: Long,
        mimeType: String,
    ): ByteArray {
        val output = ByteArrayOutputStream()

        // Field 1: Links array with links to all chunks
        chunkCids.forEachIndexed { index, chunkCid ->
            output.write(0x12) // field 1, wire type 2 (length-delimited)
            val linkData = createPbLink(chunkCid, "", CHUNK_SIZE.toLong()) // Empty name for chunks
            output.write(encodeVarInt(linkData.size))
            output.write(linkData)
        }

        // Field 2: UnixFS data with MIME type and block sizes
        val unixfsData = createChunkedUnixFsData(fileSize, mimeType, chunkCids.size)
        output.write(0x12) // field 2, wire type 2 (length-delimited)
        output.write(encodeVarInt(unixfsData.size))
        output.write(unixfsData)

        return output.toByteArray()
    }

    /**
     * Creates UnixFS data structure with MIME type
     */
    private fun createUnixFsData(fileSize: Long, mimeType: String): ByteArray {
        val output = ByteArrayOutputStream()

        // Field 1: Type (file = 1 for UnixFS file type)
        output.write(0x08) // field 1, varint
        output.write(0x01) // file type = 1 (matches working IPFS implementation)

        return output.toByteArray()
    }

    /**
     * Creates UnixFS data for chunked files
     */
    private fun createChunkedUnixFsData(fileSize: Long, mimeType: String, chunkCount: Int): ByteArray {
        val output = ByteArrayOutputStream()

        // Field 1: Type (file = 2)
        output.write(0x08) // field 1, varint
        output.write(0x02) // file type = 2

        // Field 2: File size
        output.write(0x10) // field 2, varint
        output.write(encodeVarInt(fileSize.toInt()))

        // Field 3: Block sizes (for each chunk)
        repeat(chunkCount - 1) {
            output.write(0x18) // field 3, varint (block sizes)
            output.write(encodeVarInt(CHUNK_SIZE))
        }
        // Last chunk might be smaller
        val lastChunkSize = (fileSize % CHUNK_SIZE).toInt()
        if (lastChunkSize > 0) {
            output.write(0x18)
            output.write(encodeVarInt(lastChunkSize))
        } else {
            output.write(0x18)
            output.write(encodeVarInt(CHUNK_SIZE))
        }

        return output.toByteArray()
    }

    /**
     * Detects MIME type based on file extension
     */
    private fun detectMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "mov" -> "video/quicktime"
            "avi" -> "video/x-msvideo"
            "webm" -> "video/webm"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "m4a" -> "audio/mp4"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "json" -> "application/json"
            "xml" -> "application/xml"
            else -> "application/octet-stream"
        }
    }

    /**
     * Creates a protobuf link to the raw block
     */
    private fun createPbLink(
        cid: ByteArray,
        name: String,
        size: Long,
    ): ByteArray {
        val output = ByteArrayOutputStream()

        // Field 1: CID (Hash field)
        output.write(0x0A) // field 1, wire type 2
        output.write(encodeVarInt(cid.size))
        output.write(cid)

        // Field 2: Name
        output.write(0x12) // field 2, wire type 2
        val nameBytes = name.toByteArray()
        output.write(encodeVarInt(nameBytes.size))
        output.write(nameBytes)

        // Field 3: Size
        output.write(0x18) // field 3, wire type 0 (varint)
        output.write(encodeVarInt(size.toInt()))

        return output.toByteArray()
    }

    /**
     * Creates CAR file with header and blocks
     */
    private fun createCar(
        rootCid: ByteArray,
        blocks: List<IpldBlock>,
    ): ByteArray {
        val output = ByteArrayOutputStream()

        // Create header pointing to root CID
        val headerData = createCborHeader(rootCid)

        // Write header with varint length prefix
        output.write(encodeVarInt(headerData.size))
        output.write(headerData)

        // Write all blocks with varint length prefixes
        for (block in blocks) {
            val blockSize = block.cid.size + block.data.size
            output.write(encodeVarInt(blockSize))
            output.write(block.cid)
            output.write(block.data)
        }

        return output.toByteArray()
    }

    /**
     * Creates CBOR header with roots array and version
     */
    private fun createCborHeader(rootCid: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()

        // a2 = CBOR map with 2 items
        output.write(0xA2)

        // 65 + "roots" = text string "roots" (length 5)
        output.write(0x65)
        output.write("roots".toByteArray())

        // 81 = CBOR array with 1 item
        output.write(0x81)

        // d82a = CBOR tag 42 (CID tag)
        output.write(0xD8)
        output.write(0x2A)

        // 5825 = byte string with length 37 (0x25)
        output.write(0x58)
        output.write(0x25) // 37 = 1 (multibase) + 36 (CID bytes)

        // 00 = multibase identity prefix
        output.write(0x00)

        // CID bytes (36 bytes: 1 + 1 + 2 + 32)
        output.write(rootCid)

        // 67 + "version" = text string "version" (length 7)
        output.write(0x67)
        output.write("version".toByteArray())

        // 01 = integer 1
        output.write(0x01)

        return output.toByteArray()
    }

    /**
     * Encodes integers as LEB128 varint
     */
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
}
