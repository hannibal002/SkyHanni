package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.config.core.config.Position
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

class CustomWardrobeEditScreen(
    handler: ChestMenu,
    inventory: Inventory,
    title: Component,
) : ContainerScreen(handler, inventory, title) {
    private val inventoryButtonPosition: Position = Position().ignoreScale()
    private var inventoryButton: Renderable? = null

    override fun init() {
        CustomWardrobe.switchingScreens = false
        super.init()
    }

    override fun removed() {
        if (!CustomWardrobe.switchingScreens) {
            super.removed()
        }
    }

    //~ if < 26.1 'extractRenderState' -> 'render' {
    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
        DrawContextUtils.setContext(graphics)
        try {
            onDrawScreen()
        } finally {
            DrawContextUtils.clearContext()
        }
    }
    //~}

    // TODO: use SkyhanniBaseScreen.onDrawScreen() instead of this method
    private fun onDrawScreen() {
        val renderable = inventoryButton ?: addReEnableButton().also { inventoryButton = it }
        val posX = this.leftPos + (1.05 * this.imageWidth).toInt()
        val posY = this.topPos + (this.imageHeight - renderable.height) / 2
        inventoryButtonPosition.moveTo(posX, posY)
            .renderRenderable(renderable, posLabel = CustomWardrobe.GUI_NAME, addToGuiManager = false)
    }

    private fun addReEnableButton(): Renderable {
        val color = Color(116, 150, 255, 200)
        return CustomWardrobe.createLabeledButton(
            "§bEdit",
            hoveredColor = color,
            unhoveredColor = color.darker(0.8),
            onClick = {
                CustomWardrobe.exitEditMode()
            },
        )
    }
}
