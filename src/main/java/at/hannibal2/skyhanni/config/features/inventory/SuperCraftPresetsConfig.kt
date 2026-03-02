package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SuperCraftPresetsConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Show preset craft amounts in the Supercrafting sign menu."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Preset Values",
        desc = "Comma-separated list of supercraft amounts to display."
    )
    @ConfigEditorText
    var presets: String = "4,8,16,32,64,128,256,512"

    @Expose
    @ConfigLink(owner = SuperCraftPresetsConfig::class, field = "enabled")
    val signPosition: Position = Position(100, 100, false, true)
}


