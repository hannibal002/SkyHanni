package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MiningAPI
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import net.minecraft.block.Block
import net.minecraft.block.BlockColored
import net.minecraft.block.BlockSand
import net.minecraft.block.BlockStainedGlass
import net.minecraft.block.BlockStainedGlassPane
import net.minecraft.block.BlockStone
import net.minecraft.block.BlockStoneSlabNew
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.item.EnumDyeColor

enum class OreBlock(
    val blockName: String,
    val internalName: String,
    val oreType: OreType? = null,
    val blocks: List<Block>,
    val function: (IBlockState, EnumDyeColor?) -> Boolean = { _: IBlockState, _: EnumDyeColor? -> true },
    val gemstoneColor: EnumDyeColor? = null,
    private val inDwarvenMines: Boolean = false,
    private val inCrystalHollows: Boolean = false,
    private val inGlacite: Boolean = false,
    private val inCrimsonIsle: Boolean = false,
    private val inEnd: Boolean = false,
    private val inSpidersDen: Boolean = false,
) {
    // MITHRIL
    LOW_TIER_MITHRIL(
        "Low Tier Mithril",
        "MITHRIL_ORE",
        OreType.MITHRIL,
        listOf(Blocks.wool, Blocks.stained_hardened_clay),
        ::isLowTierMithril,
        inDwarvenMines = true, inGlacite = true
    ),
    MID_TIER_MITHRIL(
        "Mid Tier Mithril",
        "MITHRIL_ORE",
        OreType.MITHRIL,
        listOf(Blocks.prismarine),
        inDwarvenMines = true, inCrystalHollows = true, inGlacite = true
    ),
    HIGH_TIER_MITHRIL(
        "High Tier Mithril",
        "MITHRIL_ORE",
        OreType.MITHRIL,
        listOf(Blocks.wool),
        ::isHighTierMithril,
        inDwarvenMines = true, inCrystalHollows = true, inGlacite = true
    ),

    // TITANIUM
    TITANIUM(
        "Titanium",
        "TITANIUM_ORE",
        OreType.TITANIUM,
        listOf(Blocks.stone),
        ::isTitanium,
        inDwarvenMines = true, inGlacite = true
    ),

    // VANILLA ORES
    STONE(
        "Stone",
        "COBBLESTONE",
        OreType.COBBLESTONE,
        listOf(Blocks.stone),
        ::isStone,
        inDwarvenMines = true,
    ),
    COBBLESTONE(
        "Cobblestone",
        "COBBLESTONE",
        OreType.COBBLESTONE,
        listOf(Blocks.cobblestone),
        inDwarvenMines = true
    ),
    COAL_ORE(
        "Coal Ore",
        "COAL",
        OreType.COAL,
        listOf(Blocks.coal_ore),
        inDwarvenMines = true, inCrystalHollows = true
    ),
    IRON_ORE(
        "Iron Ore",
        "IRON_INGOT",
        OreType.IRON,
        listOf(Blocks.iron_ore),
        inDwarvenMines = true, inCrystalHollows = true
    ),
    GOLD_ORE(
        "Gold Ore",
        "GOLD_INGOT",
        OreType.GOLD,
        listOf(Blocks.gold_ore),
        inDwarvenMines = true, inCrystalHollows = true
    ),
    LAPIS_ORE(
        "Lapis Lazuli Ore",
        "INK_SACK-4",
        OreType.LAPIS,
        listOf(Blocks.lapis_ore),
        inDwarvenMines = true, inCrystalHollows = true
    ),
    REDSTONE_ORE(
        "Redstone Ore",
        "REDSTONE",
        OreType.REDSTONE,
        listOf(Blocks.redstone_ore, Blocks.lit_redstone_ore),
        inDwarvenMines = true, inCrystalHollows = true
    ),
    EMERALD_ORE(
        "Emerald Ore",
        "EMERALD",
        OreType.EMERALD,
        listOf(Blocks.emerald_ore),
        inDwarvenMines = true, inCrystalHollows = true,
    ),
    DIAMOND_ORE(
        "Diamond Ore",
        "DIAMOND",
        OreType.DIAMOND,
        listOf(Blocks.diamond_ore),
        inDwarvenMines = true, inCrystalHollows = true
    ),

    // NETHER
    NETHERRACK(
        "Netherrack",
        "NETHERRACK",
        OreType.NETHERRACK,
        listOf(Blocks.netherrack),
        inCrimsonIsle = true
    ),
    QUARTZ_ORE(
        "Nether Quartz Ore",
        "QUARTZ",
        OreType.QUARTZ,
        listOf(Blocks.quartz_ore),
        inCrystalHollows = true, inCrimsonIsle = true
    ),
    GLOWSTONE(
        "Glowstone",
        "GLOWSTONE_DUST",
        OreType.GLOWSTONE,
        listOf(Blocks.glowstone),
        inCrimsonIsle = true
    ),
    MYCELIUM(
        "Mycelium",
        "MYCEL",
        OreType.MYCELIUM,
        listOf(Blocks.mycelium),
        inCrimsonIsle = true
    ),
    RED_SAND(
        "Red Sand",
        "SAND-1",
        OreType.RED_SAND,
        listOf(Blocks.sand),
        ::isRedSand,
        inCrimsonIsle = true
    ),
    SULPHUR(
        "Sulphur",
        "SULPHUR_ORE",
        OreType.SULPHUR,
        listOf(Blocks.sponge),
        inCrimsonIsle = true,
    ),

    // SPIDER'S DEN
    GRAVEL(
        "Gravel",
        "GRAVEL",
        OreType.GRAVEL,
        listOf(Blocks.gravel),
        inSpidersDen = true
    ),

    //END
    END_STONE(
        "End Stone",
        "ENDER_STONE",
        OreType.END_STONE,
        listOf(Blocks.end_stone),
        inEnd = true
    ),
    OBSIDIAN(
        "Obsidian",
        "OBSIDIAN",
        OreType.OBSIDIAN,
        listOf(Blocks.obsidian),
        inCrystalHollows = true, inEnd = true
    ),

    // HARD STONE
    HARD_STONE_HOLLOWS(
        "Hard stone (Hollows)",
        "HARD_STONE",
        OreType.HARD_STONE,
        listOf(Blocks.stone, Blocks.wool, Blocks.stained_hardened_clay),
        ::isHardStoneHollows,
        inCrystalHollows = true
    ),
    HARD_STONE_GLACIAL(
        "Hard Stone (Glacial)",
        "HARD_STONE",
        OreType.HARD_STONE,
        listOf(Blocks.stone, Blocks.wool),
        ::isHardstoneGlacite,
        inGlacite = true
    ),

    // DWARVEN BLOCKS
    DWARVEN_REDSTONE(
        "Dwarven Redstone",
        "REDSTONE",
        OreType.REDSTONE,
        listOf(Blocks.redstone_block),
        inCrystalHollows = true
    ),
    DWARVEN_GOLD(
        "Dwarven Gold",
        "GOLD_INGOT",
        OreType.GOLD,
        listOf(Blocks.gold_block),
        inDwarvenMines = true, inCrystalHollows = true
    ),
    DWARVEN_EMERALD(
        "Dwarven Emerald",
        "EMERALD",
        OreType.EMERALD,
        listOf(Blocks.emerald_block),
        inCrystalHollows = true
    ),
    DWARVEN_DIAMOND(
        "Dwarven Diamond",
        "DIAMOND",
        OreType.DIAMOND,
        listOf(Blocks.diamond_block),
        inCrystalHollows = true
    ),

    // GEMSTONES
    RUBY_BLOCK(
        "Ruby Block",
        "ROUGH_RUBY_GEM",
        OreType.GEMSTONE,
        listOf(Blocks.stained_glass, Blocks.stained_glass_pane),
        ::isGemstoneBlock,
        EnumDyeColor.RED,
        inCrystalHollows = true, inGlacite = true
    ),
    AMBER_BLOCK(
        "Amber Block",
        "ROUGH_AMBER_GEM",
        OreType.GEMSTONE,
        listOf(Blocks.stained_glass, Blocks.stained_glass_pane),
        ::isGemstoneBlock,
        EnumDyeColor.ORANGE,
        inCrystalHollows = true, inGlacite = true
    ),
    AMETHYST_BLOCK(
        "Amethyst Block",
        "ROUGH_AMETHYST_GEM",
        OreType.GEMSTONE,
        listOf(Blocks.stained_glass, Blocks.stained_glass_pane),
        ::isGemstoneBlock,
        EnumDyeColor.PURPLE,
        inCrystalHollows = true, inGlacite = true
    ),
    JADE_BLOCK(
        "Jade Block",
        "ROUGH_JADE_GEM",
        OreType.GEMSTONE,
        listOf(Blocks.stained_glass, Blocks.stained_glass_pane),
        ::isGemstoneBlock,
        EnumDyeColor.LIME,
        inCrystalHollows = true, inGlacite = true
    ),
    SAPPHIRE_BLOCK(
        "Sapphire Block",
        "ROUGH_SAPPHIRE_GEM",
        OreType.GEMSTONE,
        listOf(Blocks.stained_glass, Blocks.stained_glass_pane),
        ::isGemstoneBlock,
        EnumDyeColor.LIGHT_BLUE,
        inCrystalHollows = true, inGlacite = true
    ),
    TOPAZ_BLOCK(
        "Topaz Block",
        "ROUGH_TOPAZ_GEM",
        OreType.GEMSTONE,
        listOf(Blocks.stained_glass, Blocks.stained_glass_pane),
        ::isGemstoneBlock,
        gemstoneColor = EnumDyeColor.YELLOW,
        inCrystalHollows = true, inGlacite = true
    ),
    JASPER_BLOCK(
        "Jasper Block",
        "ROUGH_JASPER_GEM",
        OreType.GEMSTONE,
        listOf(Blocks.stained_glass, Blocks.stained_glass_pane),
        ::isGemstoneBlock,
        EnumDyeColor.PINK,
        inCrystalHollows = true, inGlacite = true
    ),
    OPAL_BLOCK(
        "Opal Block",
        "ROUGH_OPAL_GEM",
        OreType.GEMSTONE,
        listOf(Blocks.stained_glass, Blocks.stained_glass_pane),
        ::isGemstoneBlock,
        EnumDyeColor.WHITE,
        inGlacite = true, inCrimsonIsle = true
    ),
    AQUAMARINE_BLOCK(
        "Aquamarine Block",
        "ROUGH_AQUAMARINE_GEM",
        OreType.GEMSTONE,
        listOf(Blocks.stained_glass, Blocks.stained_glass_pane),
        ::isGemstoneBlock,
        EnumDyeColor.BLUE,
        inGlacite = true
    ),
    CITRINE_BLOCK(
        "Citrine Block",
        "ROUGH_CITRINE_GEM",
        OreType.GEMSTONE,
        listOf(Blocks.stained_glass, Blocks.stained_glass_pane),
        ::isGemstoneBlock,
        EnumDyeColor.BROWN,
        inGlacite = true
    ),
    ONYX_BLOCK(
        "Onyx Block",
        "ROUGH_ONYX_GEM",
        OreType.GEMSTONE,
        listOf(Blocks.stained_glass, Blocks.stained_glass_pane),
        ::isGemstoneBlock,
        EnumDyeColor.BLACK,
        inGlacite = true
    ),
    PERIDOT_BLOCK(
        "Peridot Block",
        "ROUGH_PERIDOT_GEM",
        OreType.GEMSTONE,
        listOf(Blocks.stained_glass, Blocks.stained_glass_pane),
        ::isGemstoneBlock,
        EnumDyeColor.GREEN,
        inGlacite = true
    ),

    // GLACIAL
    LOW_TIER_UMBER(
        "Low Tier Umber",
        "UMBER",
        OreType.UMBER,
        listOf(Blocks.stained_hardened_clay, Blocks.hardened_clay),
        ::isLowTierUmber,
        inGlacite = true
    ),
    HIGH_TIER_UMBER(
        "High Tier Umber",
        "UMBER",
        OreType.UMBER,
        listOf(Blocks.double_stone_slab2),
        ::isHighTierUmber,
        inGlacite = true
    ),
    LOW_TIER_TUNGSTEN(
        "Low Tier Tungsten",
        "TUNGSTEN",
        OreType.TUNGSTEN,
        listOf(Blocks.cobblestone),
        inGlacite = true
    ),
    HIGH_TIER_TUNGSTEN(
        "High Tier Tungsten",
        "TUNGSTEN",
        OreType.TUNGSTEN,
        listOf(Blocks.clay),
        inGlacite = true
    ),
    GLACITE(
        "Glacite",
        "GLACITE",
        OreType.GLACITE,
        listOf(Blocks.packed_ice),
        inGlacite = true
    ),
    ;

    companion object {
        fun getByStateOrNull(state: IBlockState): OreBlock? = OreBlock.entries
            .filter { it.isOre(state) }
            .takeIf { it.size <= 1 }
            ?.firstOrNull()

        private fun OreBlock.inValidLocation(): Boolean {
            return (this.inDwarvenMines && MiningAPI.inRegularDwarven() ||
                this.inCrystalHollows && MiningAPI.inCrystalHollows() ||
                this.inGlacite && MiningAPI.inGlaciteArea() ||
                this.inCrimsonIsle && IslandType.CRIMSON_ISLE.isInIsland() ||
                this.inEnd && IslandType.THE_END.isInIsland() ||
                this.inSpidersDen && IslandType.SPIDER_DEN.isInIsland())
        }
    }

    private fun isOre(state: IBlockState): Boolean = inValidLocation() &&
        blocks.any { it.blockState.block == state.block } &&
        function(state, gemstoneColor)
}

