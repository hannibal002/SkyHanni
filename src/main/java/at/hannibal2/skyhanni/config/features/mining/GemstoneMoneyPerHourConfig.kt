package at.hannibal2.skyhanni.config.features.mining

import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GemstoneMoneyPerHourConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable gemstone coins per hour display.")
    @ConfigEditorBoolean
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Force NPC", desc = "Force the NPC price of gemstones to be used.")
    @ConfigEditorBoolean
    var forceNPC: Boolean = false

    @Expose
    @ConfigOption(name = "Gemstone Type", desc = "Which type of gemstone to use for the coins per hour calculation.")
    @ConfigEditorDropdown
    var gemstoneType: GemstoneType = GemstoneType.FLAWLESS

    enum class GemstoneType(val displayName: String) {
        ROUGH("Rough"),
        FLAWED("Flawed"),
        FINE("Fine"),
        FLAWLESS("Flawless"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigLink(owner = GemstoneMoneyPerHourConfig::class, field = "enabled")
    var position: Position = Position(16, 192, false, true)
}
