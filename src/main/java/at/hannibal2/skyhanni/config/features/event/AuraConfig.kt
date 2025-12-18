package at.hannibal2.skyhanni.config.features.event

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class AuraConfig {
    @Expose
    @ConfigOption(
        name = "Check-In Reminder",
        desc = "Warns when you need to check in with a Surveillance Goon to get rid of your 10% stat debuff.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var checkInReminder: Boolean = true

}
