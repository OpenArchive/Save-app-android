package net.opendasharchive.openarchive.services.common.network

interface RequestListener {
    fun transferred(bytes: Long)
    fun continueUpload(): Boolean
    fun transferComplete()
}