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
import net.minecraft.block.Block
import net.minecraft.block.BlockStainedGlass
import net.minecraft.block.BlockStainedGlassPane
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.item.EnumDyeColor
import kotlin.math.ceil
import kotlin.math.round
//#if MC < 1.16
import net.minecraft.block.BlockColored
import net.minecraft.block.BlockSand
import net.minecraft.block.BlockSilverfish
import net.minecraft.block.BlockStone
import net.minecraft.block.BlockStoneSlab
import net.minecraft.block.BlockStoneSlabNew
//#endif

enum class OreCategory {
    BLOCK,
    ORE,
    DWARVEN_METAL,
    GEMSTONE,
}

enum class OreBlock(
    val checkBlock: (IBlockState) -> Boolean,
    val checkArea: () -> Boolean,
    val category: OreCategory,
    val hasInitSound: Boolean = true,
) {
    // MITHRIL
    LOW_TIER_MITHRIL(::isLowTierMithril, { inDwarvenMines || inGlacite }, OreCategory.DWARVEN_METAL),
    MID_TIER_MITHRIL(Blocks.prismarine, { inDwarvenMines || inCrystalHollows || inGlacite }, OreCategory.DWARVEN_METAL),
    HIGH_TIER_MITHRIL(::isHighTierMithril, { inDwarvenMines || inCrystalHollows || inGlacite }, OreCategory.DWARVEN_METAL),

    // TITANIUM
    TITANIUM(::isTitanium, { inDwarvenMines || inGlacite }, OreCategory.DWARVEN_METAL),

    // VANILLA ORES
    STONE(::isStone, { inDwarvenMines }, OreCategory.BLOCK),
    COBBLESTONE(Blocks.cobblestone, { inDwarvenMines }, OreCategory.BLOCK),
    COAL_ORE(Blocks.coal_ore, { inDwarvenMines || inCrystalHollows }, OreCategory.ORE),
    IRON_ORE(Blocks.iron_ore, { inDwarvenMines || inCrystalHollows }, OreCategory.ORE),
    GOLD_ORE(Blocks.gold_ore, { inDwarvenMines || inCrystalHollows }, OreCategory.ORE),
    LAPIS_ORE(Blocks.lapis_ore, { inDwarvenMines || inCrystalHollows }, OreCategory.ORE),
    REDSTONE_ORE(
        { it.block.equalsOneOf(BlockUtils.redstoneOreBlocks) },
        { inDwarvenMines || inCrystalHollows },
        OreCategory.ORE,
    ),
    EMERALD_ORE(Blocks.emerald_ore, { inDwarvenMines || inCrystalHollows }, OreCategory.ORE),
    DIAMOND_ORE(Blocks.diamond_ore, { inDwarvenMines || inCrystalHollows }, OreCategory.ORE),

    // NETHER
    NETHERRACK(Blocks.netherrack, { inCrimsonIsle }, OreCategory.BLOCK),
    QUARTZ_ORE(Blocks.quartz_ore, { inCrystalHollows || inCrimsonIsle }, OreCategory.ORE),
    GLOWSTONE(Blocks.glowstone, { inCrimsonIsle }, OreCategory.BLOCK),
    MYCELIUM(Blocks.mycelium, { inCrimsonIsle }, OreCategory.BLOCK),
    RED_SAND(::isRedSand, { inCrimsonIsle }, OreCategory.BLOCK),
    SULPHUR(Blocks.sponge, { inCrimsonIsle }, OreCategory.ORE),

    // SPIDER'S DEN
    GRAVEL(Blocks.gravel, { inSpidersDen }, OreCategory.BLOCK),

    // END
    END_STONE(Blocks.end_stone, { inEnd }, OreCategory.BLOCK),
    OBSIDIAN(Blocks.obsidian, { inCrystalHollows || inMineshaft || inEnd }, OreCategory.ORE),

    // HARD STONE
    HARD_STONE_HOLLOWS(::isHardStoneHollows, { inCrystalHollows }, OreCategory.BLOCK),
    HARD_STONE_TUNNELS(::isHardstoneTunnels, { inTunnels }, OreCategory.BLOCK),
    HARD_STONE_MINESHAFT(::isHardstoneMineshaft, { inMineshaft }, OreCategory.BLOCK),

    // DWARVEN BLOCKS
    PURE_COAL(Blocks.coal_block, { inDwarvenMines || inCrystalHollows }, OreCategory.ORE),
    PURE_IRON(Blocks.iron_block, { inDwarvenMines || inCrystalHollows }, OreCategory.ORE, hasInitSound = false),
    PURE_GOLD(
        Blocks.gold_block,
        { inDwarvenMines || inCrystalHollows || inMineshaft },
        OreCategory.ORE, hasInitSound = false,
    ),
    PURE_LAPIS(Blocks.lapis_block, { inDwarvenMines || inCrystalHollows }, OreCategory.ORE),
    PURE_REDSTONE(Blocks.redstone_block, { inDwarvenMines || inCrystalHollows }, OreCategory.ORE, hasInitSound = false),
    PURE_EMERALD(Blocks.emerald_block, { inDwarvenMines || inCrystalHollows }, OreCategory.ORE, hasInitSound = false),
    PURE_DIAMOND(Blocks.diamond_block, { inDwarvenMines || inCrystalHollows }, OreCategory.ORE, hasInitSound = false),

    // GEMSTONES
    RUBY(EnumDyeColor.RED, { inCrystalHollows || inGlacite }, OreCategory.GEMSTONE),
    AMBER(EnumDyeColor.ORANGE, { inCrystalHollows || inGlacite }, OreCategory.GEMSTONE),
    AMETHYST(EnumDyeColor.PURPLE, { inCrystalHollows || inGlacite }, OreCategory.GEMSTONE),
    JADE(EnumDyeColor.LIME, { inCrystalHollows || inGlacite }, OreCategory.GEMSTONE),
    SAPPHIRE(EnumDyeColor.LIGHT_BLUE, { inCrystalHollows || inGlacite }, OreCategory.GEMSTONE),
    TOPAZ(EnumDyeColor.YELLOW, { inCrystalHollows || inGlacite }, OreCategory.GEMSTONE),
    JASPER(EnumDyeColor.MAGENTA, { inCrystalHollows || inMineshaft }, OreCategory.GEMSTONE),
    OPAL(EnumDyeColor.WHITE, { inMineshaft || inCrimsonIsle }, OreCategory.GEMSTONE),
    AQUAMARINE(EnumDyeColor.BLUE, { inGlacite }, OreCategory.GEMSTONE),
    CITRINE(EnumDyeColor.BROWN, { inGlacite }, OreCategory.GEMSTONE),
    ONYX(EnumDyeColor.BLACK, { inGlacite }, OreCategory.GEMSTONE),
    PERIDOT(EnumDyeColor.GREEN, { inGlacite }, OreCategory.GEMSTONE),

    // GLACIAL
    LOW_TIER_UMBER(::isLowTierUmber, { inGlacite }, OreCategory.DWARVEN_METAL),
    MID_TIER_UMBER(::isMidTierUmber, { inGlacite }, OreCategory.DWARVEN_METAL),
    HIGH_TIER_UMBER(::isHighTierUmber, { inGlacite }, OreCategory.DWARVEN_METAL),

    LOW_TIER_TUNGSTEN_TUNNELS(::isLowTierTungstenTunnels, { inTunnels }, OreCategory.DWARVEN_METAL),
    LOW_TIER_TUNGSTEN_MINESHAFT(::isLowTierTungstenMineshaft, { inMineshaft }, OreCategory.DWARVEN_METAL),
    HIGH_TIER_TUNGSTEN(Blocks.clay, { inGlacite }, OreCategory.DWARVEN_METAL),

    GLACITE(Blocks.packed_ice, { inGlacite }, OreCategory.DWARVEN_METAL),
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

    constructor(gemstoneColor: EnumDyeColor, checkArea: () -> Boolean, category: OreCategory, hasInitSound: Boolean = true) :
        this({ it.isGemstoneWithColor(gemstoneColor) }, checkArea, category, hasInitSound)

    companion object {
        fun getByStateOrNull(state: IBlockState): OreBlock? = currentAreaOreBlocks.find { it.checkBlock(state) }

        fun getByNameOrNull(string: String) = entries.firstOrNull { it.name == string }
    }
}

