package at.hannibal2.skyhanni.config.features.mining

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class CommissionsBlocksColorConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Change the color of ores on mining island depending on your active commissions. Gray out irrelevant ores."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Sneak Toggle", desc = "Quickly disable or enable this feature via sneaking.")
    @ConfigEditorBoolean
    var sneakQuickToggle: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Color", desc = "Change the highlight color.")
    @ConfigEditorDropdown
    var color: Property<LorenzColor> = Property.of(LorenzColor.GREEN)
}
