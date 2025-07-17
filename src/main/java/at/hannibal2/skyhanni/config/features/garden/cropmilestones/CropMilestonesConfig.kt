package at.hannibal2.skyhanni.config.features.garden.cropmilestones

import at.hannibal2.skyhanni.config.FeatureToggle
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
    val overflow: CropMilestonesOverflowConfig = CropMilestonesOverflowConfig()

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
    val highestTimeFormat: Property<TimeFormatEntry> = Property.of(TimeFormatEntry.YEAR)

    enum class TimeFormatEntry(private val displayName: String) {
        YEAR("Year"),
        DAY("Day"),
        HOUR("Hour"),
        MINUTE("Minute"),
        SECOND("Second"),
        ;

        @Transient
        val timeUnit = TimeUnit.entries.firstOrNull { it.name == this.name } ?: TimeUnit.SECOND
        override fun toString(): String = displayName
    }

    @Expose
    @ConfigOption(
        name = "Maxed Milestone",
        desc = "Calculate the progress and ETA till maxed milestone (46) instead of next milestone.",
    )
    @ConfigEditorBoolean
    val bestShowMaxedNeeded: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(
        name = "Milestone Text",
        desc = "Drag text to change the appearance of the overlay.\n" +
            "Hold a farming tool to show the overlay.",
    )
    @ConfigEditorDraggableList
    val text: MutableList<MilestoneTextEntry> = mutableListOf(
        MilestoneTextEntry.TITLE,
        MilestoneTextEntry.MILESTONE_TIER,
        MilestoneTextEntry.NUMBER_OUT_OF_TOTAL,
        MilestoneTextEntry.TIME,
        MilestoneTextEntry.CROPS_PER_MINUTE,
        MilestoneTextEntry.BLOCKS_PER_SECOND,
    )

    enum class MilestoneTextEntry(private val displayName: String) {
        TITLE("§6Crop Milestones"),
        MILESTONE_TIER("§7Pumpkin Tier 22"),
        NUMBER_OUT_OF_TOTAL("§e12,300§8/§e100,000"),
        TIME("§7In §b12m 34s"),
        CROPS_PER_SECOND("§7Crops/Second§8: §e205.75"),
        CROPS_PER_MINUTE("§7Crops/Minute§8: §e12,345"),
        CROPS_PER_HOUR("§7Crops/Hour§8: §e740,700"),
        BLOCKS_PER_SECOND("§7Blocks/Second§8: §e19.85"),
        PERCENTAGE("§7Percentage: §e12.34%"),
        ;

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
    val progressDisplayPos: Position = Position(-400, -200)

    @Expose
    @ConfigOption(name = "Best Crop", desc = "")
    @Accordion
    val next: NextConfig = NextConfig()

    @Expose
    @ConfigOption(name = "Mushroom Pet Perk", desc = "")
    @Accordion
    val mushroomPetPerk: MushroomPetPerkConfig = MushroomPetPerkConfig()
}
