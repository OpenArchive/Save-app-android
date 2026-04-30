package net.opendasharchive.openarchive.services.snowbird.util

import net.opendasharchive.openarchive.extensions.urlEncode
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object SnowbirdJoinCode {
    private const val NAME_SEPARATOR = "&name="

    fun build(groupUri: String, groupName: String): String {
        val baseUri = groupUri.trim().substringBefore(NAME_SEPARATOR)
        if (baseUri.isBlank()) return ""
        return "$baseUri$NAME_SEPARATOR${groupName.urlEncode()}"
    }

    fun extractGroupName(code: String): String? {
        val value = code.trim()
        val index = value.indexOf(NAME_SEPARATOR)
        if (index == -1) return null

        val encodedName = value.substring(index + NAME_SEPARATOR.length)
        if (encodedName.isBlank()) return null

        return URLDecoder.decode(encodedName, StandardCharsets.UTF_8.toString())
    }
}

