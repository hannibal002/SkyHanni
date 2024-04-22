package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.features.inventory.wardrobe.Wardrobe.inWardrobe
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiInventory.drawEntityOnScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.math.ceil

class WardrobeOverlay {

    private val config get() = SkyHanniMod.feature.inventory.wardrobeOverlay
    private var display = emptyList<Pair<Position, Renderable>>()

    @SubscribeEvent
    fun onGuiRender(event: GuiContainerEvent.BeforeDraw) {
        if (!isEnabled()) return

        display = emptyList()

        val gui = event.gui
        val player = Minecraft.getMinecraft().thePlayer
        val centerX = gui.width / 2
        val centerY = gui.height / 2
        val totalPlayers = 18
        val maxPlayersPerRow = 9
        val playerWidth = 50
        val playerHeight = 2 * playerWidth
        val horizontalSpacing = 20
        val verticalSpacing = 20

        val rows = ceil(totalPlayers.toDouble() / maxPlayersPerRow).toInt()
        val totalHeight = rows * playerHeight + (rows - 1) * verticalSpacing

        val startY = centerY + playerHeight - totalHeight / 2


        GlStateManager.pushMatrix()
        GlStateManager.color(1f, 1f, 1f, 1f)


        for (row in 0 until rows) {
            val playersInRow =
                if (row != rows - 1 || totalPlayers % maxPlayersPerRow == 0) maxPlayersPerRow else totalPlayers % maxPlayersPerRow
            val totalWidth = playersInRow * playerWidth + (playersInRow - 1) * horizontalSpacing

            val startX = centerX - (totalWidth - playerWidth) / 2
            val playerY = startY + row * (playerHeight + verticalSpacing)

            for (i in 0 until playersInRow) {
                val playerX = startX + i * (playerWidth + horizontalSpacing)
                val scale = playerWidth

                // Calculate the new mouse position relative to the player
                val mouseXRelativeToPlayer = (playerX - event.mouseX).toFloat()
                val mouseYRelativeToPlayer = (playerY - event.mouseY - 1.62 * scale).toFloat()

                drawEntityOnScreen(
                    playerX,
                    playerY,
                    scale,
                    mouseXRelativeToPlayer,
                    mouseYRelativeToPlayer,
                    player
                )

                val padding = 5
                val pos = Position(playerX - padding - playerWidth / 2, playerY - playerHeight - padding)

                val renderable = Renderable.drawInsideRoundedRect(
                    Renderable.clickAndHoverable(
                        Renderable.emptyContainer(playerWidth, playerHeight),
                        Renderable.emptyContainer(playerWidth, playerHeight),
                        onClick = {
                            ChatUtils.chat("Clicked on player at $i")
                        },
                        onHover = {
                            ChatUtils.chat("Hovered over player at $i")
                        }
                    ),
                    Color.BLACK,
                    padding,
                )

                display += pos to renderable
            }
        }

        GlStateManager.popMatrix()

        event.cancel()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return

        for ((pos, renderable) in display) {
            pos.renderRenderables(listOf(renderable), posLabel = "Wardrobe Overlay")
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled && inWardrobe()

}
