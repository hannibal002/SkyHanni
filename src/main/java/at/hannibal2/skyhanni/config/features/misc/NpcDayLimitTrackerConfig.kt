package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class NpcDayLimitTrackerConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Track NPC sell coins from chat against the 500M daily limit (resets midnight GMT) and show a movable HUD.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Number Format",
        desc = "Short: 400k/500m\nLong: 400,000/500,000,000",
    )
    @ConfigEditorDropdown
    val numberFormat: Property<NumberFormatEntry> = Property.of(NumberFormatEntry.SHORT)

    enum class NumberFormatEntry(private val displayName: String) {
        SHORT("Short"),
        LONG("Long"),
        ;

        override fun toString(): String = displayName
    }

    @Expose
    @ConfigLink(owner = NpcDayLimitTrackerConfig::class, field = "enabled")
    val position: Position = Position(8, 520)
}
