package at.hannibal2.skyhanni.config.features.pets

import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class TamingSixtyConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Shows a helper for the cheapest prices to buy pets to raise your Taming level cap.",
    )
    @ConfigEditorBoolean
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Fetch Other Tiers",
        desc = "Fetch prices for other tiers of the same pet. These prices would not include costs to upgrade pet rarity with Kat.",
    )
    @ConfigEditorBoolean
    var otherTiers: Boolean = false

    @Expose
    @ConfigLink(owner = TamingSixtyConfig::class, field = "display")
    val displayPos: Position = Position(125, 250)
}
