package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.guide.TabDef
import at.hannibal2.skyhanni.utils.guide.TabDef.Companion.TAB_LONG_SIDE
import at.hannibal2.skyhanni.utils.guide.TabDef.Companion.TAB_SHORT_SIDE
import at.hannibal2.skyhanni.utils.guide.TabDef.Companion.TAB_SPACING
import at.hannibal2.skyhanni.utils.guide.TabPage
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.empty
import net.minecraft.world.item.ItemStack

/**
 * Abstract base for SkyHanni screens with horizontal and/or vertical tab navigation.
 *
 * @param T The enum type used to identify pages.
 * @param defaultPage The page shown when the screen first opens.
 */
abstract class SkyHanniTabScreen<T : Enum<T>>(defaultPage: T) : SkyHanniChromeScreen() {

    abstract val pages: Map<T, TabPage>
    open val horizontalTabs: List<TabDef> = emptyList()
    open val verticalTabs: List<TabDef> = emptyList()

    protected var currentPage: T = defaultPage
        set(value) {
            pages[field]?.onLeave()
            pages[value]?.onEnter()
            field = value
            rebuildDisplay()
        }

    fun hTab(item: ItemStack, tip: Renderable, onClick: (TabDef) -> Unit) =
        TabDef(item, tip, isVertical = false, onClick = onClick)

    fun vTab(item: ItemStack, tip: Renderable, onClick: (TabDef) -> Unit) =
        TabDef(item, tip, isVertical = true, onClick = onClick)

    fun refreshPage() {
        pages[currentPage]?.refresh()
    }

    final override fun buildContent(): Renderable {
        val page = pages[currentPage] ?: return Renderable.empty()
        val pageRenderable = page.buildRenderable()
        return object : Renderable {
            override val width = pageRenderable.width
            override val height = pageRenderable.height
            override val horizontalAlign = pageRenderable.horizontalAlign
            override val verticalAlign = pageRenderable.verticalAlign

            override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
                renderHorizontalTabs(mouseOffsetX, mouseOffsetY)
                renderVerticalTabs(mouseOffsetX, mouseOffsetY)
                pageRenderable.render(mouseOffsetX, mouseOffsetY)
            }
        }
    }

    private fun renderHorizontalTabs(mouseOffsetX: Int, mouseOffsetY: Int) {
        DrawContextUtils.pushPop {
            var xOffset = TAB_SPACING * 3f
            val yOffset = -TAB_LONG_SIDE.toFloat()
            DrawContextUtils.translate(xOffset, yOffset)
            for (tab in horizontalTabs) {
                tab.render(mouseOffsetX + xOffset.toInt(), mouseOffsetY + yOffset.toInt())
                val shift = (TAB_SHORT_SIDE + TAB_SPACING).toFloat()
                xOffset += shift
                DrawContextUtils.translate(shift, 0f)
            }
        }
    }

    private fun renderVerticalTabs(mouseOffsetX: Int, mouseOffsetY: Int) {
        DrawContextUtils.pushPop {
            val xOffset = -TAB_LONG_SIDE.toFloat()
            var yOffset = TAB_SPACING * 3f
            DrawContextUtils.translate(xOffset, yOffset)
            for (tab in verticalTabs) {
                tab.render(mouseOffsetX + xOffset.toInt(), mouseOffsetY + yOffset.toInt())
                val shift = (TAB_SHORT_SIDE + TAB_SPACING).toFloat()
                yOffset += shift
                DrawContextUtils.translate(0f, shift)
            }
        }
    }
}