private fun isLowTierMithril(state: IBlockState, a: EnumDyeColor? = null): Boolean {
    val block = state.block
    val color = state.getValue(BlockColored.COLOR)
    return ((block == Blocks.wool && color == EnumDyeColor.GRAY) ||
        (block == Blocks.stained_hardened_clay && color == EnumDyeColor.CYAN))
}

private fun isHighTierMithril(state: IBlockState, a: EnumDyeColor? = null): Boolean {
    return (state.block == Blocks.wool && state.getValue(BlockColored.COLOR) == EnumDyeColor.LIGHT_BLUE)
}

private fun isTitanium(state: IBlockState, a: EnumDyeColor? = null): Boolean {
    return (state.block == Blocks.stone && state.getValue(BlockStone.VARIANT) == BlockStone.EnumType.DIORITE_SMOOTH)
}

private fun isStone(state: IBlockState, a: EnumDyeColor? = null): Boolean {
    return (state.block == Blocks.stone && state.getValue(BlockStone.VARIANT) == BlockStone.EnumType.STONE)
}

private fun isHardStoneHollows(state: IBlockState, a: EnumDyeColor? = null): Boolean {
    return when (state.block) {
        Blocks.wool -> (state.getValue(BlockColored.COLOR) == EnumDyeColor.GRAY)
        Blocks.stained_hardened_clay -> (state.getValue(BlockColored.COLOR) == EnumDyeColor.CYAN)
        Blocks.stone -> (state.getValue(BlockStone.VARIANT) == BlockStone.EnumType.STONE)
        else -> false
    }
}

