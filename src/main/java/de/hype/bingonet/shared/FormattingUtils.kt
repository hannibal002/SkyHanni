package de.hype.bingonet.shared

import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.util.*

object FormattingUtils {
    /**
     * This shortens the amount to numbers like 10k or 10B etc.
     *
     * @param amount The amount to format
     */
    @JvmStatic
    fun formatAmountShortened(amount: Double): String {
        val df = DecimalFormat("0.0")
        if (amount >= 1000000000000L) {
            return df.format(amount / 1000000000000.0) + "T"
        } else if (amount >= 1000000000) {
            return df.format(amount / 1000000000.0) + "B"
        } else if (amount >= 1000000) {
            return df.format(amount / 1000000.0) + "M"
        } else if (amount >= 1000) {
            return df.format(amount / 1000.0) + "k"
        } else {
            return amount.toString()
        }
    }

    @JvmStatic
    fun formatAmountShortened(amount: Int): String {
        val df = DecimalFormat("0.0")
        if (amount >= 1000000000) {
            return df.format(amount / 1000000000.0) + "B"
        } else if (amount >= 1000000) {
            return df.format(amount / 1000000.0) + "M"
        } else if (amount >= 1000) {
            return df.format(amount / 1000.0) + "k"
        } else {
            return amount.toString()
        }
    }

    fun formatAmountShortened(amount: Int?): String? {
        if (amount == null) return null
        return formatAmountShortened(amount.toLong())
    }

    /**
     * This shortens the amount to numbers like 10k or 10B etc.
     *
     * @param amount The amount to format
     */
    @JvmStatic
    fun formatAmountShortened(amount: Long): String {
        val df = DecimalFormat("0.0")
        if (amount >= 1000000000000L) {
            return df.format(amount / 1000000000000.0) + "T"
        } else if (amount >= 1000000000) {
            return df.format(amount / 1000000000.0) + "B"
        } else if (amount >= 1000000) {
            return df.format(amount / 1000000.0) + "M"
        } else if (amount >= 1000) {
            return df.format(amount / 1000.0) + "k"
        } else {
            return amount.toString()
        }
    }

    /**
     * Returns the number as a String like '1st', '2nd', '3rd', '4th', '5th', etc.
     *
     * @param number The number to convert to an ordinal
     */
    @JvmStatic
    fun toOrdinal(number: Int): String {
        if (number <= 0) {
            return number.toString() // Handle non-positive numbers
        }

        val suffix: String
        val mod100 = number % 100
        val mod10 = number % 10

        if (mod100 >= 11 && mod100 <= 13) {
            suffix = "th" // Special case for 11th, 12th, 13th
        } else {
            when (mod10) {
                1 -> suffix = "st"
                2 -> suffix = "nd"
                3 -> suffix = "rd"
                else -> suffix = "th"
            }
        }

        return number.toString() + suffix
    }

    fun formatTime(time: Instant): String {
        return formatTime(Duration.between(Instant.now(), time))
    }

    fun formatAmount(amount: Long): String {
        return NumberFormat.getInstance(Locale.US).format(amount)
    }

    @JvmStatic
    fun formatTime(src: Duration): String {
        val seconds = src.seconds
        if (seconds == 0L) return "now"
        val prefix = if (seconds > 0) "in %s" else "%s ago"
        val days = (seconds / 86400).toInt()
        val hours = ((seconds % 86400) / 3600).toInt()
        val minutes = ((seconds % 3600) / 60).toInt()
        val secs = (seconds % 60).toInt()
        val sb = StringBuilder()
        if (days != 0) sb.append(days).append("d ")
        if (hours != 0) sb.append(hours).append("h ")
        if (minutes != 0) sb.append(minutes).append("m ")
        if (secs != 0) sb.append(secs).append("s")
        return String.format(prefix, sb.toString())
    }

    @JvmStatic
    fun formatTime(src: kotlin.time.Duration): String {
        if (src.isInfinite()) return "Never"
        val seconds = src.inWholeSeconds
        if (seconds == 0L) return "now"
        val prefix = if (seconds > 0) "in %s" else "%s ago"
        val days = (seconds / 86400).toInt()
        val hours = ((seconds % 86400) / 3600).toInt()
        val minutes = ((seconds % 3600) / 60).toInt()
        val secs = (seconds % 60).toInt()
        val sb = StringBuilder()
        if (days != 0) sb.append(days).append("d ")
        if (hours != 0) sb.append(hours).append("h ")
        if (minutes != 0) sb.append(minutes).append("m ")
        if (secs != 0) sb.append(secs).append("s")
        return String.format(prefix, sb.toString())
    }
}
