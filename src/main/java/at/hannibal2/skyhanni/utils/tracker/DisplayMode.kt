package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.TimeUtils.dayToLocalDate
import at.hannibal2.skyhanni.utils.TimeUtils.monthToLocalDate
import at.hannibal2.skyhanni.utils.TimeUtils.weekToLocalDate
import at.hannibal2.skyhanni.utils.TimeUtils.yearToLocalDate
import java.time.LocalDate
import kotlin.reflect.KClass

enum class DisplayMode(
    val displayName: String,
    val currentName: String = "This $displayName",
    val alternateName: String = displayName,
    val type: KClass<*>,
    val toValue: (String) -> Comparable<*>?,
    val fromValue: (Comparable<*>) -> String,
    val isDate: Boolean = (type == LocalDate::class),
) {
    TOTAL(
        "Total",
        "Total",
        type = String::class,
        toValue = { it },
        fromValue = { it as String }
    ),
    SESSION(
        "Session",
        type = Int::class,
        toValue = { it.toIntOrNull() },
        fromValue = { (it as Int).toString() }
    ),
    MAYOR(
        "Mayor",
        alternateName = "Mayor, Year",
        type = Int::class,
        toValue = { it.toIntOrNull() },
        fromValue = { (it as Int).toString() }
    ),
    DAY(
        "Day",
        "Today",
        alternateName = "Date",
        type = LocalDate::class,
        toValue = { it.dayToLocalDate() },
        fromValue = { (it as LocalDate).toString() }
    ),
    WEEK(
        "Week",
        type = LocalDate::class,
        toValue = { it.weekToLocalDate() },
        fromValue = { (it as LocalDate).format(TimeUtils.weekFormatter) }
    ),
    MONTH(
        "Month",
        type = LocalDate::class,
        toValue = { it.monthToLocalDate() },
        fromValue = { (it as LocalDate).format(TimeUtils.monthFormatter) }
    ),
    YEAR(
        "Year",
        type = LocalDate::class,
        toValue = { it.yearToLocalDate() },
        fromValue = { (it as LocalDate).format(TimeUtils.yearFormatter) }
    ),
    ;

    override fun toString() = displayName
}
