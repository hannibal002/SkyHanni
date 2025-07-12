package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class LastServersConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Receive notifications when you rejoin a server you were in previously.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Notification Time",
        desc = "Get notified if you rejoin a server within the specified number of seconds."
    )
    @ConfigEditorSlider(minValue = 5f, maxValue = 300f, minStep = 1f)
    var warnTime: Int = 60
}
