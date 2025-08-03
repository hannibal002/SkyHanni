package at.hannibal2.skyhanni.config.features.garden.cropmilestones

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

// TODO Write ConditionalUtils.onToggle()-s for these values in their feature classes
class MushroomPetPerkConfig {
    @Expose
    @ConfigOption(
        name = "Display Enabled",
        desc = "Show the progress and ETA for mushroom crops when farming other crops because of the Mooshroom Cow perk.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(
        name = "Mushroom Text",
        desc = "Drag text to change the appearance of the overlay.\n" +
            "Hold a farming tool to show the overlay.",
    )
    @ConfigEditorDraggableList
    val text: MutableList<MushroomTextEntry> = mutableListOf(
        MushroomTextEntry.TITLE,
        MushroomTextEntry.MUSHROOM_TIER,
        MushroomTextEntry.NUMBER_OUT_OF_TOTAL,
        MushroomTextEntry.TIME,
    )

    // TODO Change MUSHROOM_TIER to MUSHROOM_MILESTONE
    enum class MushroomTextEntry(private val displayName: String) {
        TITLE("§6Mooshroom Cow Perk"),
        MUSHROOM_TIER("§7Mushroom Milestone 8"),
        NUMBER_OUT_OF_TOTAL("§e6,700§8/§e15,000"),
        TIME("§7In §b12m 34s"),
        PERCENTAGE("§7Percentage: §e12.34%"),
        ;

        override fun toString() = displayName
    }

    // Todo rename to position
    @Expose
    @ConfigLink(owner = MushroomPetPerkConfig::class, field = "enabled")
    val pos: Position = Position(-112, -143)
}
