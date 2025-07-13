package at.hannibal2.skyhanni.config.features.event.winter

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class FrozenTreasureConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Track all of your drops from Frozen Treasure in the Glacial Caves.\n" +
            "§eIce calculations are an estimate but are relatively accurate."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Text Format", desc = "Drag text to change the appearance of the overlay.")
    @ConfigEditorDraggableList
    val textFormat: MutableList<FrozenTreasureDisplayEntry> = mutableListOf(
        FrozenTreasureDisplayEntry.TITLE,
        FrozenTreasureDisplayEntry.TREASURES_MINED,
        FrozenTreasureDisplayEntry.TOTAL_ICE,
        FrozenTreasureDisplayEntry.ICE_PER_HOUR,
        FrozenTreasureDisplayEntry.COMPACT_PROCS,
        FrozenTreasureDisplayEntry.SPACER_1,
        FrozenTreasureDisplayEntry.WHITE_GIFT,
        FrozenTreasureDisplayEntry.GREEN_GIFT,
        FrozenTreasureDisplayEntry.RED_GIFT,
        FrozenTreasureDisplayEntry.ENCHANTED_ICE,
        FrozenTreasureDisplayEntry.ENCHANTED_PACKED_ICE,
        FrozenTreasureDisplayEntry.GLACIAL_FRAGMENT,
        FrozenTreasureDisplayEntry.GLACIAL_TALISMAN
    )

    enum class FrozenTreasureDisplayEntry(private val displayName: String) {
        TITLE("§e§lFrozen Treasure Tracker"),
        TREASURES_MINED("§61,636 Treasures Mined"),
        TOTAL_ICE("§33.2m Total Ice"),
        ICE_PER_HOUR("§3342,192 Ice/hr"),
        COMPACT_PROCS("§81,002 Compact Procs"),
        SPACER_1(" "),
        WHITE_GIFT("§b182 §fWhite Gift"),
        GREEN_GIFT("§b94 §aGreen Gift"),
        RED_GIFT("§b17 §9§cRed Gift"),
        PACKED_ICE("§b328 §fPacked Ice"),
        ENCHANTED_ICE("§b80 §aEnchanted Ice"),
        ENCHANTED_PACKED_ICE("§b4 §9Enchanted Packed Ice"),
        ICE_BAIT("§b182 §aIce Bait"),
        GLOWY_CHUM_BAIT("§b3 §aGlowy Chum Bait"),
        GLACIAL_FRAGMENT("§b36 §5Glacial Fragment"),
        GLACIAL_TALISMAN("§b6 §fGlacial Talisman"),
        FROZEN_BAIT("§b20 §9Frozen Bait"),
        EINARY_RED_HOODIE("§b1 §cEinary's Red Hoodie"),
        SPACER_2(" "),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Only in Glacial Cave", desc = "Only show the overlay while in the Glacial Cave.")
    @ConfigEditorBoolean
    var onlyInCave: Boolean = true

    @Expose
    @ConfigOption(
        name = "Show as Drops",
        desc = "Multiply the numbers on the display by the base drop.\n" +
            "E.g. 3 Ice Bait -> 48 Ice Bait"
    )
    @ConfigEditorBoolean
    var showAsDrops: Boolean = false

    @Expose
    @ConfigOption(name = "Hide Chat Messages", desc = "Hide the chat messages from Frozen Treasures.")
    @ConfigEditorBoolean
    var hideMessages: Boolean = false

    @Expose
    @ConfigLink(owner = FrozenTreasureConfig::class, field = "enabled")
    val position: Position = Position(10, 80)
}
