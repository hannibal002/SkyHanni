package at.hannibal2.skyhanni.config.features.garden.visitor

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.HasLegacyId
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
    enum class DropsStatisticsTextEntry(
        private val displayName: String,
        private val legacyId: Int = -1
    ) : HasLegacyId {
        // generic stuff
        TITLE("§e§lVisitor Statistics", 0),
        TOTAL_VISITORS("§e1,636 Total", 1),
        VISITORS_BY_RARITY("§a1,172§f-§9382§f-§681§f-§d2§f-§c1", 2),
        ACCEPTED("§21,382 Accepted", 3),
        DENIED("§c254 Denied", 4),
        SPACER_1(" ", 5),
        COPPER("§c62,072 Copper", 6),
        FARMING_EXP("§33.2m Farming EXP", 7),
        COINS_SPENT("§647.2m Coins Spent", 8),
        SPACER_2(" ", 17),
        GARDEN_EXP("§212,600 Garden EXP", 18),
        BITS("§b4.2k Bits", 19),
        MITHRIL_POWDER("§220k Mithril Powder", 20),
        GEMSTONE_POWDER("§d18k Gemstone Powder", 21),

        // VisitorReward items
        // Todo: Make these names actually in sync with the VisitorReward enum entries
        FLOWERING_BOUQUET("§b23 §9Flowering Bouquet", 9),
        OVERGROWN_GRASS("§b4 §9Overgrown Grass", 10),
        GREEN_BANDANA("§b2 §5Green Bandana", 11),
        DEDICATION_IV("§b1 §9Dedication IV", 12),
        MUSIC_RUNE_I("§b6 §b◆ Music Rune I", 13),
        SPACE_HELMET("§b1 §cSpace Helmet", 14),
        CULTIVATING_I("§b1 §9Cultivating I", 15),
        REPLENISH_I("§b1 §9Replenish I", 16),
        DELICATE("§b1 §9Delicate V"),
        COPPER_DYE("§b1 §8Copper Dye"),
        JUNGLE_KEY("§b1 §5Jungle Key"),
        FRUIT_BOWL("§b1 §9Fruit Bowl"),
        HARVEST_HARBINGER("§b1 §9Harvest Harbinger V"),
        ;

        override fun getLegacyId() = legacyId
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
