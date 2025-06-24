package at.hannibal2.skyhanni.config.features.combat.end

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.combat.end.endernodetracker.EnderNode
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class EnderNodeConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Tracks all of your drops from mining Ender Nodes in the End.\n" +
            "Also tracks drops from Endermen."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Only While Holding Tool",
        desc = "Only show the tracker if holding a pickaxe, drill or gauntlet in hand."
    )
    @ConfigEditorBoolean
    var onlyPickaxe: Boolean = false

    @Expose
    @ConfigOption(name = "Text Format", desc = "Drag text to change the appearance of the overlay.")
    @ConfigEditorDraggableList
    val textFormat: Property<MutableList<EnderNodeDisplayEntry>> = Property.of(
        mutableListOf(
            EnderNodeDisplayEntry.TITLE,
            EnderNodeDisplayEntry.NODES_MINED,
            EnderNodeDisplayEntry.COINS_MADE,
            EnderNodeDisplayEntry.SPACER_1,
            EnderNodeDisplayEntry.ENDERMITE_NEST,
            EnderNodeDisplayEntry.ENCHANTED_END_STONE,
            EnderNodeDisplayEntry.ENCHANTED_OBSIDIAN,
            EnderNodeDisplayEntry.ENCHANTED_ENDER_PEARL,
            EnderNodeDisplayEntry.GRAND_XP_BOTTLE,
            EnderNodeDisplayEntry.TITANIC_XP_BOTTLE,
            EnderNodeDisplayEntry.MAGICAL_RUNE_I,
            EnderNodeDisplayEntry.MITE_GEL,
            EnderNodeDisplayEntry.SHRIMP_THE_FISH,
            EnderNodeDisplayEntry.SPACER_2,
            EnderNodeDisplayEntry.ENDER_ARMOR,
            EnderNodeDisplayEntry.ENDERMAN_PET
        )
    )

    enum class EnderNodeDisplayEntry(private val displayName: String) {
        TITLE("§5§lEnder Node Tracker"),
        NODES_MINED("§d1,303 Ender Nodes Mined"),
        COINS_MADE("§615.3M Coins Made"),
        SPACER_1(" "),
        ENDERMITE_NEST("§b123 §cEndermite Nest"),
        ENCHANTED_END_STONE("§b832 §aEnchanted End Stone"),
        ENCHANTED_OBSIDIAN("§b230 §aEnchanted Obsidian"),
        ENCHANTED_ENDER_PEARL("§b1630 §aEnchanted Ender Pearl"),
        GRAND_XP_BOTTLE("§b85 §aGrand Experience Bottle"),
        TITANIC_XP_BOTTLE("§b4 §9Titanic Experience Bottle"),
        END_STONE_SHULKER("§b15 §9End Stone Shulker"),
        END_STONE_GEODE("§b53 §9End Stone Geode"),
        MAGICAL_RUNE_I("§b10 §d◆ Magical Rune I"),
        ENDER_GAUNTLET("§b24 §5Ender Gauntlet"),
        MITE_GEL("§b357 §5Mite Gel"),
        SHRIMP_THE_FISH("§b2 §cShrimp The Fish"),
        SPACER_2(" "),
        ENDER_ARMOR("§b200 §5Ender Armor"),
        ENDER_HELMET("§b24 §5Ender Helmet"),
        ENDER_CHESTPLATE("§b24 §5Ender Chestplate"),
        ENDER_LEGGINGS("§b24 §5Ender Leggings"),
        ENDER_BOOTS("§b24 §5Ender Boots"),
        ENDER_NECKLACE("§b24 §5Ender Necklace"),
        ENDERMAN_PET("§f10§7-§a8§7-§93§7-§52§7-§61 §fEnderman Pet"),
        ;

        override fun toString() = displayName

        companion object {
            private val enderNodeCache: MutableMap<EnderNodeDisplayEntry, EnderNode?> = mutableMapOf()
        }

        fun toEnderNodeOrNull(): EnderNode? = enderNodeCache.getOrPut(this) {
            EnderNode.entries.firstOrNull {
                it.toEnderNodeDisplayEntryOrNull() == this
            }
        }
    }

    @Expose
    @ConfigLink(owner = EnderNodeConfig::class, field = "enabled")
    val position: Position = Position(10, 80)
}
