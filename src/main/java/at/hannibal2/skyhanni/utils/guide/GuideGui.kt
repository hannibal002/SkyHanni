package at.hannibal2.skyhanni.utils.guide

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.SkyhanniBaseScreen
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import net.minecraft.item.ItemStack

abstract class GuideGui<pageEnum : Enum<*>>(defaultScreen: pageEnum) : SkyhanniBaseScreen() {
    companion object {
        const val SELECTED_COLOR = 0x50000000
        const val NOT_SELECTED_COLOR = 0x50303030
        const val TAB_SPACING = 5
        const val TAB_SHORT_SIDE = 25
        const val TAB_LONG_SIDE = 28
    }

    abstract val sizeX: Int
    abstract val sizeY: Int
    lateinit var pageList: Map<pageEnum, GuidePage>
    lateinit var horizontalTabs: List<GuideTab>
    lateinit var verticalTabs: List<GuideTab>
    protected var currentPage: pageEnum = defaultScreen
        set(value) {
            pageList[field]?.onLeave()
            pageList[value]?.onEnter()
            field = value
        }

    val lastVerticalTabWrapper = object : tabWrapper {
        override var tab: GuideTab? = null
    }
    val lastHorizontalTabWrapper = object : tabWrapper {
        override var tab: GuideTab? = null
    }

    fun hTab(item: ItemStack, tip: Renderable, onClick: (GuideTab) -> Unit) =
        GuideTab(item, tip, false, lastHorizontalTabWrapper, onClick)

    fun vTab(item: ItemStack, tip: Renderable, onClick: (GuideTab) -> Unit) =
        GuideTab(item, tip, true, lastVerticalTabWrapper, onClick)

    interface tabWrapper {
        var tab: GuideTab?
    }

    fun refreshPage() {
        pageList[currentPage]?.refresh()
    }

    private fun renderHorizontalTabs() {
        var offset = Pair(TAB_SPACING.toFloat() * 3f, -TAB_LONG_SIDE.toFloat())
        DrawContextUtils.translate(offset.first, offset.second, 0f)
        for (tab in horizontalTabs) {
            tab.render(offset.first.toInt(), offset.second.toInt())
            val xShift = (TAB_SHORT_SIDE + TAB_SPACING).toFloat()
            offset = offset.first + xShift to offset.second
            DrawContextUtils.translate(xShift, 0f, 0f)
        }
        DrawContextUtils.translate(-offset.first, -offset.second, 0f)
    }

    private fun renderVerticalTabs() {
        var offset = Pair(-TAB_LONG_SIDE.toFloat(), TAB_SPACING.toFloat() * 3f)
        DrawContextUtils.translate(offset.first, offset.second, 0f)
        for (tab in verticalTabs) {
            tab.render(offset.first.toInt(), offset.second.toInt())
            val yShift = (TAB_SHORT_SIDE + TAB_SPACING).toFloat()
            offset = offset.first to offset.second + yShift
            DrawContextUtils.translate(0f, yShift, 0f)
        }
        DrawContextUtils.translate(-offset.first, -offset.second, 0f)
    }

    override fun onDrawScreen(originalMouseX: Int, originalMouseY: Int, partialTicks: Float) = try {
        drawDefaultBackground(originalMouseX, originalMouseY, partialTicks)
        val guiLeft = (width - sizeX) / 2
        val guiTop = (height - sizeY) / 2

        val relativeMouseX = originalMouseX - guiLeft
        val relativeMouseY = originalMouseY - guiTop

        DrawContextUtils.pushMatrix()
        DrawContextUtils.translate(guiLeft.toFloat(), guiTop.toFloat(), 0f)
        GuiRenderUtils.drawRect(0, 0, sizeX, sizeY, 0x50000000)

        Renderable.withMousePosition(relativeMouseX, relativeMouseY) {
            renderHorizontalTabs()
            renderVerticalTabs()

            Renderable.string(
                "§7SkyHanni ",
                horizontalAlign = RenderUtils.HorizontalAlignment.RIGHT,
                verticalAlign = RenderUtils.VerticalAlignment.BOTTOM,
            ).renderXYAligned(0, 0, sizeX, sizeY)

            val page = pageList[currentPage]
            page?.drawPage(relativeMouseX, relativeMouseY)

            DrawContextUtils.translate(-guiLeft.toFloat(), -guiTop.toFloat(), 0f)
        }
        DrawContextUtils.popMatrix()
    } catch (e: Exception) {
        DrawContextUtils.popMatrix()
        ErrorManager.logErrorWithData(
            e, "Something broke in GuideGUI",
            "Guide" to this.javaClass.typeName,
            "Page" to currentPage.name,
        )
        Unit
    }
}
