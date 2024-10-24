package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.data.MiningAPI
import at.hannibal2.skyhanni.data.MiningAPI.currentAreaOreBlocks
import at.hannibal2.skyhanni.data.MiningAPI.inCrimsonIsle
import at.hannibal2.skyhanni.data.MiningAPI.inCrystalHollows
import at.hannibal2.skyhanni.data.MiningAPI.inDwarvenMines
import at.hannibal2.skyhanni.data.MiningAPI.inEnd
import at.hannibal2.skyhanni.data.MiningAPI.inGlacite
import at.hannibal2.skyhanni.data.MiningAPI.inMineshaft
import at.hannibal2.skyhanni.data.MiningAPI.inSpidersDen
import at.hannibal2.skyhanni.data.MiningAPI.inTunnels
import at.hannibal2.skyhanni.utils.CollectionUtils.equalsOneOf
import net.minecraft.block.BlockColored
import net.minecraft.block.BlockSand
import net.minecraft.block.BlockSilverfish
import net.minecraft.block.BlockStainedGlass
import net.minecraft.block.BlockStainedGlassPane
import net.minecraft.block.BlockStone
import net.minecraft.block.BlockStoneSlab
import net.minecraft.block.BlockStoneSlabNew
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.item.EnumDyeColor
import kotlin.math.ceil
import kotlin.math.round

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
) {
    // MITHRIL
    LOW_TIER_MITHRIL(
        checkBlock = ::isLowTierMithril,
        checkArea = { inDwarvenMines || inGlacite },
        OreCategory.DWARVEN_METAL,
    ),
    MID_TIER_MITHRIL(
        checkBlock = { it.block == Blocks.prismarine },
        checkArea = { inDwarvenMines || inCrystalHollows || inGlacite },
        OreCategory.DWARVEN_METAL,
    ),
    HIGH_TIER_MITHRIL(
        checkBlock = ::isHighTierMithril,
        checkArea = { inDwarvenMines || inCrystalHollows || inGlacite },
        OreCategory.DWARVEN_METAL,
    ),

    // TITANIUM
    TITANIUM(
        checkBlock = ::isTitanium,
        checkArea = { inDwarvenMines || inGlacite },
        OreCategory.DWARVEN_METAL,
    ),

    // VANILLA ORES
    STONE(
        checkBlock = ::isStone,
        checkArea = { inDwarvenMines },
        OreCategory.BLOCK,
    ),
    COBBLESTONE(
        checkBlock = { it.block == Blocks.cobblestone },
        checkArea = { inDwarvenMines },
        OreCategory.BLOCK,
    ),
    COAL_ORE(
        checkBlock = { it.block == Blocks.coal_ore },
        checkArea = { inDwarvenMines || inCrystalHollows },
        OreCategory.ORE,
    ),
    IRON_ORE(
        checkBlock = { it.block == Blocks.iron_ore },
        checkArea = { inDwarvenMines || inCrystalHollows },
        OreCategory.ORE,
    ),
    GOLD_ORE(
        checkBlock = { it.block == Blocks.gold_ore },
        checkArea = { inDwarvenMines || inCrystalHollows },
        OreCategory.ORE,
    ),
    LAPIS_ORE(
        checkBlock = { it.block == Blocks.lapis_ore },
        checkArea = { inDwarvenMines || inCrystalHollows },
        OreCategory.ORE,
    ),
    REDSTONE_ORE(
        checkBlock = { it.block.equalsOneOf(Blocks.redstone_ore, Blocks.lit_redstone_ore) },
        checkArea = { inDwarvenMines || inCrystalHollows },
        OreCategory.ORE,
    ),
    EMERALD_ORE(
        checkBlock = { it.block == Blocks.emerald_ore },
        checkArea = { inDwarvenMines || inCrystalHollows },
        OreCategory.ORE,
    ),
    DIAMOND_ORE(
        checkBlock = { it.block == Blocks.diamond_ore },
        checkArea = { inDwarvenMines || inCrystalHollows },
        OreCategory.ORE,
    ),

    // NETHER
    NETHERRACK(
        checkBlock = { it.block == Blocks.netherrack },
        checkArea = { inCrimsonIsle },
        OreCategory.BLOCK,
    ),
    QUARTZ_ORE(
        checkBlock = { it.block == Blocks.quartz_ore },
        checkArea = { inCrystalHollows || inCrimsonIsle },
        OreCategory.ORE,
    ),
    GLOWSTONE(
        checkBlock = { it.block == Blocks.glowstone },
        checkArea = { inCrimsonIsle },
        OreCategory.BLOCK,
    ),
    MYCELIUM(
        checkBlock = { it.block == Blocks.mycelium },
        checkArea = { inCrimsonIsle },
        OreCategory.BLOCK,
    ),
    RED_SAND(
        checkBlock = ::isRedSand,
        checkArea = { inCrimsonIsle },
        OreCategory.BLOCK,
    ),
    SULPHUR(
        checkBlock = { it.block == Blocks.sponge },
        checkArea = { inCrimsonIsle },
        OreCategory.ORE,
    ),

    // SPIDER'S DEN
    GRAVEL(
        checkBlock = { it.block == Blocks.gravel },
        checkArea = { inSpidersDen },
        OreCategory.BLOCK,
    ),

    // END
    END_STONE(
        checkBlock = { it.block == Blocks.end_stone },
        checkArea = { inEnd },
        OreCategory.BLOCK,
    ),
    OBSIDIAN(
        checkBlock = { it.block == Blocks.obsidian },
        checkArea = { inCrystalHollows || inEnd },
        OreCategory.ORE,
    ),

    // HARD STONE
    HARD_STONE_HOLLOWS(
        checkBlock = ::isHardStoneHollows,
        checkArea = { inCrystalHollows },
        OreCategory.BLOCK,
    ),
    HARD_STONE_TUNNELS(
        checkBlock = ::isHardstoneTunnels,
        checkArea = { inTunnels },
        OreCategory.BLOCK,
    ),
    HARD_STONE_MINESHAFT(
        checkBlock = ::isHardstoneMineshaft,
        checkArea = { inMineshaft },
        OreCategory.BLOCK,
    ),

    // DWARVEN BLOCKS
    PURE_COAL(
        checkBlock = { it.block == Blocks.coal_block },
        checkArea = { inDwarvenMines || inCrystalHollows },
        OreCategory.ORE,
    ),
    PURE_IRON(
        // currently not detected
        checkBlock = { it.block == Blocks.iron_block },
        checkArea = { inDwarvenMines || inCrystalHollows }, OreCategory.ORE,
    ),
    PURE_GOLD(
        // currently not detected
        checkBlock = { it.block == Blocks.gold_block },
        checkArea = { inDwarvenMines || inCrystalHollows }, OreCategory.ORE,
    ),
    PURE_LAPIS(
        checkBlock = { it.block == Blocks.lapis_block },
        checkArea = { inDwarvenMines || inCrystalHollows }, OreCategory.ORE,
    ),
    PURE_REDSTONE(
        // currently not detected
        checkBlock = { it.block == Blocks.redstone_block },
        checkArea = { inDwarvenMines || inCrystalHollows }, OreCategory.ORE,
    ),
    PURE_EMERALD(
        // currently not detected
        checkBlock = { it.block == Blocks.emerald_block },
        checkArea = { inDwarvenMines || inCrystalHollows }, OreCategory.ORE,
    ),
    PURE_DIAMOND(
        // currently not detected
        checkBlock = { it.block == Blocks.diamond_block },
        checkArea = { inDwarvenMines || inCrystalHollows }, OreCategory.ORE,
    ),

    // GEMSTONES
    RUBY(
        checkBlock = { it.isGemstoneWithColor(EnumDyeColor.RED) },
        checkArea = { inCrystalHollows || inGlacite }, OreCategory.GEMSTONE,
    ),
    AMBER(
        checkBlock = { it.isGemstoneWithColor(EnumDyeColor.ORANGE) },
        checkArea = { inCrystalHollows || inGlacite }, OreCategory.GEMSTONE,
    ),
    AMETHYST(
        checkBlock = { it.isGemstoneWithColor(EnumDyeColor.PURPLE) },
        checkArea = { inCrystalHollows || inGlacite }, OreCategory.GEMSTONE,
    ),
    JADE(
        checkBlock = { it.isGemstoneWithColor(EnumDyeColor.LIME) },
        checkArea = { inCrystalHollows || inGlacite }, OreCategory.GEMSTONE,
    ),
    SAPPHIRE(
        checkBlock = { it.isGemstoneWithColor(EnumDyeColor.LIGHT_BLUE) },
        checkArea = { inCrystalHollows || inGlacite }, OreCategory.GEMSTONE,
    ),
    TOPAZ(
        checkBlock = { it.isGemstoneWithColor(EnumDyeColor.YELLOW) },
        checkArea = { inCrystalHollows || inGlacite }, OreCategory.GEMSTONE,
    ),
    JASPER(
        checkBlock = { it.isGemstoneWithColor(EnumDyeColor.MAGENTA) },
        checkArea = { inCrystalHollows || inGlacite }, OreCategory.GEMSTONE,
    ),
    OPAL(
        checkBlock = { it.isGemstoneWithColor(EnumDyeColor.WHITE) },
        checkArea = { inGlacite || inCrimsonIsle }, OreCategory.GEMSTONE,
    ),
    AQUAMARINE(
        checkBlock = { it.isGemstoneWithColor(EnumDyeColor.BLUE) },
        checkArea = { inGlacite }, OreCategory.GEMSTONE,
    ),
    CITRINE(
        checkBlock = { it.isGemstoneWithColor(EnumDyeColor.BROWN) },
        checkArea = { inGlacite }, OreCategory.GEMSTONE,
    ),
    ONYX(
        checkBlock = { it.isGemstoneWithColor(EnumDyeColor.BLACK) },
        checkArea = { inGlacite }, OreCategory.GEMSTONE,
    ),
    PERIDOT(
        checkBlock = { it.isGemstoneWithColor(EnumDyeColor.GREEN) },
        checkArea = { inGlacite }, OreCategory.GEMSTONE,
    ),

    // GLACIAL
    LOW_TIER_UMBER(
        checkBlock = ::isLowTierUmber,
        checkArea = { inGlacite }, OreCategory.DWARVEN_METAL,
    ),
    HIGH_TIER_UMBER(
        checkBlock = ::isHighTierUmber,
        checkArea = { inGlacite }, OreCategory.DWARVEN_METAL,
    ),

    LOW_TIER_TUNGSTEN_TUNNELS(
        checkBlock = ::isLowTierTungstenTunnels,
        checkArea = { inTunnels }, OreCategory.DWARVEN_METAL,
    ),
    LOW_TIER_TUNGSTEN_MINESHAFT(
        checkBlock = ::isLowTierTungstenMineshaft,
        checkArea = { inMineshaft }, OreCategory.DWARVEN_METAL,
    ),
    HIGH_TIER_TUNGSTEN(
        checkBlock = { it.block == Blocks.clay },
        checkArea = { inGlacite }, OreCategory.DWARVEN_METAL,
    ),

    GLACITE(
        checkBlock = { it.block == Blocks.packed_ice },
        checkArea = { inGlacite }, OreCategory.DWARVEN_METAL,
    ),
    ;

    val strength get() = MiningAPI.blockStrengths[this] ?: 0

    val speedSoftCap get() = ceil(20.0 / 3.0 * strength).toInt()

    val speedForInstantMine get() = strength * if (category != OreCategory.BLOCK) 60 else 30

    fun miningTicks(speed: Double): Int = when {
        speed >= speedForInstantMine -> 1
        speed >= speedSoftCap -> 4
        else -> round((strength * 30.0) / speed).toInt()
    }

    companion object {
        fun getByStateOrNull(state: IBlockState): OreBlock? = currentAreaOreBlocks.firstOrNull { it.checkBlock(state) }

        fun getByNameOrNull(string: String) = entries.firstOrNull { it.name == string }
    }
}

