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
import at.hannibal2.skyhanni.data.jsonobjects.repo.MiningJson
import at.hannibal2.skyhanni.utils.CollectionUtils.equalsOneOf
import at.hannibal2.skyhanni.utils.json.BaseGsonBuilder
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
import java.io.File
import kotlin.math.ceil
import kotlin.math.round

enum class OreBlock(
    val checkBlock: (IBlockState) -> Boolean,
    val checkArea: () -> Boolean,
    val strengthI: Int,
    val isOre: Boolean,
) {
    // MITHRIL
    LOW_TIER_MITHRIL(
        checkBlock = ::isLowTierMithril,
        checkArea = { inDwarvenMines || inGlacite },
        500, true,
    ),
    MID_TIER_MITHRIL(
        checkBlock = { it.block == Blocks.prismarine },
        checkArea = { inDwarvenMines || inCrystalHollows || inGlacite },
        800, true,
    ),
    HIGH_TIER_MITHRIL(
        checkBlock = ::isHighTierMithril,
        checkArea = { inDwarvenMines || inCrystalHollows || inGlacite },
        1500, true,
    ),

    // TITANIUM
    TITANIUM(
        checkBlock = ::isTitanium,
        checkArea = { inDwarvenMines || inGlacite },
        2000, true,
    ),

    // VANILLA ORES
    STONE(
        checkBlock = ::isStone,
        checkArea = { inDwarvenMines },
        15, false,
    ),
    COBBLESTONE(
        checkBlock = { it.block == Blocks.cobblestone },
        checkArea = { inDwarvenMines },
        20, false,
    ),
    COAL_ORE(
        checkBlock = { it.block == Blocks.coal_ore },
        checkArea = { inDwarvenMines || inCrystalHollows },
        30, true,
    ),
    IRON_ORE(
        checkBlock = { it.block == Blocks.iron_ore },
        checkArea = { inDwarvenMines || inCrystalHollows },
        30, true,
    ),
    GOLD_ORE(
        checkBlock = { it.block == Blocks.gold_ore },
        checkArea = { inDwarvenMines || inCrystalHollows },
        30, true,
    ),
    LAPIS_ORE(
        checkBlock = { it.block == Blocks.lapis_ore },
        checkArea = { inDwarvenMines || inCrystalHollows },
        30, true,
    ),
    REDSTONE_ORE(
        checkBlock = { it.block.equalsOneOf(Blocks.redstone_ore, Blocks.lit_redstone_ore) },
        checkArea = { inDwarvenMines || inCrystalHollows },
        30, true,
    ),
    EMERALD_ORE(
        checkBlock = { it.block == Blocks.emerald_ore },
        checkArea = { inDwarvenMines || inCrystalHollows },
        30, true,
    ),
    DIAMOND_ORE(
        checkBlock = { it.block == Blocks.diamond_ore },
        checkArea = { inDwarvenMines || inCrystalHollows },
        30, true,
    ),

    // NETHER
    NETHERRACK(
        checkBlock = { it.block == Blocks.netherrack },
        checkArea = { inCrimsonIsle },
        8, false,
    ),
    QUARTZ_ORE(
        checkBlock = { it.block == Blocks.quartz_ore },
        checkArea = { inCrystalHollows || inCrimsonIsle },
        30, true,
    ),
    GLOWSTONE(
        checkBlock = { it.block == Blocks.glowstone },
        checkArea = { inCrimsonIsle },
        -1, false,
    ),
    MYCELIUM(
        checkBlock = { it.block == Blocks.mycelium },
        checkArea = { inCrimsonIsle },
        -1, false,
    ),
    RED_SAND(
        checkBlock = ::isRedSand,
        checkArea = { inCrimsonIsle },
        -1, false,
    ),
    SULPHUR(
        checkBlock = { it.block == Blocks.sponge },
        checkArea = { inCrimsonIsle },
        500, true,
    ),

    // SPIDER'S DEN
    GRAVEL(
        checkBlock = { it.block == Blocks.gravel },
        checkArea = { inSpidersDen },
        -1, false,
    ),

    // END
    END_STONE(
        checkBlock = { it.block == Blocks.end_stone },
        checkArea = { inEnd },
        30, true,
    ),
    OBSIDIAN(
        checkBlock = { it.block == Blocks.obsidian },
        checkArea = { inCrystalHollows || inEnd },
        500, true,
    ),

    // HARD STONE
    HARD_STONE_HOLLOWS(
        checkBlock = ::isHardStoneHollows,
        checkArea = { inCrystalHollows },
        50, false,
    ),
    HARD_STONE_TUNNELS(
        checkBlock = ::isHardstoneTunnels,
        checkArea = { inTunnels },
        50, false,
    ),
    HARD_STONE_MINESHAFT(
        checkBlock = ::isHardstoneMineshaft,
        checkArea = { inMineshaft },
        50, false,
    ),

    // DWARVEN BLOCKS
    PURE_COAL(
        checkBlock = { it.block == Blocks.coal_block },
        checkArea = { inDwarvenMines || inCrystalHollows },
        600, true,
    ),
    PURE_IRON(
        // currently not detected
        checkBlock = { it.block == Blocks.iron_block },
        checkArea = { inDwarvenMines || inCrystalHollows }, 600, true,
    ),
    PURE_GOLD(
        // currently not detected
        checkBlock = { it.block == Blocks.gold_block },
        checkArea = { inDwarvenMines || inCrystalHollows }, 600, true,
    ),
    PURE_LAPIS(
        checkBlock = { it.block == Blocks.lapis_block },
        checkArea = { inDwarvenMines || inCrystalHollows }, 600, true,
    ),
    PURE_REDSTONE(
        // currently not detected
        checkBlock = { it.block == Blocks.redstone_block },
        checkArea = { inDwarvenMines || inCrystalHollows }, 600, true,
    ),
    PURE_EMERALD(
        // currently not detected
        checkBlock = { it.block == Blocks.emerald_block },
        checkArea = { inDwarvenMines || inCrystalHollows }, 600, true,
    ),
    PURE_DIAMOND(
        // currently not detected
        checkBlock = { it.block == Blocks.diamond_block },
        checkArea = { inDwarvenMines || inCrystalHollows }, 600, true,
    ),

    // GEMSTONES
    RUBY(
        checkBlock = { it.isGemstoneWithColor(EnumDyeColor.RED) },
        checkArea = { inCrystalHollows || inGlacite }, 2300, true,
    ),
    AMBER(
        checkBlock = { it.isGemstoneWithColor(EnumDyeColor.ORANGE) },
        checkArea = { inCrystalHollows || inGlacite }, 3000, true,
    ),
    AMETHYST(
        checkBlock = { it.isGemstoneWithColor(EnumDyeColor.PURPLE) },
        checkArea = { inCrystalHollows || inGlacite }, 3000, true,
    ),
    JADE(
        checkBlock = { it.isGemstoneWithColor(EnumDyeColor.LIME) },
        checkArea = { inCrystalHollows || inGlacite }, 3000, true,
    ),
    SAPPHIRE(
        checkBlock = { it.isGemstoneWithColor(EnumDyeColor.LIGHT_BLUE) },
        checkArea = { inCrystalHollows || inGlacite }, 3000, true,
    ),
    TOPAZ(
        checkBlock = { it.isGemstoneWithColor(EnumDyeColor.YELLOW) },
        checkArea = { inCrystalHollows || inGlacite }, 3800, true,
    ),
    JASPER(
        checkBlock = { it.isGemstoneWithColor(EnumDyeColor.MAGENTA) },
        checkArea = { inCrystalHollows || inGlacite }, 4800, true,
    ),
    OPAL(
        checkBlock = { it.isGemstoneWithColor(EnumDyeColor.WHITE) },
        checkArea = { inGlacite || inCrimsonIsle }, 3000, true,
    ),
    AQUAMARINE(
        checkBlock = { it.isGemstoneWithColor(EnumDyeColor.BLUE) },
        checkArea = { inGlacite }, 5200, true,
    ),
    CITRINE(
        checkBlock = { it.isGemstoneWithColor(EnumDyeColor.BROWN) },
        checkArea = { inGlacite }, 5200, true,
    ),
    ONYX(
        checkBlock = { it.isGemstoneWithColor(EnumDyeColor.BLACK) },
        checkArea = { inGlacite }, 5200, true,
    ),
    PERIDOT(
        checkBlock = { it.isGemstoneWithColor(EnumDyeColor.GREEN) },
        checkArea = { inGlacite }, 5200, true,
    ),

    // GLACIAL
    LOW_TIER_UMBER(
        checkBlock = ::isLowTierUmber,
        checkArea = { inGlacite }, 5600, true,
    ),
    HIGH_TIER_UMBER(
        checkBlock = ::isHighTierUmber,
        checkArea = { inGlacite }, 5600, true,
    ),

    LOW_TIER_TUNGSTEN_TUNNELS(
        checkBlock = ::isLowTierTungstenTunnels,
        checkArea = { inTunnels }, 5600, true,
    ),
    LOW_TIER_TUNGSTEN_MINESHAFT(
        checkBlock = ::isLowTierTungstenMineshaft,
        checkArea = { inMineshaft }, 5600, true,
    ),
    HIGH_TIER_TUNGSTEN(
        checkBlock = { it.block == Blocks.clay },
        checkArea = { inGlacite }, 5600, true,
    ),

    GLACITE(
        checkBlock = { it.block == Blocks.packed_ice },
        checkArea = { inGlacite }, 6000, true,
    ),
    ;

    val strength get() = MiningAPI.blockStrengths[this] ?: 0

    val speedSoftCap get() = ceil(20.0 / 3.0 * strength).toInt()

    val speedForInstantMine get() = strength * if (isOre) 60 else 30

    fun miningTicks(speed: Double): Int = when {
        speed >= speedForInstantMine -> 1
        speed >= speedSoftCap -> 4
        else -> round((strength * 30.0) / speed).toInt()
    }

    companion object {
        fun getByStateOrNull(state: IBlockState): OreBlock? = currentAreaOreBlocks.firstOrNull { it.checkBlock(state) }

        fun getByNameOrNull(string: String) = entries.firstOrNull { it.name == string }

        fun toRepo() {
            val map = entries.associateBy({ it.name }, { it.strengthI })
            val json = BaseGsonBuilder.gson().create().toJson(MiningJson(map))
            File("G:\\User FIles\\HypixelMods\\SkyHanni-REPO\\Mining.json").writeText(json)
        }
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
