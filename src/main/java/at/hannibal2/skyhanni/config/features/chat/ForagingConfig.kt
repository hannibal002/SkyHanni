package at.hannibal2.skyhanni.config.features.chat

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ForagingConfig {

    @Expose
    @ConfigOption(name = "Unmineable Trees", desc = "Hide messages from trying to cut down an unmineable tree.")
    @ConfigEditorBoolean
    @FeatureToggle
    var unmineable: Boolean = false

}
