package at.hannibal2.skyhanni.utils.guide

import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import org.lwjgl.input.Mouse

const val selectedColor = 0x50000000
const val notSelectedColor = 0x50303030
const val tabSpacing = 5
const val tabShortSide = 25
const val tabLongSide = 28
const val tabPadding = 9f

abstract class GuideGUI<pageEnum : Enum<*>>(defaultScreen: pageEnum) : GuiScreen() {

    abstract val sizeX: Int
    abstract val sizeY: Int
    lateinit var pageList: Map<pageEnum, GuidePage>
    lateinit var horizontalTabs: List<GuideTab>
    lateinit var verticalTabs: List<GuideTab>
    protected var currentPage: pageEnum = defaultScreen
        set(value) {
            pageList[value]?.onSwitch()
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

    private fun renderHorizontalTabs(mouseX: Int, mouseY: Int) {
        var offset = Pair(tabSpacing.toFloat() * 3f, -tabLongSide.toFloat())
        GlStateManager.translate(offset.first, offset.second, 0f)
        for (tab in horizontalTabs) {
            Renderable.withMousePosition(mouseX, mouseY) {
                tab.render(offset.first.toInt(), offset.second.toInt())
            }
            val xShift = (tabShortSide + tabSpacing).toFloat()
            offset = offset.first + xShift to offset.second
            GlStateManager.translate(xShift, 0f, 0f)
        }
        GlStateManager.translate(-offset.first, -offset.second, 0f)
    }

    private fun renderVerticalTabs(mouseX: Int, mouseY: Int) {
        var offset = Pair(-tabLongSide.toFloat(), tabSpacing.toFloat() * 3f)
        GlStateManager.translate(offset.first, offset.second, 0f)
        for (tab in verticalTabs) {
            Renderable.withMousePosition(mouseX, mouseY) {
                tab.render(offset.first.toInt(), offset.second.toInt())
            }
            val yShift = (tabShortSide + tabSpacing).toFloat()
            offset = offset.first to offset.second + yShift
            GlStateManager.translate(0f, yShift, 0f)
        }
        GlStateManager.translate(-offset.first, -offset.second, 0f)
    }

    override fun drawScreen(unusedX: Int, unusedY: Int, partialTicks: Float) {
        try {
            super.drawScreen(unusedX, unusedY, partialTicks)
            drawDefaultBackground()
            val guiLeft = (width - sizeX) / 2
            val guiTop = (height - sizeY) / 2

            val mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth
            val mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1

            val relativeMouseX = mouseX - guiLeft
            val relativeMouseY = mouseY - guiTop

            GlStateManager.pushMatrix()
            GlStateManager.translate(guiLeft.toFloat(), guiTop.toFloat(), 0f)
            drawRect(0, 0, sizeX, sizeY, 0x50000000)

            renderHorizontalTabs(relativeMouseX, relativeMouseY)
            renderVerticalTabs(relativeMouseX, relativeMouseY)

            val page = pageList[currentPage]
            if (page !is FFGuideGUI.FFGuidePage) { // TODO remove
                page?.drawPage(relativeMouseX, relativeMouseY)
            }
            GlStateManager.translate(-guiLeft.toFloat(), -guiTop.toFloat(), 0f)
            if (page is FFGuideGUI.FFGuidePage) {
                page.drawPage(mouseX, mouseY)
            }
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(
                e, "Something broke in GuideGUI",
            )
        }
    }
}