private fun isLowTierMithril(state: IBlockState): Boolean = when (state.block) {
    Blocks.wool -> state.getValue(BlockColored.COLOR) == EnumDyeColor.GRAY
    Blocks.stained_hardened_clay -> state.getValue(BlockColored.COLOR) == EnumDyeColor.CYAN
    else -> false
}

private fun isHighTierMithril(state: IBlockState): Boolean {
    return (state.block == Blocks.wool && state.getValue(BlockColored.COLOR) == EnumDyeColor.LIGHT_BLUE)
}

fun isTitanium(state: IBlockState): Boolean {
    return (state.block == Blocks.stone && state.getValue(BlockStone.VARIANT) == BlockStone.EnumType.DIORITE_SMOOTH)
}

private fun isStone(state: IBlockState): Boolean {
    return (state.block == Blocks.stone && state.getValue(BlockStone.VARIANT) == BlockStone.EnumType.STONE)
}

private fun isHardStoneHollows(state: IBlockState): Boolean {
    return when (state.block) {
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
        else -> false
    }
}

private fun isHardstoneTunnels(state: IBlockState): Boolean =
    (state.block == Blocks.monster_egg && state.getValue(BlockSilverfish.VARIANT) == BlockSilverfish.EnumType.STONE) ||
        (state.block == Blocks.wool && state.getValue(BlockColored.COLOR) == EnumDyeColor.SILVER)

