package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ColorUtils.darker
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.ChestMenu
import java.awt.Color

class CustomMenuEditScreen(
    menu: ChestMenu,
    inventory: Inventory,
    title: Component,
    val gui: AbstractCustomMenu,
) : ContainerScreen(menu, inventory, title) {
    private val inventoryButtonPosition: Position = Position().ignoreScale()
    private var inventoryButton: Renderable? = null

    override fun init() {
        gui.switchingScreens = false
        super.init()
    }

    override fun removed() {
        if (!gui.switchingScreens) {
            super.removed()
        }
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
        DrawContextUtils.setContext(graphics)
        try {
            onDrawScreen()
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(
                e,
                "Error while drawing editable custom menu screen",
                "screen" to this,
                "gui" to gui.guiName,
            )
        } finally {
            DrawContextUtils.clearContext()
        }
    }

    // TODO: use SkyhanniBaseScreen.onDrawScreen() instead of this method
    private fun onDrawScreen() {
        val renderable = inventoryButton ?: addReEnableButton().also { inventoryButton = it }
        val posX = this.leftPos + (1.05 * this.imageWidth).toInt()
        val posY = this.topPos + (this.imageHeight - renderable.height) / 2
        inventoryButtonPosition.moveTo(posX, posY)
            .renderRenderable(renderable, posLabel = gui.guiName, addToGuiManager = false)
    }

    private fun addReEnableButton(): Renderable {
        val color = Color(116, 150, 255, 200)
        return gui.createLabeledButton(
            "§bEdit",
            hoveredColor = color,
            unhoveredColor = color.darker(0.8),
            onClick = {
                gui.exitEditMode()
            },
        )
    }
}
