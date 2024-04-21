package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiInventory.drawEntityOnScreen
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class WardrobeOverlay {

    @SubscribeEvent
    fun onGuiRender(event: GuiScreenEvent.DrawScreenEvent.Post) {
        if (!LorenzUtils.inSkyBlock) return
        drawEntityOnScreen(event.gui.width / 2, event.gui.height / 2, 30, 0.0F, 0.0F, Minecraft.getMinecraft().thePlayer)
    }

}