private fun isHardstoneMineshaft(state: IBlockState): Boolean =
    (state.block == Blocks.stone && state.getValue(BlockStone.VARIANT) == BlockStone.EnumType.STONE) ||
        (state.block == Blocks.wool && state.getValue(BlockColored.COLOR) == EnumDyeColor.SILVER)

private fun isRedSand(state: IBlockState): Boolean =
    (state.block == Blocks.sand && state.getValue(BlockSand.VARIANT) == BlockSand.EnumType.RED_SAND)

private fun isLowTierUmber(state: IBlockState): Boolean =
    state.block == Blocks.hardened_clay ||
        (state.block == Blocks.stained_hardened_clay && state.getValue(BlockColored.COLOR) == EnumDyeColor.BROWN)

private fun isHighTierUmber(state: IBlockState): Boolean =
    (state.block == Blocks.double_stone_slab2 && state.getValue(BlockStoneSlabNew.VARIANT) == BlockStoneSlabNew.EnumType.RED_SANDSTONE)

private fun isLowTierTungstenTunnels(state: IBlockState): Boolean =
    state.block == Blocks.monster_egg && state.getValue(BlockSilverfish.VARIANT) == BlockSilverfish.EnumType.COBBLESTONE

private fun isLowTierTungstenMineshaft(state: IBlockState): Boolean = when (state.block) {
    Blocks.stone_slab -> state.getValue(BlockStoneSlab.VARIANT) == BlockStoneSlab.EnumType.COBBLESTONE
    Blocks.cobblestone, Blocks.stone_stairs -> true
    else -> false
}

private fun IBlockState.isGemstoneWithColor(color: EnumDyeColor): Boolean = when (block) {
    Blocks.stained_glass -> color == getValue(BlockStainedGlass.COLOR)
    Blocks.stained_glass_pane -> color == getValue(BlockStainedGlassPane.COLOR)
    else -> false
}
