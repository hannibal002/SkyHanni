package at.hannibal2.skyhanni.config.features.chat

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class HuntingConfig {

    @Expose
    @ConfigOption(name = "Redundant Comments", desc = "Hide redundant comments from successfully hunting shards.")
    @ConfigEditorBoolean
    @FeatureToggle
    var redundantComments: Boolean = false

}