private fun isLowTierMithril(state: IBlockState): Boolean = when (state.block) {
    //#if MC < 1.16
    Blocks.wool -> state.getValue(BlockColored.COLOR) == EnumDyeColor.GRAY
    Blocks.stained_hardened_clay -> state.getValue(BlockColored.COLOR) == EnumDyeColor.CYAN
    //#else
    //$$ Blocks.GRAY_WOOL -> true
    //$$ Blocks.CYAN_TERRACOTTA -> true
    //#endif
    else -> false
}

private fun isHighTierMithril(state: IBlockState): Boolean {
    //#if MC < 1.16
    return (state.block == Blocks.wool && state.getValue(BlockColored.COLOR) == EnumDyeColor.LIGHT_BLUE)
    //#else
    //$$ return state.block == Blocks.LIGHT_BLUE_WOOL
    //#endif
}

fun isTitanium(state: IBlockState): Boolean {
    //#if MC < 1.16
    return (state.block == Blocks.stone && state.getValue(BlockStone.VARIANT) == BlockStone.EnumType.DIORITE_SMOOTH)
    //#else
    //$$ return state.block == Blocks.POLISHED_DIORITE
    //#endif
}

private fun isStone(state: IBlockState): Boolean {
    //#if MC < 1.16
    return (state.block == Blocks.stone && state.getValue(BlockStone.VARIANT) == BlockStone.EnumType.STONE)
    //#else
    //$$ return state.block == Blocks.STONE
    //#endif
}

