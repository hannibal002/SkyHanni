package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.utils.Stopwatch
import com.google.gson.annotations.Expose

abstract class TrackerData {
    @Expose
    var sessionUptime = Stopwatch()

    fun reset() {
        sessionUptime = Stopwatch()
        resetData()
    }

    abstract fun resetData()
}
