package at.hannibal2.skyhanni.config.features.chat

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ForagingFilterConfig {

    @Expose
    @ConfigOption(name = "Hide Lottery Messages", desc = "Hide the Lottery messages outside of Foraging Islands.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hideLottery: Boolean = true

    @Expose
    @ConfigOption(name = "Unmineable Trees", desc = "Hide messages from trying to cut down an unmineable tree.")
    @ConfigEditorBoolean
    var unmineable: Boolean = false
}
