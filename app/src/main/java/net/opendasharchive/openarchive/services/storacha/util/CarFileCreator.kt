package net.opendasharchive.openarchive.services.storacha.util

import net.opendasharchive.openarchive.services.storacha.model.IpldBlock
import java.io.File
import java.security.MessageDigest
import java.util.Base64

object CarFileCreator {
    fun createCarFile(file: File): ByteArray {
        val fileData = file.readBytes()
        val block = createIPLDBlock(fileData)
        return createCar(block.cid, listOf(block))
    }

    private fun createIPLDBlock(data: ByteArray): IpldBlock {
        // Step 1: SHA-256 hash
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)

        // Step 2: Prepend multiHash prefix for sha2-256 (0x12) and length (32)
        val multiHash = byteArrayOf(0x12, 0x20) + hash

        // Step 3: Prepend CIDv1 prefix: 0x01 (CIDv1) + 0x55 (raw)
        val cidBytes = byteArrayOf(0x01, 0x55) + multiHash

        // Step 4: Base32 encode (lowercase) the CID bytes
        val cid =
            Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(cidBytes)
                .lowercase()

        return IpldBlock(cid, data)
    }

    private fun createCar(
        rootCid: String,
        blocks: List<IpldBlock>,
    ): ByteArray {
        val output = mutableListOf<Byte>()

        // CAR v1 Header
        val header = """{"version":1,"roots":["$rootCid"]}""".toByteArray()
        output.addAll(encodeVarInt(header.size).toList())
        output.addAll(header.toList())

        for (block in blocks) {
            val cidBytes = block.cid.toByteArray() // Simplified CID encoding
            val blockData = block.data

            val blockSize = cidBytes.size + blockData.size
            output.addAll(encodeVarInt(blockSize).toList())
            output.addAll(cidBytes.toList())
            output.addAll(blockData.toList())
        }

        return output.toByteArray()
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
}
