package net.opendasharchive.openarchive.services.storacha.util

import net.opendasharchive.openarchive.services.storacha.model.IpldBlock
import java.io.File

object CarFileCreator {
    fun createCarFile(file: File): ByteArray {
        val fileData = file.readBytes()
        val block = createIpldBlock(fileData)
        return createCar(block.cid, listOf(block))
    }

    private fun createIpldBlock(data: ByteArray): IpldBlock {
        throw NotImplementedError("Use ipfs-car-kotlin or implement IPLD block structure.")
    }

    private fun createCar(rootCid: String, blocks: List<IpldBlock>): ByteArray {
        throw NotImplementedError("Use ipfs-car-kotlin or encode a CAR file manually.")
    }
}