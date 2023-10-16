package at.hannibal2.skyhanni.utils

import io.github.moulberry.notenoughupdates.util.SkyBlockTime

fun SkyBlockTime.formatted(): String {
    val date = SkyBlockTime.now()
    val hour = if (date.hour > 12) date.hour - 12 else date.hour
    val timeOfDay = if (date.hour > 11) "pm" else "am" // hooray for 12-hour clocks
    var minute = date.minute.toString()
    if (minute.length != 2) {
        minute = minute.padStart(2, '0')
    }

    val month = SkyBlockTime.monthName(date.month)
    val day = date.day
    val daySuffix = SkyBlockTime.daySuffix(day)
    val year = date.year
    return "$month $day$daySuffix, Year $year $hour:${minute}$timeOfDay" // Early Winter 1st Year 300, 12:03pm
}