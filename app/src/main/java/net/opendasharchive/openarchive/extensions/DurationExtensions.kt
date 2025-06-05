package net.opendasharchive.openarchive.extensions

import kotlin.math.pow
import kotlin.math.round
import kotlin.time.Duration

/**
 * Formats a Duration to a string with a specified number of decimal places.
 *
 * @param decimals The number of decimal places to round to (default is 1).
 * @return A formatted string representation of the duration in seconds, rounded to the specified number of decimal places.
 */
fun Duration.formatToDecimalPlaces(decimals: Int = 1): String {
    require(decimals >= 0) { "Number of decimal places must be non-negative" }

    val seconds = this.inWholeNanoseconds / 1e9
    val factor = 10.0.pow(decimals)
    val roundedSeconds = round(seconds * factor) / factor

    return "%.${decimals}f".format(roundedSeconds)
}