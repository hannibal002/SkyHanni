package at.hannibal2.skyhanni.config.features.garden.visitor

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.tracker.timed.TimedGardenIndividualItemTrackerConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class DropsStatisticsConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Tally statistics about visitors and the rewards you have received from them."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    val enabled: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Text Format", desc = "Drag text to change the appearance of the overlay.")
    @ConfigEditorDraggableList
    val textFormat: Property<MutableList<DropsStatisticsTextEntry>> = Property.of(
        mutableListOf(
            DropsStatisticsTextEntry.TITLE,
            DropsStatisticsTextEntry.TOTAL_VISITORS,
            DropsStatisticsTextEntry.VISITORS_BY_RARITY,
            DropsStatisticsTextEntry.ACCEPTED,
            DropsStatisticsTextEntry.DENIED,
            DropsStatisticsTextEntry.SPACER_1,
            DropsStatisticsTextEntry.COPPER,
            DropsStatisticsTextEntry.FARMING_EXP,
            DropsStatisticsTextEntry.COINS_SPENT,
            DropsStatisticsTextEntry.PROFIT_LIST
        )
    )

    /**
     * Generic non VisitorReward stuff belongs in front of the first VisitorReward.
     */
    enum class DropsStatisticsTextEntry(private val displayName: String) {
        // generic stuff
        TITLE("§e§lVisitor Statistics"),
        TOTAL_VISITORS("§e1,636 Total"),
        VISITORS_BY_RARITY("§a1,172§f-§9382§f-§681§f-§d2§f-§c1"),
        ACCEPTED("§21,382 Accepted"),
        DENIED("§c254 Denied"),
        SPACER_1(" "),
        COPPER("§c62,072 Copper"),
        FARMING_EXP("§33.2m Farming EXP"),
        COINS_SPENT("§647.2m Coins Spent"),
        SPACER_2(" "),
        GARDEN_EXP("§212,600 Garden EXP"),
        BITS("§b4.2k Bits"),
        MITHRIL_POWDER("§220k Mithril Powder"),
        GEMSTONE_POWDER("§d18k Gemstone Powder"),
        // TODO reformat this
        PROFIT_LIST("Dropped Item List")
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Display Numbers First",
        desc = "Whether the number or drop name displays first.\n" +
            "§eNote: Will not update the preview above!"
    )
    @ConfigEditorBoolean
    val displayNumbersFirst: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Only on Barn Plot", desc = "Only show the overlay while on the Barn plot.")
    @ConfigEditorBoolean
    val onlyOnBarn: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Copper In Profit Calculations", desc = "Include Copper Profit in Total Profit. Set the coins per copper below.")
    @ConfigEditorBoolean
    val includeCopper: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Coins Per Copper", desc = "Set the amount of coins each copper is worth.")
    @ConfigEditorSlider(minValue = 1000f, maxValue = 10000f, minStep = 250f)
    val coinsPerCopper: Property<Int> = Property.of(5000)

    @Expose
    @ConfigOption(name = "Bits In Profit Calculations", desc = "Include Bits Profit in Total Profit. Set the coins per bit below.")
    @ConfigEditorBoolean
    val includeBits: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Bits Per Copper", desc = "Set the amount of bits each copper is worth.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 2000f, minStep = 100f)
    val coinsPerBit: Property<Int> = Property.of(1000)


    @Expose
    @ConfigOption(
        name = "Tracker Settings",
        desc = ""
    )
    @Accordion
    val perTrackerConfig: TimedGardenIndividualItemTrackerConfig = TimedGardenIndividualItemTrackerConfig()

    @Expose
    @ConfigLink(owner = DropsStatisticsConfig::class, field = "enabled")
    val pos: Position = Position(5, 20)
}
