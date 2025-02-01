package at.hannibal2.skyhanni.config.features.event

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class AnniversaryCelebration400Config {

    @ConfigOption(
        name = "Daily Highlight",
        desc = "Highlights incomplete daily tasks.",
    )
    @Expose
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightDailyTasks: Boolean = true
}
