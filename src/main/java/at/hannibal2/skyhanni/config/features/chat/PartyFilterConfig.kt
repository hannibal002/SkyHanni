package at.hannibal2.skyhanni.config.features.chat

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PartyFilterConfig {
    @Expose
    @ConfigOption(name = "Party Lines", desc = "Hides the \"---------\'s\" in party messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    var partyLine: Boolean = false

}
