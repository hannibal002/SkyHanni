package at.hannibal2.skyhanni.config.features.garden.visitor

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
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
            DropsStatisticsTextEntry.OVERGROWN_GRASS,
            DropsStatisticsTextEntry.GREEN_BANDANA,
            DropsStatisticsTextEntry.DEDICATION_IV,
            DropsStatisticsTextEntry.COPPER_DYE
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

        // VisitorReward items
        // Todo: Make these names actually in sync with the VisitorReward enum entries
        FLOWERING_BOUQUET("§b23 §9Flowering Bouquet"),
        OVERGROWN_GRASS("§b4 §9Overgrown Grass"),
        GREEN_BANDANA("§b2 §5Green Bandana"),
        DEDICATION_IV("§b1 §9Dedication IV"),
        MUSIC_RUNE_I("§b6 §b◆ Music Rune I"),
        SPACE_HELMET("§b1 §cSpace Helmet"),
        CULTIVATING_I("§b1 §9Cultivating I"),
        REPLENISH_I("§b1 §9Replenish I"),
        DELICATE("§b1 §9Delicate V"),
        COPPER_DYE("§b1 §8Copper Dye"),
        JUNGLE_KEY("§b1 §5Jungle Key"),
        FRUIT_BOWL("§b1 §9Fruit Bowl"),
        HARVEST_HARBINGER("§b1 §9Harvest Harbinger V"),
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
    @ConfigOption(
        name = "Display Icons",
        desc = "Replace the drop names with icons.\n" +
            "§eNote: Will not update the preview above!"
    )
    @ConfigEditorBoolean
    val displayIcons: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Only on Barn Plot", desc = "Only show the overlay while on the Barn plot.")
    @ConfigEditorBoolean
    val onlyOnBarn: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigLink(owner = DropsStatisticsConfig::class, field = "enabled")
    val pos: Position = Position(5, 20)
}
