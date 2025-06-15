package at.hannibal2.skyhanni.config.features.rift

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class CruxTalismanDisplayConfig {
    @Expose
    @ConfigOption(name = "Crux Talisman Display", desc = "Display progress of the Crux Talisman on screen.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Compact", desc = "Show a compacted version of the overlay when the talisman is maxed.")
    @ConfigEditorBoolean
    var compactWhenMaxed: Boolean = false

    @Expose
    @ConfigOption(name = "Show Bonuses", desc = "Show bonuses you get from the talisman.")
    @ConfigEditorBoolean
    @FeatureToggle
    val showBonuses: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigLink(owner = CruxTalismanDisplayConfig::class, field = "enabled")
    val position: Position = Position(144, 139)
}
