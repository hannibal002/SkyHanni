package at.hannibal2.skyhanni.config.features.rift.motes

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.HasLegacyId
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class RiftInventoryValueConfig {
    @Expose
    @ConfigOption(name = "Inventory Value", desc = "Show total Motes NPC price for the current opened inventory.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(
        name = "Number Format Type",
        desc = "Short: 1.2M\n" +
            "Long: 1,200,000"
    )
    @ConfigEditorDropdown
    val formatType: Property<NumberFormatEntry> = Property.of(
        NumberFormatEntry.SHORT
    )

    enum class NumberFormatEntry(private val displayName: String, private val legacyId: Int = -1) : HasLegacyId {
        SHORT("Short", 0),
        LONG("Long", 1);

        override fun getLegacyId() = legacyId
        override fun toString() = displayName
    }

    @Expose
    @ConfigLink(owner = RiftInventoryValueConfig::class, field = "enabled")
    val position: Position = Position(126, 156)
}
