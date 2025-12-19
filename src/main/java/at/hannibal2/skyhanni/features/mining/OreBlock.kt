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
    LOW_TIER_MITHRIL(::isLowTierMithril, { inDwarvenMines || inGlacite }, OreCategory.DWARVEN_METAL),
    MID_TIER_MITHRIL(::isMidTierMithril, { inDwarvenMines || inCrystalHollows || inGlacite }, OreCategory.DWARVEN_METAL),
    HIGH_TIER_MITHRIL(::isHighTierMithril, { inDwarvenMines || inCrystalHollows || inGlacite }, OreCategory.DWARVEN_METAL),

    // TITANIUM
    TITANIUM(::isTitanium, { inDwarvenMines || inGlacite }, OreCategory.DWARVEN_METAL),

    // VANILLA ORES
    STONE(::isStone, { inDwarvenMines }, OreCategory.BLOCK),
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
    RED_SAND(::isRedSand, { inCrimsonIsle }, OreCategory.BLOCK),
    SULPHUR(Blocks.SPONGE, { inCrimsonIsle }, OreCategory.ORE),

    // SPIDER'S DEN
    GRAVEL(Blocks.GRAVEL, { inSpidersDen }, OreCategory.BLOCK),

    // END
    END_STONE(Blocks.END_STONE, { inEnd }, OreCategory.BLOCK),
    OBSIDIAN(Blocks.OBSIDIAN, { inCrystalHollows || inMineshaft || inEnd }, OreCategory.ORE),

    // HARD STONE
    HARD_STONE_HOLLOWS(::isHardStoneHollows, { inCrystalHollows }, OreCategory.BLOCK),
    HARD_STONE_TUNNELS(::isHardstoneTunnels, { inTunnels }, OreCategory.BLOCK),
    HARD_STONE_MINESHAFT(::isHardstoneMineshaft, { inMineshaft }, OreCategory.BLOCK),

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
    LOW_TIER_UMBER(::isLowTierUmber, { inGlacite }, OreCategory.DWARVEN_METAL),
    MID_TIER_UMBER(::isMidTierUmber, { inGlacite }, OreCategory.DWARVEN_METAL),
    HIGH_TIER_UMBER(::isHighTierUmber, { inGlacite }, OreCategory.DWARVEN_METAL),

    LOW_TIER_TUNGSTEN_TUNNELS(::isLowTierTungstenTunnels, { inTunnels }, OreCategory.DWARVEN_METAL),
    LOW_TIER_TUNGSTEN_MINESHAFT(::isLowTierTungstenMineshaft, { inMineshaft }, OreCategory.DWARVEN_METAL),
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
     * Assume below softcap
     */
    fun speedNeededForNextTick(currentSpeed: Double): Double =
        (strength * 30) / (miningTicks(currentSpeed) - 0.5) - currentSpeed

    constructor(block: Block, checkArea: () -> Boolean, category: OreCategory, hasInitSound: Boolean = true) :
        this({ it.block == block }, checkArea, category, hasInitSound)

    constructor(gemstoneColor: DyeColor, checkArea: () -> Boolean, category: OreCategory, hasInitSound: Boolean = true) :
        this({ it.isGemstoneWithColor(gemstoneColor) }, checkArea, category, hasInitSound)

    companion object {
        fun getByStateOrNull(state: BlockState): OreBlock? = currentAreaOreBlocks.find { it.checkBlock(state) }

        fun getByNameOrNull(string: String) = entries.firstOrNull { it.name == string }
    }
}

private fun isLowTierMithril(state: BlockState): Boolean = when (state.block) {
    Blocks.GRAY_WOOL -> true
    Blocks.CYAN_TERRACOTTA -> true
    else -> false
}

private fun isMidTierMithril(state: BlockState): Boolean {
    return state.block == Blocks.PRISMARINE || state.block == Blocks.PRISMARINE_BRICKS || state.block == Blocks.DARK_PRISMARINE
}

private fun isHighTierMithril(state: BlockState): Boolean {
    return state.block == Blocks.LIGHT_BLUE_WOOL
}

fun isTitanium(state: BlockState): Boolean {
    return state.block == Blocks.POLISHED_DIORITE
}

private fun isStone(state: BlockState): Boolean {
    return state.block == Blocks.STONE
}

private fun isHardStoneHollows(state: BlockState): Boolean {
    return when (state.block) {
        Blocks.GRAY_WOOL -> true
        Blocks.GREEN_WOOL -> true
        Blocks.CYAN_TERRACOTTA -> true
        Blocks.BROWN_TERRACOTTA -> true
        Blocks.GRAY_TERRACOTTA -> true
        Blocks.BLACK_TERRACOTTA -> true
        Blocks.LIME_TERRACOTTA -> true
        Blocks.GREEN_TERRACOTTA -> true
        Blocks.BLUE_TERRACOTTA -> true
        Blocks.RED_TERRACOTTA -> true
        Blocks.LIGHT_GRAY_TERRACOTTA -> true
        Blocks.CLAY -> true
        Blocks.STONE_BRICKS -> true
        Blocks.MOSSY_STONE_BRICKS -> true
        Blocks.CRACKED_STONE_BRICKS -> true
        Blocks.CHISELED_STONE_BRICKS -> true
        Blocks.STONE -> true
        Blocks.DIORITE -> true
        Blocks.GRANITE -> true
        Blocks.ANDESITE -> true
        else -> false
    }
}

private fun isHardstoneTunnels(state: BlockState): Boolean =
    state.block == Blocks.INFESTED_STONE || state.block == Blocks.LIGHT_GRAY_WOOL

private fun isHardstoneMineshaft(state: BlockState): Boolean =
    state.block == Blocks.STONE || state.block == Blocks.LIGHT_GRAY_WOOL

private fun isRedSand(state: BlockState): Boolean =
    state.block == Blocks.RED_SAND

private fun isLowTierUmber(state: BlockState): Boolean =
    state.block == Blocks.TERRACOTTA

private fun isMidTierUmber(state: BlockState): Boolean =
    state.block == Blocks.BROWN_TERRACOTTA

private fun isHighTierUmber(state: BlockState): Boolean =
    state.block == Blocks.SMOOTH_RED_SANDSTONE

private fun isLowTierTungstenTunnels(state: BlockState): Boolean =
    state.block == Blocks.INFESTED_COBBLESTONE

private fun isLowTierTungstenMineshaft(state: BlockState): Boolean = when (state.block) {
    Blocks.COBBLESTONE_SLAB -> true
    Blocks.COBBLESTONE, Blocks.COBBLESTONE_STAIRS -> true
    else -> false
}

private fun BlockState.isGemstoneWithColor(color: DyeColor): Boolean = when (block) {
    is StainedGlassBlock -> (block as StainedGlassBlock).color == color
    is StainedGlassPaneBlock -> (block as StainedGlassPaneBlock).color == color
    else -> false
}
