package at.hannibal2.skyhanni.config.features.slayer

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.generic.LineToConfig
import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MinibossConfig {

    @Expose
    @ConfigOption(name = "Miniboss Highlight", desc = "Highlight Slayer Mini-Boss in blue color.")
    @ConfigEditorBoolean
    @FeatureToggle
    var slayerMinibossHighlight: Boolean = false

    @Expose
    @ConfigOption(name = "Line to Miniboss Mob", desc = "")
    @Accordion
    val minibossLine: LineToConfig = LineToConfig(defaultColor = LorenzColor.AQUA.toChromaColor(255))

    @Expose
    @ConfigOption(name = "Line To Cocoon with Miniboss", desc = "")
    @Accordion
    val cocoonLine: LineToConfig = LineToConfig(defaultColor = LorenzColor.AQUA.toChromaColor(255))
}
