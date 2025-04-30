package at.hannibal2.skyhanni.config.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.event.hoppity.HoppityCollectionStats.HighlightRabbitTypes
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import java.util.*

class HoppityCollectionStatsConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show info about your Hoppity rabbit collection.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigLink(owner = HoppityCollectionStatsConfig::class, field = "enabled")
    var position: Position = Position(163, 160)

    @Expose
    @ConfigOption(name = "Highlight Found Rabbits", desc = "Highlight rabbits that have already been found.")
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightFoundRabbits: Boolean = false

    @Expose
    @ConfigOption(name = "Highlight Rabbits", desc = "Highlight specific rabbit types in Hoppity's Collection.")
    @ConfigEditorDraggableList
    var highlightRabbits: MutableList<HighlightRabbitTypes> = mutableListOf(
        HighlightRabbitTypes.ABI,
        HighlightRabbitTypes.FACTORY,
        HighlightRabbitTypes.MET,
        HighlightRabbitTypes.NOT_MET,
        HighlightRabbitTypes.SHOP,
        HighlightRabbitTypes.STRAYS
    )

    @Expose
    @ConfigOption(
        name = "Re-color Missing Rabbit Dyes",
        desc = "Replace the gray dye in Hoppity's Collection with a color for the rarity of the rabbit."
    )
    @ConfigEditorBoolean
    var rarityDyeRecolor: Boolean = true

    @Expose
    @ConfigOption(
        name = "Missing Location Rabbits",
        desc = "Show the locations you have yet to find enough egg locations for in order to unlock the rabbit for that location."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var showLocationRequirementsRabbits: Boolean = false

    @Expose
    @ConfigOption(
        name = "Missing Resident Rabbits",
        desc = "Show the islands that you have the most missing resident rabbits on."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var showResidentSummary: Boolean = false

    @Expose
    @ConfigOption(
        name = "Missing Hotspot Rabbits",
        desc = "Show the islands that have the most hotspot rabbits that you are missing."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var showHotspotSummary: Boolean = false
}
