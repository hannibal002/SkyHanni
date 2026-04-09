package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class BoopPartyConfig {

    @Expose
    @ConfigOption(
        name = "Boop Party",
        desc = "Send a chat prompt to party invite players that /boop you. (Will activate on all profiles incl Bingo)"
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var boopParty: Boolean = false

    @Expose
    @ConfigOption(
        name = "Only on Bingo",
        desc = "Above setting but only active on Bingo."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var boopPartyBingo: Boolean = false

}
