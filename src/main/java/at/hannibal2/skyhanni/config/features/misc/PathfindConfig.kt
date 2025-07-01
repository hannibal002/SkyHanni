package at.hannibal2.skyhanni.config.features.misc

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class PathfindConfig {

    @Expose
    @ConfigOption(name = "Chat Update Interval", desc = "Change how often the chat message should update the distance.")
    @ConfigEditorDropdown
    @SearchTag("navigation, pathfind")
    var chatUpdateInterval: UpdateInterval = UpdateInterval.PERFECT

    enum class UpdateInterval(private val displayName: String, val duration: Duration) {
        IMMEDIATELY("1/20 second", 0.seconds),
        PERFECT("1/10 second", 100.milliseconds),
        SHORT("1/4 second", 250.milliseconds),
        LESS_SHORT("1/2 second", 500.milliseconds),
        MID("Every second", 1.seconds),
        A_BIT_LONGER("Every 2 second", 2.seconds),
        LONG("5 seconds", 5.seconds),
        ;

        override fun toString() = displayName
    }
}
