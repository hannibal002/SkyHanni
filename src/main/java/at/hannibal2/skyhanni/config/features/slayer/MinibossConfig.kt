package at.hannibal2.skyhanni.config.features.slayer

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.generic.lineconfigs.SlayerLineConfigs
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MinibossConfig {

    @Expose
    @ConfigOption(name = "Miniboss Highlight", desc = "Highlight Slayer Minibosses in line color below (even when line is disabled).")
    @ConfigEditorBoolean
    @FeatureToggle
    var slayerMinibossHighlight: Boolean = false

    @Expose
    @ConfigOption(name = "Line to Miniboss Mob", desc = "")
    @Accordion
    val minibossLine: SlayerLineConfigs.SlayerLineDefaultOff = SlayerLineConfigs.SlayerLineDefaultOff()

    @Expose
    @ConfigOption(name = "Disable when Boss", desc = "Disables Line to Miniboss Mobs when your own Slayer Boss is active.")
    @ConfigEditorBoolean
    var shouldBossInterruptLine: Boolean = true

    @Expose
    @ConfigOption(name = "Miniboss Highlight", desc = "Highlight Slayer Miniboss cocoons in line color below (even when line is disabled).")
    @ConfigEditorBoolean
    @FeatureToggle
    var cocoonHighlight: Boolean = false

    @Expose
    @ConfigOption(name = "Line To Cocoon with Miniboss", desc = "")
    @Accordion
    val cocoonLine: SlayerLineConfigs.SlayerLineDefaultOn = SlayerLineConfigs.SlayerLineDefaultOn()

    @Expose
    @ConfigOption(name = "Disable when Boss", desc = "Disables Line to Miniboss Cocoon when your own Slayer Boss is active.")
    @ConfigEditorBoolean
    var shouldBossInterruptCocoonLine: Boolean = true
}
