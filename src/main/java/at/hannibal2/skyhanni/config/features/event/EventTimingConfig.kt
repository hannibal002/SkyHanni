package at.hannibal2.skyhanni.config.features.event

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class EventTimingConfig {

    @Expose
    @ConfigOption(
        name = "Season Locked Messages",
        desc = "When Hypixel says that something is only available during a specific season, " +
            "show how long it takes until that season starts.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var seasonLockedMessages: Boolean = true
}
