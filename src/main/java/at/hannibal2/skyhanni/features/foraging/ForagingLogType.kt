package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

enum class ForagingLogType(
    val logName: String,
    val internalName: NeuInternalName,
    val eliteLbName: String,
) {
    // Hub log - vanilla item, NEU internal name uses the old damage-value format.
    OAK("Oak Log", "LOG".toInternalName(), eliteLbName = "oak"),
    // Park logs - vanilla items stored with damage-value-based NEU internal names
    SPRUCE("Spruce Log", "LOG-1".toInternalName(), eliteLbName = "spruce"),
    BIRCH("Birch Log", "LOG-2".toInternalName(), eliteLbName = "birch"),
    JUNGLE("Jungle Log", "LOG-3".toInternalName(), eliteLbName = "jungle"),
    ACACIA("Acacia Log", "LOG_2".toInternalName(), eliteLbName = "acacia"),
    DARK_OAK("Dark Oak Log", "LOG_2-1".toInternalName(), eliteLbName = "dark-oak"),
    // Galatea custom logs - SkyBlock-specific items with proper named internal names
    MANGROVE("Mangrove Log", "MANGROVE_LOG".toInternalName(), eliteLbName = "mangrove"),
    FIG("Fig Log", "FIG_LOG".toInternalName(), eliteLbName = "fig"),
    ;

    override fun toString() = logName

    // Tree type label from "Sweep Details" messages, e.g. "Fig", "Dark Oak".
    val sweepTreeName: String get() = logName.removeSuffix(" Log")

    companion object {
        // Maps a "Sweep Details" tree type string to its ForagingLogType, or null.
        fun fromSweepTreeName(name: String): ForagingLogType? = entries.find { it.sweepTreeName == name }

        // Returns the ForagingLogType for a given BlockState, or null if it's not a forageable log.
        // FIG_LOG maps to STRIPPED_SPRUCE_WOOD and is handled elsewhere, so it's excluded here.
        fun BlockState.getForagingLogType(): ForagingLogType? = when (block) {
            Blocks.OAK_LOG, Blocks.OAK_WOOD,
            Blocks.STRIPPED_OAK_LOG, Blocks.STRIPPED_OAK_WOOD -> OAK

            Blocks.SPRUCE_LOG, Blocks.SPRUCE_WOOD,
            Blocks.STRIPPED_SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_WOOD -> SPRUCE

            Blocks.BIRCH_LOG, Blocks.BIRCH_WOOD,
            Blocks.STRIPPED_BIRCH_LOG, Blocks.STRIPPED_BIRCH_WOOD -> BIRCH

            Blocks.JUNGLE_LOG, Blocks.JUNGLE_WOOD,
            Blocks.STRIPPED_JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_WOOD -> JUNGLE

            Blocks.ACACIA_LOG, Blocks.ACACIA_WOOD,
            Blocks.STRIPPED_ACACIA_LOG, Blocks.STRIPPED_ACACIA_WOOD -> ACACIA

            Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_WOOD,
            Blocks.STRIPPED_DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_WOOD -> DARK_OAK

            Blocks.MANGROVE_LOG, Blocks.MANGROVE_WOOD,
            Blocks.STRIPPED_MANGROVE_LOG, Blocks.STRIPPED_MANGROVE_WOOD -> MANGROVE

            else -> null
        }
    }
}
