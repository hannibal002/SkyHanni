package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.features.inventory.wardrobe.Wardrobe.inWardrobe
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiInventory.drawEntityOnScreen
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.ceil

class WardrobeOverlay {

    private val config get() = SkyHanniMod.feature.inventory.wardrobeOverlay

    @SubscribeEvent
    fun onGuiRender(event: GuiContainerEvent.BeforeDraw) {
        if (!isEnabled()) return
        if (!inWardrobe()) return

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
            }
        }

        event.cancel()
    }


    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled

}
