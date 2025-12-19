package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.LorenzColor
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

/**
 * Enum class that represents colored blocks in Minecraft, stained clay, wool, stained-glass, and stained-glass panes.
 * This is because on modern versions instead of all stemming from the same block but having different metadata,
 * they are all separate blocks
 *
 * This does not include uncolored blocks like glass, glass panes clay and unstained hardened clay
 */
enum class ColoredBlockCompat(
    private val metaColor: Int,
    private val color: LorenzColor,
    private val glassBlock: Block,
    private val glassPaneBlock: Block,
    private val woolBlock: Block,
    private val clayBlock: Block,
) {
    WHITE(
        0,
        LorenzColor.WHITE,
        Blocks.WHITE_STAINED_GLASS,
        Blocks.WHITE_STAINED_GLASS_PANE,
        Blocks.WHITE_WOOL,
        Blocks.WHITE_TERRACOTTA,
    ),
    ORANGE(
        1,
        LorenzColor.GOLD,
        Blocks.ORANGE_STAINED_GLASS,
        Blocks.ORANGE_STAINED_GLASS_PANE,
        Blocks.ORANGE_WOOL,
        Blocks.ORANGE_TERRACOTTA,
    ),
    MAGENTA(
        2,
        LorenzColor.LIGHT_PURPLE,
        Blocks.MAGENTA_STAINED_GLASS,
        Blocks.MAGENTA_STAINED_GLASS_PANE,
        Blocks.MAGENTA_WOOL,
        Blocks.MAGENTA_TERRACOTTA,
    ),
    LIGHT_BLUE(
        3,
        LorenzColor.AQUA,
        Blocks.LIGHT_BLUE_STAINED_GLASS,
        Blocks.LIGHT_BLUE_STAINED_GLASS_PANE,
        Blocks.LIGHT_BLUE_WOOL,
        Blocks.LIGHT_BLUE_TERRACOTTA,
    ),
    YELLOW(
        4,
        LorenzColor.YELLOW,
        Blocks.YELLOW_STAINED_GLASS,
        Blocks.YELLOW_STAINED_GLASS_PANE,
        Blocks.YELLOW_WOOL,
        Blocks.YELLOW_TERRACOTTA,

        ),
    LIME(
        5,
        LorenzColor.GREEN,
        Blocks.LIME_STAINED_GLASS,
        Blocks.LIME_STAINED_GLASS_PANE,
        Blocks.LIME_WOOL,
        Blocks.LIME_TERRACOTTA,
    ),
    PINK(
        6,
        LorenzColor.LIGHT_PURPLE,
        Blocks.PINK_STAINED_GLASS,
        Blocks.PINK_STAINED_GLASS_PANE,
        Blocks.PINK_WOOL,
        Blocks.PINK_TERRACOTTA,
    ),
    GRAY(
        7,
        LorenzColor.GRAY,
        Blocks.GRAY_STAINED_GLASS,
        Blocks.GRAY_STAINED_GLASS_PANE,
        Blocks.GRAY_WOOL,
        Blocks.GRAY_TERRACOTTA,
    ),
    LIGHT_GRAY(
        8,
        LorenzColor.GRAY,
        Blocks.LIGHT_GRAY_STAINED_GLASS,
        Blocks.LIGHT_GRAY_STAINED_GLASS_PANE,
        Blocks.LIGHT_GRAY_WOOL,
        Blocks.LIGHT_GRAY_TERRACOTTA,
    ),
    CYAN(
        9,
        LorenzColor.DARK_AQUA,
        Blocks.CYAN_STAINED_GLASS,
        Blocks.CYAN_STAINED_GLASS_PANE,
        Blocks.CYAN_WOOL,
        Blocks.CYAN_TERRACOTTA,
    ),
    PURPLE(
        10,
        LorenzColor.DARK_PURPLE,
        Blocks.PURPLE_STAINED_GLASS,
        Blocks.PURPLE_STAINED_GLASS_PANE,
        Blocks.PURPLE_WOOL,
        Blocks.PURPLE_TERRACOTTA,
    ),
    BLUE(
        11,
        LorenzColor.BLUE,
        Blocks.BLUE_STAINED_GLASS,
        Blocks.BLUE_STAINED_GLASS_PANE,
        Blocks.BLUE_WOOL,
        Blocks.BLUE_TERRACOTTA,
    ),
    BROWN(
        12,
        LorenzColor.GOLD,
        Blocks.BROWN_STAINED_GLASS,
        Blocks.BROWN_STAINED_GLASS_PANE,
        Blocks.BROWN_WOOL,
        Blocks.BROWN_TERRACOTTA,
    ),
    GREEN(
        13,
        LorenzColor.DARK_GREEN,
        Blocks.GREEN_STAINED_GLASS,
        Blocks.GREEN_STAINED_GLASS_PANE,
        Blocks.GREEN_WOOL,
        Blocks.GREEN_TERRACOTTA,
    ),
    RED(
        14,
        LorenzColor.RED,
        Blocks.RED_STAINED_GLASS,
        Blocks.RED_STAINED_GLASS_PANE,
        Blocks.RED_WOOL,
        Blocks.RED_TERRACOTTA,
    ),
    BLACK(
        15,
        LorenzColor.DARK_GRAY,
        Blocks.BLACK_STAINED_GLASS,
        Blocks.BLACK_STAINED_GLASS_PANE,
        Blocks.BLACK_WOOL,
        Blocks.BLACK_TERRACOTTA,
    );

    fun createGlassStack(amount: Int = 1): ItemStack {
        return ItemStack(glassBlock, amount)
    }

    fun createGlassPaneStack(amount: Int = 1): ItemStack {
        return ItemStack(glassPaneBlock, amount)
    }

    fun createWoolStack(amount: Int = 1): ItemStack {
        return ItemStack(woolBlock, amount)
    }

    fun createWoolBlockState(): BlockState {
        return this.woolBlock.defaultBlockState()
    }

    fun createGlassBlockState(state: BlockState? = null): BlockState {
        if (state == null) return this.glassBlock.defaultBlockState()
        if (state.isStainedGlassPane()) {
            return this.glassPaneBlock.withPropertiesOf(state)
        }
        return this.glassBlock.withPropertiesOf(state)
    }

    fun createStainedClay(amount: Int = 1): ItemStack {
        return ItemStack(clayBlock, amount)
    }

    fun getDyeColor(): DyeColor {
        for (entry in DyeColor.entries) {
            if (entry.id == this.metaColor) return entry
        }
        return DyeColor.WHITE
    }

    companion object {
        fun ItemStack.isStainedGlass(color: ColoredBlockCompat): Boolean = this.isStainedGlass(color.metaColor)
        fun ItemStack.isStainedGlassPane(color: ColoredBlockCompat): Boolean = this.isStainedGlassPane(color.metaColor)
        fun ItemStack.isWool(color: ColoredBlockCompat): Boolean = this.isWool(color.metaColor)
        fun ItemStack.isStainedClay(color: ColoredBlockCompat): Boolean = this.isStainedClay(color.metaColor)

        /**
         * No metadata means any stained-glass
         */
        fun ItemStack.isStainedGlass(meta: Int? = null): Boolean {
            return entries.any { (meta == null || it.metaColor == meta) && this.item == it.glassBlock.asItem() }
        }

        /**
         * No metadata means any stained-glass pane
         */
        fun ItemStack.isStainedGlassPane(meta: Int? = null): Boolean {
            return entries.any { (meta == null || it.metaColor == meta) && this.item == it.glassPaneBlock.asItem() }
        }

        /**
         * No metadata means any wool
         */
        fun ItemStack.isWool(meta: Int? = null): Boolean {
            return entries.any { (meta == null || it.metaColor == meta) && this.item == it.woolBlock.asItem() }
        }

        /**
         * No metadata means any stained clay
         */
        fun ItemStack.isStainedClay(meta: Int? = null): Boolean {
            return entries.any { (meta == null || it.metaColor == meta) && this.item == it.clayBlock.asItem() }
        }

        fun BlockState.isStainedGlass(color: ColoredBlockCompat): Boolean = isStainedGlass(color.metaColor)
        fun BlockState.isStainedGlassPane(color: ColoredBlockCompat): Boolean = isStainedGlassPane(color.metaColor)
        fun BlockState.isWool(color: ColoredBlockCompat): Boolean = isWool(color.metaColor)
        fun BlockState.isStainedClay(color: ColoredBlockCompat): Boolean = isStainedClay(color.metaColor)

        /**
         * No metadata means any stained-glass
         */
        fun BlockState.isStainedGlass(meta: Int? = null): Boolean {
            return ColoredBlockCompat.entries.any { (meta == null || it.metaColor == meta) && this.block == it.glassBlock }
        }

        /**
         * No metadata means any stained-glass pane
         */
        fun BlockState.isStainedGlassPane(meta: Int? = null): Boolean {
            return ColoredBlockCompat.entries.any { (meta == null || it.metaColor == meta) && this.block == it.glassPaneBlock }
        }

        /**
         * No metadata means any wool
         */
        fun BlockState.isWool(meta: Int? = null): Boolean {
            return ColoredBlockCompat.entries.any { (meta == null || it.metaColor == meta) && this.block == it.woolBlock }
        }

        /**
         * No metadata means any stained clay
         */
        fun BlockState.isStainedClay(meta: Int? = null): Boolean {
            return ColoredBlockCompat.entries.any { (meta == null || it.metaColor == meta) && this.block == it.clayBlock }
        }

        fun BlockState.getBlockColor(): LorenzColor {
            return ColoredBlockCompat.entries.firstOrNull { block ->
                block.glassBlock == this.block || block.glassPaneBlock == this.block || block.woolBlock == this.block || block.clayBlock == this.block
            }?.color ?: LorenzColor.WHITE
        }

        fun fromMeta(meta: Int): ColoredBlockCompat {
            for (entry in entries) {
                if (entry.metaColor == meta) return entry
            }
            return WHITE
        }
    }
}
