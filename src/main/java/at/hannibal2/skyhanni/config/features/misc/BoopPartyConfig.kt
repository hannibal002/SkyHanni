package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class BoopPartyConfig {

    @Expose
    @ConfigOption(
        name = "Boop Party",
        desc = "Send party invite to players that /boop you."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var boopParty: Boolean = false

    @Expose
    @ConfigOption(
        name = "Boop Party",
        desc = "Send party invite to players that boop you while you are on a Bingo profile (Usable Independent to above toggle)."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var boopPartyBingo: Boolean = false

}