private fun isHardstoneGlacite(state: IBlockState, a: EnumDyeColor? = null): Boolean {
    return ((state.block == Blocks.stone && state.getValue(BlockStone.VARIANT) == BlockStone.EnumType.STONE) &&
        state.block == Blocks.wool && state.getValue(BlockColored.COLOR) == EnumDyeColor.GRAY)
}

private fun isRedSand(state: IBlockState, a: EnumDyeColor? = null): Boolean {
    return (state.block == Blocks.stone && state.getValue(BlockSand.VARIANT) == BlockSand.EnumType.RED_SAND)
}

private fun isLowTierUmber(state: IBlockState, a: EnumDyeColor? = null): Boolean {
    return (state.block == Blocks.hardened_clay ||
        (state.block == Blocks.stained_hardened_clay && state.getValue(BlockColored.COLOR) == EnumDyeColor.BROWN))
}

private fun isHighTierUmber(state: IBlockState, a: EnumDyeColor? = null): Boolean {
    return (state.block == Blocks.double_stone_slab2 &&
        state.getValue(BlockStoneSlabNew.VARIANT) == BlockStoneSlabNew.EnumType.RED_SANDSTONE)
}

private fun isGemstoneBlock(state: IBlockState, color: EnumDyeColor? = null): Boolean {
    return when (state.block) {
        Blocks.stained_glass -> color == state.getValue(BlockStainedGlass.COLOR)
        Blocks.stained_glass_pane -> color == state.getValue(BlockStainedGlassPane.COLOR)
        else -> false
    }
}
