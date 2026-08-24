package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.data.MiningApi
import at.hannibal2.skyhanni.data.MiningApi.currentAreaOreBlocks
import at.hannibal2.skyhanni.data.MiningApi.inCrimsonIsle
import at.hannibal2.skyhanni.data.MiningApi.inCrystalHollows
import at.hannibal2.skyhanni.data.MiningApi.inDwarvenMines
import at.hannibal2.skyhanni.data.MiningApi.inEnd
import at.hannibal2.skyhanni.data.MiningApi.inGlacite
import at.hannibal2.skyhanni.data.MiningApi.inMineshaft
import at.hannibal2.skyhanni.data.MiningApi.inSpidersDen
import at.hannibal2.skyhanni.data.MiningApi.inTunnels
import at.hannibal2.skyhanni.utils.BlockUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.equalsOneOf
import at.hannibal2.skyhanni.utils.compat.ColoredBlockCompat
import net.minecraft.world.item.DyeColor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.StainedGlassBlock
import net.minecraft.world.level.block.StainedGlassPaneBlock
import net.minecraft.world.level.block.state.BlockState
import kotlin.math.ceil
import kotlin.math.round

enum class OreCategory {
    BLOCK,
    ORE,
    DWARVEN_METAL,
    GEMSTONE,
}