private fun isHardStoneHollows(state: IBlockState): Boolean {
    return when (state.block) {
        //#if MC < 1.16
        Blocks.wool -> {
            val color = state.getValue(BlockColored.COLOR)
            color == EnumDyeColor.GRAY || color == EnumDyeColor.GREEN
        }

        Blocks.stained_hardened_clay -> when (state.getValue(BlockColored.COLOR)) {
            EnumDyeColor.CYAN, EnumDyeColor.BROWN, EnumDyeColor.GRAY, EnumDyeColor.BLACK,
            EnumDyeColor.LIME, EnumDyeColor.GREEN, EnumDyeColor.BLUE, EnumDyeColor.RED,
            EnumDyeColor.SILVER,
            -> true

            else -> false
        }

        Blocks.clay, Blocks.stonebrick, Blocks.stone -> true
        //#else
        //$$ Blocks.GRAY_WOOL -> true
        //$$ Blocks.GREEN_WOOL -> true
        //$$ Blocks.CYAN_TERRACOTTA -> true
        //$$ Blocks.BROWN_TERRACOTTA -> true
        //$$ Blocks.GRAY_TERRACOTTA -> true
        //$$ Blocks.BLACK_TERRACOTTA -> true
        //$$ Blocks.LIME_TERRACOTTA -> true
        //$$ Blocks.GREEN_TERRACOTTA -> true
        //$$ Blocks.BLUE_TERRACOTTA -> true
        //$$ Blocks.RED_TERRACOTTA -> true
        //$$ Blocks.LIGHT_GRAY_TERRACOTTA -> true
        //$$ Blocks.CLAY -> true
        //$$ Blocks.STONE_BRICKS -> true
        //$$ Blocks.MOSSY_STONE_BRICKS -> true
        //$$ Blocks.CRACKED_STONE_BRICKS -> true
        //$$ Blocks.CHISELED_STONE_BRICKS -> true
        //$$ Blocks.STONE -> true
        //$$ Blocks.DIORITE -> true
        //$$ Blocks.GRANITE -> true
        //$$ Blocks.ANDESITE -> true
        //#endif
        else -> false
    }
}

