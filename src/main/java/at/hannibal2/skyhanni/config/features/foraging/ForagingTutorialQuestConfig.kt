package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.OnlyModern
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ForagingTutorialQuestConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show where to go to unlock foraging islands.")
    @OnlyModern
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Suggest to enable", desc = "When useful, suggest in chat to enable this feature.")
    @OnlyModern
    @ConfigEditorBoolean
    var suggestToEnable: Boolean = true
}
