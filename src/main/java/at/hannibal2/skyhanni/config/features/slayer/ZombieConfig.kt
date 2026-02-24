package at.hannibal2.skyhanni.config.features.slayer

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ZombieConfig {

    @Expose
    @ConfigOption(name = "Boom Display", desc = "Show BOOM for Revenant 5 when the boss is about to explode.")
    @ConfigEditorBoolean
    var boomDisplay: Boolean = false
}
