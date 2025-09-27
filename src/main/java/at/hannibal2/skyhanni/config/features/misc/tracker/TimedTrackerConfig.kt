package at.hannibal2.skyhanni.config.features.misc.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class TimedTrackerConfig {
    @Expose
    @ConfigOption(name = "New Session on Game Start", desc = "Create new session display mode when opening the game.")
    @ConfigEditorBoolean
    var resetSession: Boolean = true

    @Expose
    @ConfigOption(
        name = "Sessions to Keep",
        desc = "If there are more than these many year entries, delete the oldest. Set to 0 to never delete."
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 30f, minStep = 1f)
    var session: Int = 5

    @Expose
    @ConfigOption(
        name = "Days to Keep",
        desc = "If there are more than these many day entries, delete the oldest. Set to 0 to never delete."
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 30f, minStep = 1f)
    var days: Int = 8

    @Expose
    @ConfigOption(
        name = "Weeks to Keep",
        desc = "If there are more than these many week entries, delete the oldest. Set to 0 to never delete."
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 30f, minStep = 1f)
    var weeks: Int = 5

    @Expose
    @ConfigOption(
        name = "Months to Keep",
        desc = "If there are more than these many month entries, delete the oldest. Set to 0 to never delete."
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 30f, minStep = 1f)
    var months: Int = 13

    @Expose
    @ConfigOption(
        name = "Years to Keep",
        desc = "If there are more than these many year entries, delete the oldest. Set to 0 to never delete."
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 30f, minStep = 1f)
    var years: Int = 0

    @Expose
    @ConfigOption(
        name = "Other",
        desc = "Amount to keep of other modes not listed above."
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 30f, minStep = 1f)
    var others: Int = 10

    private val config get() = SkyHanniMod.feature.misc.tracker.timedTracker

    fun syncSettings() {
        resetSession = config.resetSession
        days = config.days
        weeks = config.weeks
        months = config.months
        years = config.years
    }
}
