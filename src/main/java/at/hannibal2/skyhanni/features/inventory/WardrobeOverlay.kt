package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiInventory.drawEntityOnScreen
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class WardrobeOverlay {

    private val config get() = SkyHanniMod.feature.inventory.wardrobeOverlay
    private var inWardrobe = false

    @SubscribeEvent
    fun onGuiRender(event: GuiContainerEvent.BeforeDraw) {
        if (!isEnabled()) return
        if (!InventoryUtils.openInventoryName().startsWith("Wardrobe")) return
        inWardrobe = true

        val gui = event.gui
        val player = Minecraft.getMinecraft().thePlayer
        val centerX = gui.width / 2
        val centerY = gui.height / 2
        val totalPlayers = 9
        val playerWidth = 50
        val spacing = 20

        // Calculate the total width occupied by players and spacing
        val totalWidth = totalPlayers * playerWidth + (totalPlayers - 1) * spacing

        // Calculate the starting X position to center the players
        val startX = centerX - (totalWidth - playerWidth) / 2

        // Draw each player
        for (i in 0 until totalPlayers) {
            val playerX = startX + i * (playerWidth + spacing)

            drawEntityOnScreen(
                playerX,
                centerY,
                playerWidth,
                (centerX - event.mouseX).toFloat(),
                (centerY - event.mouseY).toFloat(),
                player
            )
        }

        event.cancel()
    }

    @SubscribeEvent
    fun onGuiClose(event: InventoryCloseEvent) {
        if (!inWardrobe) return
        inWardrobe = false
    }

    @SubscribeEvent
    fun onClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!inWardrobe) return
        event.cancel()
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled

}
