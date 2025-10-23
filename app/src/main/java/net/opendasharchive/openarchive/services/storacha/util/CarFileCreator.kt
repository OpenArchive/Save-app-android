package net.opendasharchive.openarchive.services.storacha.util

import net.opendasharchive.openarchive.services.storacha.model.IpldBlock
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

data class CarFileResult(
    val carFile: File,
    val carCid: String,
    val rootCid: String,
    val carSize: Long,
)

object CarFileCreator {
    private const val CHUNK_SIZE = 1048576 // 1MB chunks to match ipfs-car default

    fun createCarFile(file: File, outputDir: File? = null): CarFileResult {
        // ipfs-car uses embedded approach for files < 1MB, chunked approach for larger files
        return if (file.length() < CHUNK_SIZE) {
            createEmbeddedCarWithCids(file, outputDir)
        } else {
            createChunkedCarWithCids(file, outputDir)
        }
    }

    /**
     * Creates CAR file using embedded approach for small files (< 1MB) - like ipfs-car does
     */
    private fun createEmbeddedCarWithCids(file: File, outputDir: File?): CarFileResult {
        val blocks = mutableListOf<IpldBlock>()
        val fileData = file.readBytes()

        // Create raw block for the file data
        val rawBlock = createRawBlock(fileData)
        blocks.add(rawBlock)

        // Create single DAG-PB block that directly links to the raw block with filename
        // This is the "embedded" approach - no intermediate block
        val rootDagPbBlock = createEmbeddedDagPbBlock(rawBlock.cid, file.name, file.length())
        blocks.add(rootDagPbBlock)

        // Create CIDs in proper format
        val rootCid = cidBytesToString(rootDagPbBlock.cid)

        // Create temporary CAR file
        val carFile = File.createTempFile("car_", ".car", outputDir ?: file.parentFile)

        // Write CAR data directly to file
        writeCarToFile(carFile, rootDagPbBlock.cid, blocks)

        // Calculate CAR CID from the file
        val carCid = createCarCidFromFile(carFile)

        return CarFileResult(carFile, carCid, rootCid, carFile.length())
    }

    /**
     * Creates a DAG-PB block that directly links to the raw file block with filename (embedded approach)
     */
    private fun createEmbeddedDagPbBlock(
        rawCid: ByteArray,
        fileName: String,
        fileSize: Long,
    ): IpldBlock {
        // Create minimal UnixFS root node (directory-like)
        val unixfsData = byteArrayOf(0x08, 0x01) // type = directory (1)

        // Create link to raw block with filename
        val linkData = createPbLink(rawCid, fileName, fileSize)

        // Create DAG-PB protobuf structure: Link first, then Data (field 2, then field 1 - ipfs-car order)
        val pbData =
            ByteArrayOutputStream()
                .apply {
                    // Field 2: Link to raw block first
                    write(0x12) // field 2, wire type 2
                    write(encodeVarInt(linkData.size))
                    write(linkData)

                    // Field 1: Data (UnixFS metadata)
                    write(0x0A) // field 1, wire type 2
                    write(encodeVarInt(unixfsData.size))
                    write(unixfsData)
                }.toByteArray()

        // Create CID for the DAG-PB block
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pbData)
        val multiHash = byteArrayOf(0x12, 0x20) + hash
        val cidBytes = byteArrayOf(0x01.toByte(), 0x70.toByte()) + multiHash

        return IpldBlock(cidBytes, pbData)
    }

    private fun createChunkedCarWithCids(file: File, outputDir: File?): CarFileResult {
        val blocks = mutableListOf<IpldBlock>()
        val chunkCids = mutableListOf<ByteArray>()

        // Stream the file in chunks to avoid loading entire file into memory
        FileInputStream(file).use { inputStream ->
            val buffer = ByteArray(CHUNK_SIZE)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                // Only use the actual bytes read (might be less than buffer size for last chunk)
                val chunkData = if (bytesRead < buffer.size) {
                    buffer.copyOf(bytesRead)
                } else {
                    buffer.copyOf()
                }

                val rawBlock = createRawBlock(chunkData)
                blocks.add(rawBlock)
                chunkCids.add(rawBlock.cid)
            }
        }

        // Create intermediate DAG-PB block that links to raw chunks (like ipfs-car)
        val intermediateDagPbBlock =
            createIntermediateDagPbBlock(chunkCids, file.length())
        blocks.add(intermediateDagPbBlock)

        // Create root DAG-PB block that links to intermediate block with filename
        val rootDagPbBlock =
            createRootDagPbBlock(intermediateDagPbBlock.cid, file.name, file.length())
        blocks.add(rootDagPbBlock)

        // Create CIDs in proper format
        val rootCid = cidBytesToString(rootDagPbBlock.cid)

        // Create temporary CAR file
        val carFile = File.createTempFile("car_", ".car", outputDir ?: file.parentFile)

        // Write CAR data directly to file
        writeCarToFile(carFile, rootDagPbBlock.cid, blocks)

        // Calculate CAR CID from the file
        val carCid = createCarCidFromFile(carFile)

        return CarFileResult(carFile, carCid, rootCid, carFile.length())
    }

