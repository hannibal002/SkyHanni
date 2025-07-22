package at.hannibal2.skyhanni.config.features.combat.damageindicator

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class VampireSlayerConfig {
    @Expose
    @ConfigOption(
        name = "HP Until Steak",
        desc = "Show the amount of HP left until the Steak can be used on the Vampire Slayer on top of the boss."
    )
    @ConfigEditorBoolean
    var hpTillSteak: Boolean = false

    @Expose
    @ConfigOption(
        name = "Mania Circles",
        desc = "Show a timer until the boss leaves the invincible Mania Circles state."
    )
    @ConfigEditorBoolean
    var maniaCircles: Boolean = false

    @Expose
    @ConfigOption(name = "Percentage HP", desc = "Show a percentage next to the HP.")
    @ConfigEditorBoolean
    var percentage: Boolean = false
}
