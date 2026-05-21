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
        desc = "Track NPC sell coins from chat against the 500M daily limit (resets midnight GMT).",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Show HUD",
        desc = "Movable widget showing sold coins vs the 500M daily cap.",
    )
    @ConfigEditorBoolean
    var showHud: Boolean = true

    @Expose
    @ConfigOption(
        name = "Number Format",
        desc = "How coin amounts are shown on the HUD. Only applies when Show HUD is enabled.",
    )
    @ConfigEditorDropdown
    val numberFormat: Property<NumberFormatEntry> = Property.of(NumberFormatEntry.CONDENSED)

    enum class NumberFormatEntry(private val displayName: String) {
        CONDENSED("Condensed"),
        FULL("Full"),
        ;

        override fun toString(): String = displayName
    }

    @Expose
    @ConfigLink(owner = NpcDayLimitTrackerConfig::class, field = "showHud")
    val position: Position = Position(8, 520)

    @Expose
    var gmtEpochDay: Long = 0L

    @Expose
    var soldCoins: Long = 0L
}