//    private fun createLargeFileCarWithCids(file: File, mimeType: String): CarFileResult {
//        val blocks = mutableListOf<IpldBlock>()
//        val chunkCids = mutableListOf<ByteArray>()
//
//        // Read file in chunks and create raw blocks
//        FileInputStream(file).use { inputStream ->
//            val buffer = ByteArray(CHUNK_SIZE)
//            var bytesRead: Int
//
//            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
//                val chunkData = if (bytesRead < CHUNK_SIZE) {
//                    buffer.copyOf(bytesRead)
//                } else {
//                    buffer.copyOf()
//                }
//
//                val rawBlock = createRawBlock(chunkData)
//                blocks.add(rawBlock)
//                chunkCids.add(rawBlock.cid)
//            }
//        }
//
//        // Create DAG-PB block that links to all chunks
//        val dagPbBlock = createChunkedDagPbBlock(chunkCids, file.name, file.length(), mimeType)
//        blocks.add(dagPbBlock)
//
//        // Create CAR with all blocks, header points to DAG-PB root
//        val carData = createCar(dagPbBlock.cid, blocks)
//
//        // Create CIDs in proper format
//        val rootCid = cidBytesToString(dagPbBlock.cid, "bafy")
//        val carCid = createCarCid(carData)
//
//        return CarFileResult(carData, carCid, rootCid)
//    }

    /**
     * Creates a raw IPLD block (codec 0x55) containing the file data
     */
    private fun createRawBlock(data: ByteArray): IpldBlock {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)

        // Create multihash: 0x12 (SHA-256) + 0x20 (32 bytes) + hash
        val multiHash = byteArrayOf(0x12, 0x20) + hash
        // Create CID v1: version(1) + codec(0x55 raw) + multihash
        val cidBytes = byteArrayOf(0x01.toByte(), 0x55.toByte()) + multiHash

        return IpldBlock(cidBytes, data)
    }

    /**
     * Creates an intermediate DAG-PB block that links to raw blocks (like ipfs-car intermediate block)
     */
    private fun createIntermediateDagPbBlock(
        chunkCids: List<ByteArray>,
        fileSize: Long,
    ): IpldBlock {
        // Create UnixFS metadata for intermediate block (matches ipfs-car structure)
        val unixfsData = createIntermediateUnixFsData(fileSize, chunkCids.size)

        // Create DAG-PB protobuf structure: Links first, then Data (field 2, then field 1 - ipfs-car order)
        val pbData =
            ByteArrayOutputStream()
                .apply {
                    // Field 2: Links to all chunks first - ipfs-car puts Links before Data in intermediate
                    chunkCids.forEachIndexed { index, chunkCid ->
                        write(0x12) // field 2, wire type 2 (length-delimited)
                        val chunkSize =
                            if (index == chunkCids.size - 1) {
                                val remainingSize = fileSize % CHUNK_SIZE
                                if (remainingSize == 0L) CHUNK_SIZE.toLong() else remainingSize
                            } else {
                                CHUNK_SIZE.toLong()
                            }
                        val linkData = createPbLink(chunkCid, "", chunkSize)
                        write(encodeVarInt(linkData.size))
                        write(linkData)
                    }

                    // Field 1: Data (UnixFS metadata only) - ipfs-car puts Data after Links in intermediate
                    write(0x0A) // field 1, wire type 2 (length-delimited)
                    write(encodeVarInt(unixfsData.size))
                    write(unixfsData)
                }.toByteArray()

        // Create CID for the intermediate DAG-PB block
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pbData)
        val multiHash = byteArrayOf(0x12, 0x20) + hash
        val cidBytes = byteArrayOf(0x01.toByte(), 0x70.toByte()) + multiHash

        return IpldBlock(cidBytes, pbData)
    }

    /**
     * Creates a root DAG-PB block that links to intermediate block with filename (like ipfs-car root)
     */
    private fun createRootDagPbBlock(
        intermediateCid: ByteArray,
        fileName: String,
        fileSize: Long,
    ): IpldBlock {
        // Create minimal UnixFS root node (directory-like)
        val unixfsData = byteArrayOf(0x08, 0x01) // type = directory (1)

        // ipfs-car reports total size including intermediate block overhead (108 bytes)
        val totalSize = fileSize + 108L // Add intermediate block overhead

        // Create link to intermediate block with filename
        val linkData = createPbLink(intermediateCid, fileName, totalSize)

        // Create DAG-PB protobuf structure: Link first, then Data (field 2, then field 1 - ipfs-car order)
        val pbData =
            ByteArrayOutputStream()
                .apply {
                    // Field 2: Link to intermediate block first
                    write(0x12) // field 2, wire type 2
                    write(encodeVarInt(linkData.size))
                    write(linkData)

                    // Field 1: Data (UnixFS directory metadata) after link
                    write(0x0A) // field 1, wire type 2
                    write(encodeVarInt(unixfsData.size))
                    write(unixfsData)
                }.toByteArray()

        // Create CID for the root DAG-PB block
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pbData)
        val multiHash = byteArrayOf(0x12, 0x20) + hash
        val cidBytes = byteArrayOf(0x01.toByte(), 0x70.toByte()) + multiHash

        return IpldBlock(cidBytes, pbData)
    }

    /**
     * Creates UnixFS data for intermediate DAG-PB blocks (matches ipfs-car structure)
     */
    private fun createIntermediateUnixFsData(
        fileSize: Long,
        chunkCount: Int,
    ): ByteArray {
        val output = ByteArrayOutputStream()

        // Field 1: Type (file = 2)
        output.write(0x08) // field 1, varint
        output.write(0x02) // file type = 2

        // Field 3: Total file size (ipfs-car puts file size in field 3)
        output.write(0x18) // field 3, varint
        output.write(encodeVarInt(fileSize.toInt()))

        // Field 4: Block sizes (ipfs-car puts block sizes in field 4)
        repeat(chunkCount - 1) {
            output.write(0x20) // field 4, varint (block sizes)
            output.write(encodeVarInt(CHUNK_SIZE))
        }
        // Last chunk might be smaller
        val lastChunkSize = (fileSize % CHUNK_SIZE).toInt()
        if (lastChunkSize > 0) {
            output.write(0x20)
            output.write(encodeVarInt(lastChunkSize))
        } else {
            output.write(0x20)
            output.write(encodeVarInt(CHUNK_SIZE))
        }

        return output.toByteArray()
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
    /**
     * Writes CAR data directly to a file using streaming to avoid memory issues
     */
    private fun writeCarToFile(
        outputFile: File,
        rootCid: ByteArray,
        blocks: List<IpldBlock>,
    ) {
        BufferedOutputStream(FileOutputStream(outputFile), 8192).use { output ->
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

            output.flush()
        }
    }

    /**
     * Calculates CAR CID from a file by streaming (avoids loading entire file into memory)
     */
    private fun createCarCidFromFile(carFile: File): String {
        val digest = MessageDigest.getInstance("SHA-256")

        // Stream the file in chunks to calculate hash
        FileInputStream(carFile).use { inputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        val hash = digest.digest()
        // Create multihash: 0x12 (SHA-256) + 0x20 (32 bytes) + hash
        val multiHash = byteArrayOf(0x12, 0x20) + hash

        // Create CID v1: version(1) + codec(CAR multicodec 0x0202) + multihash
        // CAR multicodec 0x0202 (514) encodes as varint [0x82, 0x04]
        val carCodecVarint = encodeVarInt(0x0202)
        val cidBytes = byteArrayOf(0x01.toByte()) + carCodecVarint + multiHash

        return cidBytesToString(cidBytes)
    }

    /**
     * Creates CBOR header with empty roots array and version (matches ipfs-car format)
     */
    private fun createCborHeader(rootCid: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()

        // a2 = CBOR map with 2 items
        output.write(0xA2)

        // 65 + "roots" = text string "roots" (length 5)
        output.write(0x65)
        output.write("roots".toByteArray())

        // 80 = CBOR empty array (ipfs-car uses empty roots)
        // output.write(0x80)

        // Roots array with 1 element
        output.write(0x81) // CBOR array of length 1
        output.write(0xD8) // CBOR tag
        output.write(0x2A) // Tag 42 for CID

        // Byte string with (1 + rootCid.size) length
        output.write(0x58)
        output.write(rootCid.size + 1) // include leading 0x00

        // Write the 0x00 prefix
        output.write(0x00)

        // Write the actual CID bytes
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

    /**
     * Convert CID bytes to proper CID string format
     */
    private fun cidBytesToString(cidBytes: ByteArray): String = "b${encodeBase32(cidBytes)}"


    /**
     * Encode data as base32 string (RFC 4648)
     */
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
