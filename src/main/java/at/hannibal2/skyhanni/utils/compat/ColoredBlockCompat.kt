package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzColor.Companion.toLorenzColor
import net.minecraft.block.Block
import net.minecraft.block.BlockStainedGlass
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack

/**
 * Enum class that represents colored blocks in Minecraft, wool, stained-glass, and stained-glass panes.
 * This is because on modern versions instead of all stemming from the same block but having different metadata,
 * they are all separate blocks
 *
 * This does not include uncolored blocks like glass and glass panes
 */
enum class ColoredBlockCompat(
    private val metaColor: Int,
    //#if MC > 1.16
    //$$ private val color: LorenzColor,
    //$$ private val glassBlock: Block,
    //$$ private val glassPaneBlock: Block,
    //$$ private val woolBlock: Block,
    //#endif
) {
    WHITE(
        0,
        //#if MC > 1.16
        //$$ LorenzColor.WHITE,
        //$$ Blocks.WHITE_STAINED_GLASS,
        //$$ Blocks.WHITE_STAINED_GLASS_PANE,
        //$$ Blocks.WHITE_WOOL,
        //#endif
    ),
    ORANGE(
        1,
        //#if MC > 1.16
        //$$ LorenzColor.GOLD,
        //$$ Blocks.ORANGE_STAINED_GLASS,
        //$$ Blocks.ORANGE_STAINED_GLASS_PANE,
        //$$ Blocks.ORANGE_WOOL,
        //#endif
    ),
    MAGENTA(
        2,
        //#if MC > 1.16
        //$$ LorenzColor.LIGHT_PURPLE,
        //$$ Blocks.MAGENTA_STAINED_GLASS,
        //$$ Blocks.MAGENTA_STAINED_GLASS_PANE,
        //$$ Blocks.MAGENTA_WOOL,
        //#endif
    ),
    LIGHT_BLUE(
        3,
        //#if MC > 1.16
        //$$ LorenzColor.AQUA,
        //$$ Blocks.LIGHT_BLUE_STAINED_GLASS,
        //$$ Blocks.LIGHT_BLUE_STAINED_GLASS_PANE,
        //$$ Blocks.LIGHT_BLUE_WOOL,
        //#endif
    ),
    YELLOW(
        4,
        //#if MC > 1.16
        //$$ LorenzColor.YELLOW,
        //$$ Blocks.YELLOW_STAINED_GLASS,
        //$$ Blocks.YELLOW_STAINED_GLASS_PANE,
        //$$ Blocks.YELLOW_WOOL,
        //#endif

    ),
    LIME(
        5,
        //#if MC > 1.16
        //$$ LorenzColor.GREEN,
        //$$ Blocks.LIME_STAINED_GLASS,
        //$$ Blocks.LIME_STAINED_GLASS_PANE,
        //$$ Blocks.LIME_WOOL,
        //#endif
    ),
    PINK(
        6,
        //#if MC > 1.16
        //$$ LorenzColor.LIGHT_PURPLE,
        //$$ Blocks.PINK_STAINED_GLASS,
        //$$ Blocks.PINK_STAINED_GLASS_PANE,
        //$$ Blocks.PINK_WOOL,
        //#endif
    ),
    GRAY(
        7,
        //#if MC > 1.16
        //$$ LorenzColor.GRAY,
        //$$ Blocks.GRAY_STAINED_GLASS,
        //$$ Blocks.GRAY_STAINED_GLASS_PANE,
        //$$ Blocks.GRAY_WOOL,
        //#endif
    ),
    LIGHT_GRAY(
        8,
        //#if MC > 1.16
        //$$ LorenzColor.GRAY,
        //$$ Blocks.LIGHT_GRAY_STAINED_GLASS,
        //$$ Blocks.LIGHT_GRAY_STAINED_GLASS_PANE,
        //$$ Blocks.LIGHT_GRAY_WOOL,
        //#endif
    ),
    CYAN(
        9,
        //#if MC > 1.16
        //$$ LorenzColor.DARK_AQUA,
        //$$ Blocks.CYAN_STAINED_GLASS,
        //$$ Blocks.CYAN_STAINED_GLASS_PANE,
        //$$ Blocks.CYAN_WOOL,
        //#endif
    ),
    PURPLE(
        10,
        //#if MC > 1.16
        //$$ LorenzColor.DARK_PURPLE,
        //$$ Blocks.PURPLE_STAINED_GLASS,
        //$$ Blocks.PURPLE_STAINED_GLASS_PANE,
        //$$ Blocks.PURPLE_WOOL,
        //#endif
    ),
    BLUE(
        11,
        //#if MC > 1.16
        //$$ LorenzColor.BLUE,
        //$$ Blocks.BLUE_STAINED_GLASS,
        //$$ Blocks.BLUE_STAINED_GLASS_PANE,
        //$$ Blocks.BLUE_WOOL,
        //#endif
    ),
    BROWN(
        12,
        //#if MC > 1.16
        //$$ LorenzColor.GOLD,
        //$$ Blocks.BROWN_STAINED_GLASS,
        //$$ Blocks.BROWN_STAINED_GLASS_PANE,
        //$$ Blocks.BROWN_WOOL,
        //#endif
    ),
    GREEN(
        13,
        //#if MC > 1.16
        //$$ LorenzColor.DARK_GREEN,
        //$$ Blocks.GREEN_STAINED_GLASS,
        //$$ Blocks.GREEN_STAINED_GLASS_PANE,
        //$$ Blocks.GREEN_WOOL,
        //#endif
    ),
    RED(
        14,
        //#if MC > 1.16
        //$$ LorenzColor.RED,
        //$$ Blocks.RED_STAINED_GLASS,
        //$$ Blocks.RED_STAINED_GLASS_PANE,
        //$$ Blocks.RED_WOOL,
        //#endif
    ),
    BLACK(
        15,
        //#if MC > 1.16
        //$$ LorenzColor.DARK_GRAY,
        //$$ Blocks.BLACK_STAINED_GLASS,
        //$$ Blocks.BLACK_STAINED_GLASS_PANE,
        //$$ Blocks.BLACK_WOOL,
        //#endif
    );

    fun createGlassStack(amount: Int = 1): ItemStack {
        //#if MC < 1.16
        return ItemStack(Blocks.stained_glass, amount, metaColor)
        //#else
        //$$ return ItemStack(glassBlock, amount)
        //#endif
    }

    fun createGlassPaneStack(amount: Int = 1): ItemStack {
        //#if MC < 1.16
        return ItemStack(Blocks.stained_glass_pane, amount, metaColor)
        //#else
        //$$ return ItemStack(glassPaneBlock, amount)
        //#endif
    }

    fun createWoolStack(amount: Int = 1): ItemStack {
        //#if MC < 1.16
        return ItemStack(Blocks.wool, amount, metaColor)
        //#else
        //$$ return ItemStack(woolBlock, amount)
        //#endif
    }

    companion object {
        fun ItemStack.isStainedGlass(color: ColoredBlockCompat): Boolean = this.isStainedGlass(color.metaColor)
        fun ItemStack.isStainedGlassPane(color: ColoredBlockCompat): Boolean = this.isStainedGlassPane(color.metaColor)
        fun ItemStack.isWool(color: ColoredBlockCompat): Boolean = this.isWool(color.metaColor)

        /**
         * No metadata means any stained-glass
         */
        fun ItemStack.isStainedGlass(meta: Int? = null): Boolean {
            //#if MC < 1.16
            if (this.item != ItemStack(Blocks.stained_glass).item) return false
            meta ?: return true
            return this.metadata == meta
            //#else
            //$$ return entries.any { (meta == null || it.metaColor == meta) && this.item == it.glassBlock.asItem() }
            //#endif
        }

        /**
         * No metadata means any stained-glass pane
         */
        fun ItemStack.isStainedGlassPane(meta: Int? = null): Boolean {
            //#if MC < 1.16
            if (this.item != ItemStack(Blocks.stained_glass_pane).item) return false
            meta ?: return true
            return this.metadata == meta
            //#else
            //$$ return entries.any { (meta == null || it.metaColor == meta) && this.item == it.glassPaneBlock.asItem() }
            //#endif
        }

        /**
         * No metadata means any wool
         */
        fun ItemStack.isWool(meta: Int? = null): Boolean {
            //#if MC < 1.16
            if (this.item != ItemStack(Blocks.wool).item) return false
            meta ?: return true
            return this.metadata == meta
            //#else
            //$$ return entries.any { (meta == null || it.metaColor == meta) && this.item == it.woolBlock.asItem() }
            //#endif
        }

        fun Block.isStainedGlass(color: ColoredBlockCompat): Boolean = isStainedGlass(color.metaColor)
        fun Block.isStainedGlassPane(color: ColoredBlockCompat): Boolean = isStainedGlassPane(color.metaColor)
        fun Block.isWool(color: ColoredBlockCompat): Boolean = isWool(color.metaColor)

        /**
         * No metadata means any stained-glass
         */
        fun Block.isStainedGlass(meta: Int? = null): Boolean {
            //#if MC < 1.16
            if (this != Blocks.stained_glass) return false
            meta ?: return true
            return defaultState.getValue(BlockStainedGlass.COLOR).metadata == meta
            //#else
            //$$ return entries.any { (meta == null || it.metaColor == meta) && this == it.glassBlock }
            //#endif
        }

        /**
         * No metadata means any stained-glass pane
         */
        fun Block.isStainedGlassPane(meta: Int? = null): Boolean {
            //#if MC < 1.16
            if (this != Blocks.stained_glass_pane) return false
            meta ?: return true
            return defaultState.getValue(BlockStainedGlass.COLOR).metadata == meta
            //#else
            //$$ return entries.any { (meta == null || it.metaColor == meta) && this == it.glassPaneBlock }
            //#endif
        }

        /**
         * No metadata means any wool
         */
        fun Block.isWool(meta: Int? = null): Boolean {
            //#if MC < 1.16
            if (this != Blocks.wool) return false
            meta ?: return true
            return defaultState.getValue(BlockStainedGlass.COLOR).metadata == meta
            //#else
            //$$ return entries.any { (meta == null || it.metaColor == meta) && this == it.woolBlock }
            //#endif
        }

        fun IBlockState.getBlockColor(): LorenzColor {
            //#if MC < 1.16
            return this.getValue(BlockStainedGlass.COLOR).toLorenzColor()
            //#else
            //$$ return ColoredBlockCompat.entries.firstOrNull { block ->
            //$$     block.glassBlock == this.block || block.glassPaneBlock == this.block || block.woolBlock == this.block
            //$$ }?.color ?: LorenzColor.WHITE
            //#endif
        }
    }
}
