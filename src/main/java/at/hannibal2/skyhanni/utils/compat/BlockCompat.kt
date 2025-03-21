package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzColor.Companion.toLorenzColor
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
//#if MC > 1.21
//$$ import net.minecraft.registry.tag.BlockTags
//#else
import net.minecraft.block.BlockStainedGlass
//#endif


fun MutableList<Block>.addLeaves() {
    //#if MC < 1.21
    this.add(Blocks.leaves)
    //#else
    //$$ this.add(Blocks.OAK_LEAVES)
    //$$ this.add(Blocks.SPRUCE_LEAVES)
    //$$ this.add(Blocks.BIRCH_LEAVES)
    //$$ this.add(Blocks.JUNGLE_LEAVES)
    //#endif
}

fun MutableList<Block>.addLeaves2() {
    //#if MC < 1.21
    this.add(Blocks.leaves2)
    //#else
    //$$ this.add(Blocks.ACACIA_LEAVES)
    //$$ this.add(Blocks.DARK_OAK_LEAVES)
    //#endif
}

fun MutableList<Block>.addTallGrass() {
    //#if MC < 1.21
    this.add(Blocks.tallgrass)
    //#else
    //$$ this.add(Blocks.SHORT_GRASS)
    //$$ this.add(Blocks.FERN)
    //#endif
}

fun MutableList<Block>.addDoublePlant() {
    //#if MC < 1.21
    this.add(Blocks.double_plant)
    //#else
    //$$ this.add(Blocks.SUNFLOWER)
    //$$ this.add(Blocks.LILAC)
    //$$ this.add(Blocks.TALL_GRASS)
    //$$ this.add(Blocks.LARGE_FERN)
    //$$ this.add(Blocks.ROSE_BUSH)
    //$$ this.add(Blocks.PEONY)
    //#endif
}

fun MutableList<Block>.addRedFlower() {
    //#if MC < 1.21
    this.add(Blocks.red_flower)
    //#else
    //$$ this.add(Blocks.POPPY)
    //$$ this.add(Blocks.BLUE_ORCHID)
    //$$ this.add(Blocks.ALLIUM)
    //$$ this.add(Blocks.AZURE_BLUET)
    //$$ this.add(Blocks.RED_TULIP)
    //$$ this.add(Blocks.ORANGE_TULIP)
    //$$ this.add(Blocks.WHITE_TULIP)
    //$$ this.add(Blocks.PINK_TULIP)
    //$$ this.add(Blocks.OXEYE_DAISY)
    //#endif
}

fun MutableList<Block>.addRedstoneOres() {
    this.add(Blocks.redstone_ore)
    //#if MC < 1.16
    this.add(Blocks.lit_redstone_ore)
    //#endif
}

fun MutableList<Block>.addWaters() {
    this.add(Blocks.water)
    //#if MC < 1.16
    this.add(Blocks.flowing_water)
    //#endif
}

fun MutableList<Block>.addLavas() {
    this.add(Blocks.lava)
    //#if MC < 1.16
    this.add(Blocks.flowing_lava)
    //#endif
}

