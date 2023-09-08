package at.hannibal2.skyhanni.utils

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


data class TimeMark(val long: Long) {

    fun hasNeverHappened() = long == 0L
    fun passedTime() = if (long == 0L) Duration.Companion.INFINITE else (System.currentTimeMillis() - long).milliseconds

    companion object {
        fun never() = TimeMark(0)
        fun now() = TimeMark(System.currentTimeMillis())
    }
}