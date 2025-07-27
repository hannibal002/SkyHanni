package at.hannibal2.skyhanni.features.inventory.searchablestorage

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.OtherInventoryData
import at.hannibal2.skyhanni.features.inventory.searchablestorage.SearchableStorageUtil.minecraftButtonRenderable
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.compat.SkyhanniBaseScreen
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import net.minecraft.client.renderer.GlStateManager
import java.awt.Color

class SearchableStorageReminderGui(private var search: String = "") : SkyhanniBaseScreen() {

    private val config get() = SkyHanniMod.feature.inventory

    private var display: Renderable? = null
    private var content: List<Renderable>? = null
    private var buttonSpacing = 0
    private var guiLeft = 0
    private var guiTop = 0


    override fun guiClosed() {
        OtherInventoryData.close("Searchable Storage GUI")
    }

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground(mouseX, mouseY, partialTicks)

        display = Renderable.drawInsideRoundedRect(buildContent(), Color.decode("#202020"))

        display?.let { display ->
            guiLeft = (width - display.width) / 2
            guiTop = (height - display.height) / 2

            GlStateManager.disableLighting()
            DrawContextUtils.pushPop {
                DrawContextUtils.translate(guiLeft.toFloat(), guiTop.toFloat(), 0f)
                display.render(guiLeft, guiTop)
            }
        }
    }

    private fun buildContent(): Renderable {
        val warning = string("§4WARNING")
        val warningText1 = string("§cYou currently don't save private island chests.")
        val warningText2 = string("§cIt is recommended to enable that option for maximum QOL.")
        val placeholder = Renderable.placeholder(warningText1.width)
        val dontRemind = minecraftButtonRenderable("§${if (config.searchableStorageReminder) "cD" else "2W"}on't remind again")
        val settings = minecraftButtonRenderable("§eTo Settings")
        val acknowledge = minecraftButtonRenderable("§aAcknowledge")
        val contents = listOf(
            warning,
            warningText1,
            warningText2,
            placeholder,
            dontRemind,
            placeholder,
            settings,
            placeholder,
            acknowledge,
            placeholder,
        )
        content = contents

        return Renderable.vertical(
            contents,
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
        )
    }

    private fun string(text: String): Renderable = StringRenderable(text, horizontalAlign = RenderUtils.HorizontalAlignment.CENTER)

    override fun onMouseClicked(originalMouseX: Int, originalMouseY: Int, mouseButton: Int) {
        val (relativeMouseX, relativeMouseY) = getRelativeMousePos()

        content?.let { content ->
            val display = display ?: return
            var top = 0

            content.forEachIndexed { i, renderable ->
                if (i in listOf(4, 6, 8)) {
                    if (GuiRenderUtils.isPointInRect(
                            relativeMouseX,
                            relativeMouseY,
                            left = (display.width - renderable.width) / 2,
                            top,
                            renderable.width,
                            renderable.height,
                        )
                    ) {
                        when (i) {
                            4 -> config.searchableStorageReminder = false
                            6 -> config::savePrivateIslandChests.jumpToEditor()
                            8 -> SkyHanniMod.screenToOpen = SearchableStorageGui(search)
                        }
                    }
                }
                top += renderable.height
            }
        }
    }

    private fun getRelativeMousePos(): Pair<Int, Int> {
        val (mouseX, mouseY) = GuiScreenUtils.mousePos
        return mouseX - guiLeft to mouseY - guiTop
    }
}
