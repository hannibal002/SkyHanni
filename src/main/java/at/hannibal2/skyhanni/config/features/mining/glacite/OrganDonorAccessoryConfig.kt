package at.hannibal2.skyhanni.config.features.mining.glacite

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class OrganDonorAccessoryConfig {
    @Expose
    @ConfigOption(
        name = "Mute when all Corpses found",
        desc = "Mutes the sounds made from the Organ Donor accessory " +
            "once all Frozen Corpses in the Mineshaft have been found.\n" +
            "§eRequires Found Corpse waypoints to be enabled!",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var muteWhenAllFound: Boolean = true
}
