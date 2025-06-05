package net.opendasharchive.openarchive.services.snowbird.service

class HttpLikeException(val code: Int) : Exception("HTTP $code")