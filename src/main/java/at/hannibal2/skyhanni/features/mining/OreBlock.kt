package at.hannibal2.skyhanni.features.mining

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
//#if MC < 1.16
import net.minecraft.block.BlockColored
import net.minecraft.block.BlockSand
import net.minecraft.block.BlockSilverfish
import net.minecraft.block.BlockStone
import net.minecraft.block.BlockStoneSlab
import net.minecraft.block.BlockStoneSlabNew
//#endif

enum class OreBlock(
    val checkBlock: (IBlockState) -> Boolean,
    val checkArea: () -> Boolean,
    val hasInitSound: Boolean = true,
) {
    // MITHRIL
    LOW_TIER_MITHRIL(::isLowTierMithril, { inDwarvenMines || inGlacite }),
    MID_TIER_MITHRIL(Blocks.prismarine, { inDwarvenMines || inCrystalHollows || inGlacite }),
    HIGH_TIER_MITHRIL(::isHighTierMithril, { inDwarvenMines || inCrystalHollows || inGlacite }),

    // TITANIUM
    TITANIUM(::isTitanium, { inDwarvenMines || inGlacite }),

    // VANILLA ORES
    STONE(::isStone, { inDwarvenMines }),
    COBBLESTONE(Blocks.cobblestone, { inDwarvenMines }),
    COAL_ORE(Blocks.coal_ore, { inDwarvenMines || inCrystalHollows }),
    IRON_ORE(Blocks.iron_ore, { inDwarvenMines || inCrystalHollows }),
    GOLD_ORE(Blocks.gold_ore, { inDwarvenMines || inCrystalHollows }),
    LAPIS_ORE(Blocks.lapis_ore, { inDwarvenMines || inCrystalHollows }),
    REDSTONE_ORE(
        { it.block.equalsOneOf(BlockUtils.redstoneOreBlocks) },
        { inDwarvenMines || inCrystalHollows },
    ),
    EMERALD_ORE(Blocks.emerald_ore, { inDwarvenMines || inCrystalHollows }),
    DIAMOND_ORE(Blocks.diamond_ore, { inDwarvenMines || inCrystalHollows }),

    // NETHER
    NETHERRACK(Blocks.netherrack, { inCrimsonIsle }),
    QUARTZ_ORE(Blocks.quartz_ore, { inCrystalHollows || inCrimsonIsle }),
    GLOWSTONE(Blocks.glowstone, { inCrimsonIsle }),
    MYCELIUM(Blocks.mycelium, { inCrimsonIsle }),
    RED_SAND(::isRedSand, { inCrimsonIsle }),
    SULPHUR(Blocks.sponge, { inCrimsonIsle }),

    // SPIDER'S DEN
    GRAVEL(Blocks.gravel, { inSpidersDen }),

    // END
    END_STONE(Blocks.end_stone, { inEnd }),
    OBSIDIAN(Blocks.obsidian, { inCrystalHollows || inMineshaft || inEnd }),

    // HARD STONE
    HARD_STONE_HOLLOWS(::isHardStoneHollows, { inCrystalHollows }),
    HARD_STONE_TUNNELS(::isHardstoneTunnels, { inTunnels }),
    HARD_STONE_MINESHAFT(::isHardstoneMineshaft, { inMineshaft }),

    // DWARVEN BLOCKS
    PURE_COAL(Blocks.coal_block, { inDwarvenMines || inCrystalHollows }),
    PURE_IRON(Blocks.iron_block, { inDwarvenMines || inCrystalHollows }, hasInitSound = false),
    PURE_GOLD(Blocks.gold_block, { inDwarvenMines || inCrystalHollows || inMineshaft }, hasInitSound = false),
    PURE_LAPIS(Blocks.lapis_block, { inDwarvenMines || inCrystalHollows }),
    PURE_REDSTONE(Blocks.redstone_block, { inDwarvenMines || inCrystalHollows }, hasInitSound = false),
    PURE_EMERALD(Blocks.emerald_block, { inDwarvenMines || inCrystalHollows }, hasInitSound = false),
    PURE_DIAMOND(Blocks.diamond_block, { inDwarvenMines || inCrystalHollows }, hasInitSound = false),

    // GEMSTONES
    RUBY(EnumDyeColor.RED, { inCrystalHollows || inGlacite }),
    AMBER(EnumDyeColor.ORANGE, { inCrystalHollows || inGlacite }),
    AMETHYST(EnumDyeColor.PURPLE, { inCrystalHollows || inGlacite }),
    JADE(EnumDyeColor.LIME, { inCrystalHollows || inGlacite }),
    SAPPHIRE(EnumDyeColor.LIGHT_BLUE, { inCrystalHollows || inGlacite }),
    TOPAZ(EnumDyeColor.YELLOW, { inCrystalHollows || inGlacite }),
    JASPER(EnumDyeColor.MAGENTA, { inCrystalHollows || inMineshaft }),
    OPAL(EnumDyeColor.WHITE, { inMineshaft || inCrimsonIsle }),
    AQUAMARINE(EnumDyeColor.BLUE, { inGlacite }),
    CITRINE(EnumDyeColor.BROWN, { inGlacite }),
    ONYX(EnumDyeColor.BLACK, { inGlacite }),
    PERIDOT(EnumDyeColor.GREEN, { inGlacite }),

    // GLACIAL
    LOW_TIER_UMBER(::isLowTierUmber, { inGlacite }),
    MID_TIER_UMBER(::isMidTierUmber, { inGlacite }),
    HIGH_TIER_UMBER(::isHighTierUmber, { inGlacite }),

    LOW_TIER_TUNGSTEN_TUNNELS(::isLowTierTungstenTunnels, { inTunnels }),
    LOW_TIER_TUNGSTEN_MINESHAFT(::isLowTierTungstenMineshaft, { inMineshaft }),
    HIGH_TIER_TUNGSTEN(Blocks.clay, { inGlacite }),

    GLACITE(Blocks.packed_ice, { inGlacite }),
    ;

    constructor(block: Block, checkArea: () -> Boolean, hasInitSound: Boolean = true) :
        this({ it.block == block }, checkArea, hasInitSound)

    constructor(gemstoneColor: EnumDyeColor, checkArea: () -> Boolean, hasInitSound: Boolean = true) :
        this({ it.isGemstoneWithColor(gemstoneColor) }, checkArea, hasInitSound)

    companion object {
        fun getByStateOrNull(state: IBlockState): OreBlock? = currentAreaOreBlocks.find { it.checkBlock(state) }
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
