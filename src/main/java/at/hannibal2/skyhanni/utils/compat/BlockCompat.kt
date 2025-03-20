package at.hannibal2.skyhanni.utils.compat

import net.minecraft.block.Block
import net.minecraft.block.BlockStainedGlass
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemStack

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
    //$$ private val stackType: Block
    //#endif
) {
    WHITE(
        15,
        //#if MC > 1.16
        //$$ Items.WHITE_DYE
        //#endif
    ),
    ORANGE(
        14,
        //#if MC > 1.16
        //$$ Items.ORANGE_DYE
        //#endif
    ),
    MAGENTA(
        13,
        //#if MC > 1.16
        //$$ Items.MAGENTA_DYE
        //#endif
    ),
    LIGHT_BLUE(
        12,
        //#if MC > 1.16
        //$$ Items.LIGHT_BLUE_DYE
        //#endif
    ),
    YELLOW(
        11,
        //#if MC > 1.16
        //$$ Items.YELLOW_DYE
        //#endif
    ),
    LIME(
        10,
        //#if MC > 1.16
        //$$ Items.LIME_DYE
        //#endif
    ),
    PINK(
        9,
        //#if MC > 1.16
        //$$ Items.PINK_DYE
        //#endif
    ),
    GRAY(
        8,
        //#if MC > 1.16
        //$$ Items.GRAY_DYE
        //#endif
    ),
    LIGHT_GRAY(
        7,
        //#if MC > 1.16
        //$$ Items.LIGHT_GRAY_DYE
        //#endif
    ),
    CYAN(
        6,
        //#if MC > 1.16
        //$$ Items.CYAN_DYE
        //#endif
    ),
    PURPLE(
        5,
        //#if MC > 1.16
        //$$ Items.PURPLE_DYE
        //#endif
    ),
    BLUE(
        4,
        //#if MC > 1.16
        //$$ Items.BLUE_DYE
        //#endif
    ),
    BROWN(
        3,
        //#if MC > 1.16
        //$$ Items.BROWN_DYE
        //#endif
    ),
    GREEN(
        2,
        //#if MC > 1.16
        //$$ Items.GREEN_DYE
        //#endif
    ),
    RED(
        1,
        //#if MC > 1.16
        //$$ Items.RED_DYE
        //#endif
    ),
    BLACK(
        0,
        //#if MC > 1.16
        //$$ Items.BLACK_DYE
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
         * Check if the item is a dye.
         * Enter a metadata to check for a specific dye color.
         */
        fun Block.isWool(metadata: Int = -1): Boolean {
            if (metadata == -1) {
                //#if MC < 1.16
                return this == Blocks.wool
                //#else
                //$$ return this is DyeItem
                //#endif
            }

            //#if MC < 1.16
            return this == Blocks.wool && this.defaultState.getValue(BlockStainedGlass.COLOR)
            //#else
            //$$ return this.item == fromDyeColor(metadata).stackType
            //#endif
        }

        private fun fromDyeColor(dyeColor: Int): DyeCompat = entries.firstOrNull { it.woolColor == dyeColor } ?: GRAY

        fun createDyeStack(dyeColor: Int, size: Int = 1): ItemStack =
            fromDyeColor(dyeColor).createStack(size)
    }
}