private fun isHardstoneTunnels(state: IBlockState): Boolean =
    //#if MC < 1.16
    (state.block == Blocks.monster_egg && state.getValue(BlockSilverfish.VARIANT) == BlockSilverfish.EnumType.STONE) ||
        (state.block == Blocks.wool && state.getValue(BlockColored.COLOR) == EnumDyeColor.SILVER)
//#else
//$$ state.block == Blocks.INFESTED_STONE || state.block == Blocks.LIGHT_GRAY_WOOL
//#endif

private fun isHardstoneMineshaft(state: IBlockState): Boolean =
    //#if MC < 1.16
    (state.block == Blocks.stone && state.getValue(BlockStone.VARIANT) == BlockStone.EnumType.STONE) ||
        (state.block == Blocks.wool && state.getValue(BlockColored.COLOR) == EnumDyeColor.SILVER)
//#else
//$$ state.block == Blocks.STONE || state.block == Blocks.LIGHT_GRAY_WOOL
//#endif

private fun isRedSand(state: IBlockState): Boolean =
    //#if MC < 1.16
    (state.block == Blocks.sand && state.getValue(BlockSand.VARIANT) == BlockSand.EnumType.RED_SAND)
//#else
//$$ state.block == Blocks.RED_SAND
//#endif

private fun isLowTierUmber(state: IBlockState): Boolean =
    state.block == Blocks.hardened_clay

private fun isMidTierUmber(state: IBlockState): Boolean =
    //#if MC < 1.16
    (state.block == Blocks.stained_hardened_clay && state.getValue(BlockColored.COLOR) == EnumDyeColor.BROWN)
//#else
//$$ state.block == Blocks.BROWN_TERRACOTTA
//#endif

private fun isHighTierUmber(state: IBlockState): Boolean =
    //#if MC < 1.16
    (state.block == Blocks.double_stone_slab2 && state.getValue(BlockStoneSlabNew.VARIANT) == BlockStoneSlabNew.EnumType.RED_SANDSTONE)
//#else
//$$ state.block == Blocks.RED_SANDSTONE_SLAB
//#endif

private fun isLowTierTungstenTunnels(state: IBlockState): Boolean =
    //#if MC < 1.16
    state.block == Blocks.monster_egg && state.getValue(BlockSilverfish.VARIANT) == BlockSilverfish.EnumType.COBBLESTONE
//#else
//$$ state.block == Blocks.INFESTED_COBBLESTONE
//#endif

private fun isLowTierTungstenMineshaft(state: IBlockState): Boolean = when (state.block) {
    //#if MC < 1.16
    Blocks.stone_slab -> state.getValue(BlockStoneSlab.VARIANT) == BlockStoneSlab.EnumType.COBBLESTONE
    //#else
    //$$ Blocks.COBBLESTONE_SLAB -> true
    //#endif
    Blocks.cobblestone, Blocks.stone_stairs -> true
    else -> false
}

private fun IBlockState.isGemstoneWithColor(color: EnumDyeColor): Boolean = when (block) {
    //#if MC < 1.16
    Blocks.stained_glass -> color == getValue(BlockStainedGlass.COLOR)
    Blocks.stained_glass_pane -> color == getValue(BlockStainedGlassPane.COLOR)
    //#else
    //$$ is StainedGlassBlock -> (block as StainedGlassBlock).color == color
    //$$ is StainedGlassPaneBlock -> (block as StainedGlassPaneBlock).color == color
    //#endif
    else -> false
}