enum class WoolCompat(
    private val woolColor: Int,
    //#if MC > 1.16
    //$$ private val stackType: Block,
    //$$ private val color: LorenzColor
    //#endif
) {
    WHITE(
        15,
        //#if MC > 1.16
        //$$ Blocks.WHITE_WOOL,
        //$$ LorenzColor.WHITE
        //#endif
    ),
    ORANGE(
        14,
        //#if MC > 1.16
        //$$ Blocks.ORANGE_WOOL,
        //$$ LorenzColor.GOLD
        //#endif
    ),
    MAGENTA(
        13,
        //#if MC > 1.16
        //$$ Blocks.MAGENTA_WOOL,
        //$$ LorenzColor.LIGHT_PURPLE
        //#endif
    ),
    LIGHT_BLUE(
        12,
        //#if MC > 1.16
        //$$ Blocks.LIGHT_BLUE_WOOL,
        //$$ LorenzColor.AQUA
        //#endif
    ),
    YELLOW(
        11,
        //#if MC > 1.16
        //$$ Blocks.YELLOW_WOOL,
        //$$ LorenzColor.YELLOW
        //#endif
    ),
    LIME(
        10,
        //#if MC > 1.16
        //$$ Blocks.LIME_WOOL,
        //$$ LorenzColor.GREEN
        //#endif
    ),
    PINK(
        9,
        //#if MC > 1.16
        //$$ Blocks.PINK_WOOL,
        //$$ LorenzColor.LIGHT_PURPLE
        //#endif
    ),
    GRAY(
        8,
        //#if MC > 1.16
        //$$ Blocks.GRAY_WOOL,
        //$$ LorenzColor.GRAY
        //#endif
    ),
    LIGHT_GRAY(
        7,
        //#if MC > 1.16
        //$$ Blocks.LIGHT_GRAY_WOOL,
        //$$ LorenzColor.GRAY
        //#endif
    ),
    CYAN(
        6,
        //#if MC > 1.16
        //$$ Blocks.CYAN_WOOL,
        //$$ LorenzColor.DARK_AQUA
        //#endif
    ),
    PURPLE(
        5,
        //#if MC > 1.16
        //$$ Blocks.PURPLE_WOOL,
        //$$ LorenzColor.DARK_PURPLE
        //#endif
    ),
    BLUE(
        4,
        //#if MC > 1.16
        //$$ Blocks.BLUE_WOOL,
        //$$ LorenzColor.BLUE
        //#endif
    ),
    BROWN(
        3,
        //#if MC > 1.16
        //$$ Blocks.BROWN_WOOL,
        //$$ LorenzColor.GOLD
        //#endif
    ),
    GREEN(
        2,
        //#if MC > 1.16
        //$$ Blocks.GREEN_WOOL,
        //$$ LorenzColor.DARK_GREEN
        //#endif
    ),
    RED(
        1,
        //#if MC > 1.16
        //$$ Blocks.RED_WOOL,
        //$$ LorenzColor.RED
        //#endif
    ),
    BLACK(
        0,
        //#if MC > 1.16
        //$$ Blocks.BLACK_WOOL,
        //$$ LorenzColor.BLACK
        //#endif
    )
    ;

    fun createStack(size: Int = 1): ItemStack =
        //#if MC < 1.16
        ItemStack(Blocks.wool, size, woolColor)
    //#else
    //$$ ItemStack(stackType, size)
    //#endif

    companion object {

        fun Block.isWool(wool: WoolCompat): Boolean = isWool(wool.woolColor)

        /**
         * Check if the item is wool.
         * Enter a metadata to check for a specific wool color.
         */
        fun Block.isWool(metadata: Int = -1): Boolean {
            if (metadata == -1) {
                //#if MC < 1.16
                return this == Blocks.wool
                //#else
                //$$ return this.defaultState.isIn(BlockTags.WOOL)
                //#endif
            }

            //#if MC < 1.16
            return this == Blocks.wool && this.defaultState.getValue(BlockStainedGlass.COLOR).metadata == metadata
            //#else
            //$$ return this == fromWoolColor(metadata).stackType
            //#endif
        }

        fun IBlockState.getWoolColor(): LorenzColor {
            //#if MC < 1.21
            return this.getValue(BlockStainedGlass.COLOR).toLorenzColor()
            //#else
            //$$ return WoolCompat.entries.firstOrNull { it.stackType == this.block }?.color ?: LorenzColor.GRAY
            //#endif
        }

        private fun fromWoolColor(woolColor: Int): WoolCompat = entries.firstOrNull { it.woolColor == woolColor } ?: GRAY

        fun createWoolBlock(dyeColor: Int, size: Int = 1): ItemStack =
            fromWoolColor(dyeColor).createStack(size)
    }
}
