package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiInventory.drawEntityOnScreen
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class WardrobeOverlay {

    private val config get() = SkyHanniMod.feature.inventory.wardrobeOverlay
    private var inWardrobe = false

    @SubscribeEvent
    fun onGuiRender(event: GuiContainerEvent.BeforeDraw) {
        if (!isEnabled()) return
        if (!InventoryUtils.openInventoryName().startsWith("Wardrobe")) return
        inWardrobe = true

        val y = event.gui.height / 2

        drawEntityOnScreen(
            event.gui.width / 2,
            y,
            30,
            (event.gui.width / 2 - event.mouseX).toFloat(),
            (y - event.mouseY).toFloat(),
            Minecraft.getMinecraft().thePlayer
        )

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
