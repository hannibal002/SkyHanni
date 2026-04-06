package at.hannibal2.skyhanni.utils.guide

import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import net.minecraft.world.item.ItemStack

/**
 * Defines a single tab in a [SkyHanniTabScreen][at.hannibal2.skyhanni.utils.compat.SkyHanniTabScreen].
 *
 * @param item The item stack rendered as the tab icon.
 * @param tip The tooltip Renderable shown on hover.
 * @param isVertical True for left-side vertical tabs, false for top horizontal tabs.
 * @param onClick Callback invoked when the tab is clicked; receives this TabDef.
 */
class TabDef(
    val item: ItemStack,
    val tip: Renderable,
    val isVertical: Boolean = false,
    val onClick: (TabDef) -> Unit,
) {
    companion object {
        const val SELECTED_COLOR = 0x50000000
        const val NOT_SELECTED_COLOR = 0x50303030
        const val TAB_SHORT_SIDE = 25
        const val TAB_LONG_SIDE = 28
        const val TAB_SPACING = 5
    }

    val width = if (isVertical) TAB_LONG_SIDE else TAB_SHORT_SIDE
    val height = if (isVertical) TAB_SHORT_SIDE else TAB_LONG_SIDE

    private var selectColor = NOT_SELECTED_COLOR

    fun select() {
        selectColor = SELECTED_COLOR
    }
    fun unSelect() {
        selectColor = NOT_SELECTED_COLOR
    }
    fun isSelected() = selectColor == SELECTED_COLOR

    private val itemRenderable by lazy {
        Renderable.item(item) {
            scale = 1.0
            horizontalAlign = HorizontalAlignment.CENTER
            verticalAlign = VerticalAlignment.CENTER
        }
    }

    private val renderable = Renderable.clickable(
        object : Renderable {
            override val width = this@TabDef.width
            override val height = this@TabDef.height
            override val horizontalAlign = HorizontalAlignment.LEFT
            override val verticalAlign = VerticalAlignment.TOP

            override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
                GuiRenderUtils.drawRect(0, 0, width, height, selectColor)
                itemRenderable.renderXYAligned(mouseOffsetX, mouseOffsetY, width, height)
            }
        },
        tips = listOf(tip),
        onLeftClick = {
            onClick(this@TabDef)
            SoundUtils.playClickSound()
        },
    )

    fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
        renderable.render(mouseOffsetX, mouseOffsetY)
    }
}
