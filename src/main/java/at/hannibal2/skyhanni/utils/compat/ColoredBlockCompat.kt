package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.SafeItemStack
import net.minecraft.world.item.DyeColor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

/**
 * Enum class that represents colored blocks in Minecraft, stained clay, wool, stained-glass, and stained-glass panes.
 * This is because on modern versions instead of all stemming from the same block but having different metadata,
 * they are all separate blocks
 *
 * This does not include uncolored blocks like glass, glass panes clay and unstained hardened clay
 *
 * This is a compatibility layer that helps with multiple minecraft versions and mixins.
 * This class should be used in utils/data/api classes and not in feature classes.
 */
enum class ColoredBlockCompat(
    private val metaColor: Int,
    private val color: LorenzColor,
    val glassBlock: Block,
    val glassPaneBlock: Block,
    val woolBlock: Block,
    val clayBlock: Block,
) {
    WHITE(
        0,
        LorenzColor.WHITE,
        Blocks.STAINED_GLASS.white(),
        Blocks.STAINED_GLASS_PANE.white(),
        Blocks.WOOL.white(),
        Blocks.DYED_TERRACOTTA.white(),
    ),
    ORANGE(
        1,
        LorenzColor.GOLD,
        Blocks.STAINED_GLASS.orange(),
        Blocks.STAINED_GLASS_PANE.orange(),
        Blocks.WOOL.orange(),
        Blocks.DYED_TERRACOTTA.orange(),
    ),
    MAGENTA(
        2,
        LorenzColor.LIGHT_PURPLE,
        Blocks.STAINED_GLASS.magenta(),
        Blocks.STAINED_GLASS_PANE.magenta(),
        Blocks.WOOL.magenta(),
        Blocks.DYED_TERRACOTTA.magenta(),
    ),
    LIGHT_BLUE(
        3,
        LorenzColor.AQUA,
        Blocks.STAINED_GLASS.lightBlue(),
        Blocks.STAINED_GLASS_PANE.lightBlue(),
        Blocks.WOOL.lightBlue(),
        Blocks.DYED_TERRACOTTA.lightBlue(),
    ),
    YELLOW(
        4,
        LorenzColor.YELLOW,
        Blocks.STAINED_GLASS.yellow(),
        Blocks.STAINED_GLASS_PANE.yellow(),
        Blocks.WOOL.yellow(),
        Blocks.DYED_TERRACOTTA.yellow(),
    ),
    LIME(
        5,
        LorenzColor.GREEN,
        Blocks.STAINED_GLASS.lime(),
        Blocks.STAINED_GLASS_PANE.lime(),
        Blocks.WOOL.lime(),
        Blocks.DYED_TERRACOTTA.lime(),
    ),
    PINK(
        6,
        LorenzColor.LIGHT_PURPLE,
        Blocks.STAINED_GLASS.pink(),
        Blocks.STAINED_GLASS_PANE.pink(),
        Blocks.WOOL.pink(),
        Blocks.DYED_TERRACOTTA.pink(),
    ),
    GRAY(
        7,
        LorenzColor.GRAY,
        Blocks.STAINED_GLASS.gray(),
        Blocks.STAINED_GLASS_PANE.gray(),
        Blocks.WOOL.gray(),
        Blocks.DYED_TERRACOTTA.gray(),
    ),
    LIGHT_GRAY(
        8,
        LorenzColor.GRAY,
        Blocks.STAINED_GLASS.lightGray(),
        Blocks.STAINED_GLASS_PANE.lightGray(),
        Blocks.WOOL.lightGray(),
        Blocks.DYED_TERRACOTTA.lightGray(),
    ),
    CYAN(
        9,
        LorenzColor.DARK_AQUA,
        Blocks.STAINED_GLASS.cyan(),
        Blocks.STAINED_GLASS_PANE.cyan(),
        Blocks.WOOL.cyan(),
        Blocks.DYED_TERRACOTTA.cyan(),
    ),
    PURPLE(
        10,
        LorenzColor.DARK_PURPLE,
        Blocks.STAINED_GLASS.purple(),
        Blocks.STAINED_GLASS_PANE.purple(),
        Blocks.WOOL.purple(),
        Blocks.DYED_TERRACOTTA.purple(),
    ),
    BLUE(
        11,
        LorenzColor.BLUE,
        Blocks.STAINED_GLASS.blue(),
        Blocks.STAINED_GLASS_PANE.blue(),
        Blocks.WOOL.blue(),
        Blocks.DYED_TERRACOTTA.blue(),
    ),
    BROWN(
        12,
        LorenzColor.GOLD,
        Blocks.STAINED_GLASS.brown(),
        Blocks.STAINED_GLASS_PANE.brown(),
        Blocks.WOOL.brown(),
        Blocks.DYED_TERRACOTTA.brown(),
    ),
    GREEN(
        13,
        LorenzColor.DARK_GREEN,
        Blocks.STAINED_GLASS.green(),
        Blocks.STAINED_GLASS_PANE.green(),
        Blocks.WOOL.green(),
        Blocks.DYED_TERRACOTTA.green(),
    ),
    RED(
        14,
        LorenzColor.RED,
        Blocks.STAINED_GLASS.red(),
        Blocks.STAINED_GLASS_PANE.red(),
        Blocks.WOOL.red(),
        Blocks.DYED_TERRACOTTA.red(),
    ),
    BLACK(
        15,
        LorenzColor.DARK_GRAY,
        Blocks.STAINED_GLASS.black(),
        Blocks.STAINED_GLASS_PANE.black(),
        Blocks.WOOL.black(),
        Blocks.DYED_TERRACOTTA.black(),
    );

    fun createGlassStack(amount: Int = 1): SafeItemStack {
        return SafeItemStack(glassBlock, amount)
    }

    fun createGlassPaneStack(amount: Int = 1): SafeItemStack {
        return SafeItemStack(glassPaneBlock, amount)
    }

    fun createWoolStack(amount: Int = 1): SafeItemStack {
        return SafeItemStack(woolBlock, amount)
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

    fun createStainedClay(amount: Int = 1): SafeItemStack {
        return SafeItemStack(clayBlock, amount)
    }

    fun getDyeColor(): DyeColor {
        for (entry in DyeColor.entries) {
            if (entry.id == this.metaColor) return entry
        }
        return DyeColor.WHITE
    }

    companion object {
        fun SafeItemStack.isStainedGlass(color: ColoredBlockCompat): Boolean = this.isStainedGlass(color.metaColor)
        fun SafeItemStack.isStainedGlassPane(color: ColoredBlockCompat): Boolean = this.isStainedGlassPane(color.metaColor)
        fun SafeItemStack.isWool(color: ColoredBlockCompat): Boolean = this.isWool(color.metaColor)
        fun SafeItemStack.isStainedClay(color: ColoredBlockCompat): Boolean = this.isStainedClay(color.metaColor)

        /**
         * No metadata means any stained-glass
         */
        fun SafeItemStack.isStainedGlass(meta: Int? = null): Boolean {
            return entries.any { (meta == null || it.metaColor == meta) && this.`is`(it.glassBlock.asItem()) }
        }

        /**
         * No metadata means any stained-glass pane
         */
        fun SafeItemStack.isStainedGlassPane(meta: Int? = null): Boolean {
            return entries.any { (meta == null || it.metaColor == meta) && this.`is`(it.glassPaneBlock.asItem()) }
        }

        /**
         * No metadata means any wool
         */
        fun SafeItemStack.isWool(meta: Int? = null): Boolean {
            return entries.any { (meta == null || it.metaColor == meta) && this.`is`(it.woolBlock.asItem()) }
        }

        /**
         * No metadata means any stained clay
         */
        fun SafeItemStack.isStainedClay(meta: Int? = null): Boolean {
            return entries.any { (meta == null || it.metaColor == meta) && this.`is`(it.clayBlock.asItem()) }
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
