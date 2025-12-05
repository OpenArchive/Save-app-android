package net.opendasharchive.openarchive.services.snowbird.service

class HttpLikeException(val code: Int, override val message: String) : Exception("HTTP $code")