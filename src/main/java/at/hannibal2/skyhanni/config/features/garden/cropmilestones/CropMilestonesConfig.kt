package at.hannibal2.skyhanni.config.features.garden.cropmilestones

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.HasLegacyId
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.utils.TimeUnit
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class CropMilestonesConfig {
    @Expose
    @ConfigOption(
        name = "Progress Display",
        desc = "Show the progress and ETA until the next crop milestone is reached and the current crops/minute value.\n" +
            "§eRequires a tool with either a counter or Cultivating enchantment for full accuracy.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var progress: Boolean = true

    @Expose
    @ConfigOption(name = "Overflow", desc = "")
    @Accordion
    var overflow: CropMilestonesOverflowConfig = CropMilestonesOverflowConfig()

    @Expose
    @ConfigOption(
        name = "Warn When Close",
        desc = "Warn with title and sound when the next crop milestone upgrade happens in 5 seconds. " +
            "Useful for switching to a different pet for leveling.",
    )
    @ConfigEditorBoolean
    var warnClose: Boolean = false

    @Expose
    @ConfigOption(name = "Time Format", desc = "Change the highest time unit to show (1h30m vs 90min)")
    @ConfigEditorDropdown
    var highestTimeFormat: Property<TimeFormatEntry> = Property.of(TimeFormatEntry.YEAR)

    enum class TimeFormatEntry(
        private val displayName: String,
        private val legacyId: Int = -1,
    ) : HasLegacyId {
        YEAR("Year", 0),
        DAY("Day", 1),
        HOUR("Hour", 2),
        MINUTE("Minute", 3),
        SECOND("Second", 4),
        ;

        @Transient
        val timeUnit = TimeUnit.entries.firstOrNull { it.name == this.name } ?: TimeUnit.SECOND
        override fun getLegacyId(): Int = legacyId
        override fun toString(): String = displayName
    }

    @Expose
    @ConfigOption(
        name = "Maxed Milestone",
        desc = "Calculate the progress and ETA till maxed milestone (46) instead of next milestone.",
    )
    @ConfigEditorBoolean
    var bestShowMaxedNeeded: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(
        name = "Milestone Text",
        desc = "Drag text to change the appearance of the overlay.\n" +
            "Hold a farming tool to show the overlay.",
    )
    @ConfigEditorDraggableList
    var text: MutableList<MilestoneTextEntry> = mutableListOf(
        MilestoneTextEntry.TITLE,
        MilestoneTextEntry.MILESTONE_TIER,
        MilestoneTextEntry.NUMBER_OUT_OF_TOTAL,
        MilestoneTextEntry.TIME,
        MilestoneTextEntry.CROPS_PER_MINUTE,
        MilestoneTextEntry.BLOCKS_PER_SECOND,
    )

    enum class MilestoneTextEntry(
        private val displayName: String,
        private val legacyId: Int = -1,
    ) : HasLegacyId {
        TITLE("§6Crop Milestones", 0),
        MILESTONE_TIER("§7Pumpkin Tier 22", 1),
        NUMBER_OUT_OF_TOTAL("§e12,300§8/§e100,000", 2),
        TIME("§7In §b12m 34s", 3),
        CROPS_PER_SECOND("§7Crops/Second§8: §e205.75"),
        CROPS_PER_MINUTE("§7Crops/Minute§8: §e12,345", 4),
        CROPS_PER_HOUR("§7Crops/Hour§8: §e740,700"),
        BLOCKS_PER_SECOND("§7Blocks/Second§8: §e19.85", 5),
        PERCENTAGE("§7Percentage: §e12.34%", 6),
        ;

        override fun getLegacyId() = legacyId
        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Block Broken Precision", desc = "The amount of decimals displayed in blocks/second.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 6f, minStep = 1f)
    var blocksBrokenPrecision: Int = 2

    @Expose
    @ConfigOption(name = "Seconds Before Reset", desc = "How many seconds of not farming until blocks/second resets.")
    @ConfigEditorSlider(minValue = 2f, maxValue = 60f, minStep = 1f)
    var blocksBrokenResetTime: Int = 5

    @Expose
    @ConfigLink(owner = CropMilestonesConfig::class, field = "progress")
    var progressDisplayPos: Position = Position(-400, -200)

    @Expose
    @ConfigOption(name = "Best Crop", desc = "")
    @Accordion
    var next: NextConfig = NextConfig()

    @Expose
    @ConfigOption(name = "Mushroom Pet Perk", desc = "")
    @Accordion
    var mushroomPetPerk: MushroomPetPerkConfig = MushroomPetPerkConfig()
}
