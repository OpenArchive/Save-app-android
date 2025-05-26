package org.openarchive

object SaveTor {
    external fun start(storage: String)

    init {
        System.loadLibrary("arti_mobile")
    }
}