enum class OreBlock(
    val checkBlock: (BlockState) -> Boolean,
    val checkArea: () -> Boolean,
    val category: OreCategory,
    val hasInitSound: Boolean = true,
) {
    // MITHRIL
    LOW_TIER_MITHRIL({ isLowTierMithril(it) }, { inDwarvenMines || inGlacite }, OreCategory.DWARVEN_METAL),
    MID_TIER_MITHRIL({ isMidTierMithril(it) }, { inDwarvenMines || inCrystalHollows || inGlacite }, OreCategory.DWARVEN_METAL),
    HIGH_TIER_MITHRIL({ isHighTierMithril(it) }, { inDwarvenMines || inCrystalHollows || inGlacite }, OreCategory.DWARVEN_METAL),

    // TITANIUM
    TITANIUM({ isTitanium(it) }, { inDwarvenMines || inGlacite }, OreCategory.DWARVEN_METAL),

    // VANILLA ORES
    STONE({ isStone(it) }, { inDwarvenMines }, OreCategory.BLOCK),
    COBBLESTONE(Blocks.COBBLESTONE, { inDwarvenMines }, OreCategory.BLOCK),
    COAL_ORE(Blocks.COAL_ORE, { inDwarvenMines || inCrystalHollows }, OreCategory.ORE),
    IRON_ORE(Blocks.IRON_ORE, { inDwarvenMines || inCrystalHollows }, OreCategory.ORE),
    GOLD_ORE(Blocks.GOLD_ORE, { inDwarvenMines || inCrystalHollows }, OreCategory.ORE),
    LAPIS_ORE(Blocks.LAPIS_ORE, { inDwarvenMines || inCrystalHollows }, OreCategory.ORE),
    REDSTONE_ORE(
        { it.block.equalsOneOf(BlockUtils.redstoneOreBlocks) },
        { inDwarvenMines || inCrystalHollows },
        OreCategory.ORE,
    ),
    EMERALD_ORE(Blocks.EMERALD_ORE, { inDwarvenMines || inCrystalHollows }, OreCategory.ORE),
    DIAMOND_ORE(Blocks.DIAMOND_ORE, { inDwarvenMines || inCrystalHollows }, OreCategory.ORE),

    // NETHER
    NETHERRACK(Blocks.NETHERRACK, { inCrimsonIsle }, OreCategory.BLOCK),
    QUARTZ_ORE(Blocks.NETHER_QUARTZ_ORE, { inCrystalHollows || inCrimsonIsle }, OreCategory.ORE),
    GLOWSTONE(Blocks.GLOWSTONE, { inCrimsonIsle }, OreCategory.BLOCK),
    MYCELIUM(Blocks.MYCELIUM, { inCrimsonIsle }, OreCategory.BLOCK),
    RED_SAND({ isRedSand(it) }, { inCrimsonIsle }, OreCategory.BLOCK),
    SULPHUR(Blocks.SPONGE, { inCrimsonIsle }, OreCategory.ORE),

    // SPIDER'S DEN
    GRAVEL(Blocks.GRAVEL, { inSpidersDen }, OreCategory.BLOCK),

    // END
    END_STONE(Blocks.END_STONE, { inEnd }, OreCategory.BLOCK),
    OBSIDIAN(Blocks.OBSIDIAN, { inCrystalHollows || inMineshaft || inEnd }, OreCategory.ORE),

    // HARD STONE
    HARD_STONE_HOLLOWS({ isHardStoneHollows(it) }, { inCrystalHollows }, OreCategory.BLOCK),
    HARD_STONE_TUNNELS({ isHardstoneTunnels(it) }, { inTunnels }, OreCategory.BLOCK),
    HARD_STONE_MINESHAFT({ isHardstoneMineshaft(it) }, { inMineshaft }, OreCategory.BLOCK),

    // DWARVEN BLOCKS
    PURE_COAL(Blocks.COAL_BLOCK, { inDwarvenMines || inCrystalHollows }, OreCategory.ORE),
    PURE_IRON(Blocks.IRON_BLOCK, { inDwarvenMines || inCrystalHollows }, OreCategory.ORE, hasInitSound = false),
    PURE_GOLD(
        Blocks.GOLD_BLOCK,
        { inDwarvenMines || inCrystalHollows || inMineshaft },
        OreCategory.ORE, hasInitSound = false,
    ),
    PURE_LAPIS(Blocks.LAPIS_BLOCK, { inDwarvenMines || inCrystalHollows }, OreCategory.ORE),
    PURE_REDSTONE(Blocks.REDSTONE_BLOCK, { inDwarvenMines || inCrystalHollows }, OreCategory.ORE, hasInitSound = false),
    PURE_EMERALD(Blocks.EMERALD_BLOCK, { inDwarvenMines || inCrystalHollows }, OreCategory.ORE, hasInitSound = false),
    PURE_DIAMOND(Blocks.DIAMOND_BLOCK, { inDwarvenMines || inCrystalHollows }, OreCategory.ORE, hasInitSound = false),
    PURE_QUARTZ(Blocks.QUARTZ_BLOCK, { inDwarvenMines || inCrystalHollows }, OreCategory.ORE),

    // GEMSTONES
    RUBY(DyeColor.RED, { inCrystalHollows || inGlacite }, OreCategory.GEMSTONE),
    AMBER(DyeColor.ORANGE, { inCrystalHollows || inGlacite }, OreCategory.GEMSTONE),
    AMETHYST(DyeColor.PURPLE, { inCrystalHollows || inGlacite }, OreCategory.GEMSTONE),
    JADE(DyeColor.LIME, { inCrystalHollows || inGlacite }, OreCategory.GEMSTONE),
    SAPPHIRE(DyeColor.LIGHT_BLUE, { inCrystalHollows || inGlacite }, OreCategory.GEMSTONE),
    TOPAZ(DyeColor.YELLOW, { inCrystalHollows || inGlacite }, OreCategory.GEMSTONE),
    JASPER(DyeColor.MAGENTA, { inCrystalHollows || inMineshaft }, OreCategory.GEMSTONE),
    OPAL(DyeColor.WHITE, { inMineshaft || inCrimsonIsle }, OreCategory.GEMSTONE),
    AQUAMARINE(DyeColor.BLUE, { inGlacite }, OreCategory.GEMSTONE),
    CITRINE(DyeColor.BROWN, { inGlacite }, OreCategory.GEMSTONE),
    ONYX(DyeColor.BLACK, { inGlacite }, OreCategory.GEMSTONE),
    PERIDOT(DyeColor.GREEN, { inGlacite }, OreCategory.GEMSTONE),

    // GLACIAL
    LOW_TIER_UMBER({ isLowTierUmber(it) }, { inGlacite }, OreCategory.DWARVEN_METAL),
    MID_TIER_UMBER({ isMidTierUmber(it) }, { inGlacite }, OreCategory.DWARVEN_METAL),
    HIGH_TIER_UMBER({ isHighTierUmber(it) }, { inGlacite }, OreCategory.DWARVEN_METAL),

    LOW_TIER_TUNGSTEN_TUNNELS({ isLowTierTungstenTunnels(it) }, { inTunnels }, OreCategory.DWARVEN_METAL),
    LOW_TIER_TUNGSTEN_MINESHAFT({ isLowTierTungstenMineshaft(it) }, { inMineshaft }, OreCategory.DWARVEN_METAL),
    HIGH_TIER_TUNGSTEN(Blocks.CLAY, { inGlacite }, OreCategory.DWARVEN_METAL),

    GLACITE(Blocks.PACKED_ICE, { inGlacite }, OreCategory.DWARVEN_METAL),
    ;

    val strength get() = MiningApi.blockStrengths[this] ?: 0

    val speedSoftCap get() = ceil(20.0 / 3.0 * strength).toInt()

    val speedForInstantMine get() = strength * if (category != OreCategory.BLOCK) 60 else 30

    fun miningTicks(speed: Double): Int = when {
        speed >= speedForInstantMine -> 1
        speed >= speedSoftCap -> 4
        else -> round((strength * 30.0) / speed).toInt()
    }

    /**
     * Assume below soft cap
     */
    fun speedNeededForNextTick(currentSpeed: Double): Double =
        (strength * 30) / (miningTicks(currentSpeed) - 0.5) - currentSpeed

    constructor(block: Block, checkArea: () -> Boolean, category: OreCategory, hasInitSound: Boolean = true) :
        this({ it.block == block }, checkArea, category, hasInitSound)

    constructor(gemstoneColor: DyeColor, checkArea: () -> Boolean, category: OreCategory, hasInitSound: Boolean = true) :
        this({ it.isGemstoneWithColor(gemstoneColor) }, checkArea, category, hasInitSound)

    @Suppress("TooManyFunctions")
    companion object {
        fun getByStateOrNull(state: BlockState): OreBlock? = currentAreaOreBlocks.find { it.checkBlock(state) }

        fun getByNameOrNull(string: String): OreBlock? = entries.firstOrNull { it.name == string }

        // The enum entries must call these checks through a lambda, never through a method reference.
        // A method reference to a companion member captures the companion instance when the reference is created,
        // which happens while the enum entries are being initialized, before the companion instance exists.
        // A lambda resolves the companion at call time instead, which is safe.
        private fun isLowTierMithril(state: BlockState): Boolean = state.block.equalsOneOf(
            ColoredBlockCompat.GRAY.woolBlock,
            ColoredBlockCompat.CYAN.clayBlock,
        )

        private fun isMidTierMithril(state: BlockState): Boolean = state.block.equalsOneOf(
            Blocks.PRISMARINE,
            Blocks.PRISMARINE_BRICKS,
            Blocks.DARK_PRISMARINE,
        )

        private fun isHighTierMithril(state: BlockState): Boolean =
            state.block == ColoredBlockCompat.LIGHT_BLUE.woolBlock

        fun isTitanium(state: BlockState): Boolean =
            state.block == Blocks.POLISHED_DIORITE

        private fun isStone(state: BlockState): Boolean =
            state.block == Blocks.STONE

        private fun isHardStoneHollows(state: BlockState): Boolean = state.block.equalsOneOf(
            ColoredBlockCompat.GRAY.woolBlock,
            ColoredBlockCompat.GREEN.woolBlock,
            ColoredBlockCompat.CYAN.clayBlock,
            ColoredBlockCompat.BROWN.clayBlock,
            ColoredBlockCompat.GRAY.clayBlock,
            ColoredBlockCompat.BLACK.clayBlock,
            ColoredBlockCompat.LIME.clayBlock,
            ColoredBlockCompat.GREEN.clayBlock,
            ColoredBlockCompat.BLUE.clayBlock,
            ColoredBlockCompat.RED.clayBlock,
            ColoredBlockCompat.LIGHT_GRAY.clayBlock,
            Blocks.CLAY,
            Blocks.STONE_BRICKS,
            Blocks.MOSSY_STONE_BRICKS,
            Blocks.CRACKED_STONE_BRICKS,
            Blocks.CHISELED_STONE_BRICKS,
            Blocks.STONE,
            Blocks.DIORITE,
            Blocks.GRANITE,
            Blocks.ANDESITE,
        )

        private fun isHardstoneTunnels(state: BlockState): Boolean = state.block.equalsOneOf(
            Blocks.INFESTED_STONE,
            ColoredBlockCompat.LIGHT_GRAY.woolBlock,
        )

        private fun isHardstoneMineshaft(state: BlockState): Boolean = state.block.equalsOneOf(
            Blocks.STONE,
            ColoredBlockCompat.LIGHT_GRAY.woolBlock,
        )

        private fun isRedSand(state: BlockState): Boolean =
            state.block == Blocks.RED_SAND

        private fun isLowTierUmber(state: BlockState): Boolean =
            state.block == Blocks.TERRACOTTA

        private fun isMidTierUmber(state: BlockState): Boolean =
            state.block == ColoredBlockCompat.BROWN.clayBlock

        private fun isHighTierUmber(state: BlockState): Boolean =
            state.block == Blocks.SMOOTH_RED_SANDSTONE

        private fun isLowTierTungstenTunnels(state: BlockState): Boolean =
            state.block == Blocks.INFESTED_COBBLESTONE

        private fun isLowTierTungstenMineshaft(state: BlockState): Boolean = state.block.equalsOneOf(
            Blocks.COBBLESTONE_SLAB,
            Blocks.COBBLESTONE,
            Blocks.COBBLESTONE_STAIRS,
        )

        private fun BlockState.isGemstoneWithColor(color: DyeColor): Boolean = when (block) {
            is StainedGlassBlock -> (block as StainedGlassBlock).color == color
            is StainedGlassPaneBlock -> (block as StainedGlassPaneBlock).color == color
            else -> false
        }
    }
}
