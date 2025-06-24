package at.hannibal2.skyhanni.config.features.inventory.sacks

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class OutsideSackValueConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show the value of all items in the sacks as GUI, while not being in the sacks.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigLink(owner = OutsideSackValueConfig::class, field = "enabled")
    var position: Position = Position(144, 139)
}
