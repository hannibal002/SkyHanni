package at.hannibal2.hanni.features.pets

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.GuiRenderItemEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RenderUtils.drawSlotText
import at.hannibal2.hanni.utils.SkyBlockItemModifierUtils.getHeldPetItem
import net.minecraft.client.Minecraft

@HanniModule
object PetItemDisplay {

    private val config get() = HanniMod.feature.misc.pets

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderItemOverlayPost(event: GuiRenderItemEvent.RenderOverlayEvent.GuiRenderItemPost) {
        val stack = event.stack?.takeIf { it.stackSize == 1 } ?: return
        if (config.petItemDisplay.isEmpty()) return

        val petItem = stack.getHeldPetItem() ?: return
        val icon = config.petItemDisplay.firstOrNull { it.item == petItem.asString() }?.icon ?: return

        val width = (Minecraft.getMinecraft().fontRendererObj.getStringWidth(icon) * config.petItemDisplayScale).toInt()
        val x = event.x + 22 - width
        val y = event.y - 1

        event.drawSlotText(x, y, icon, config.petItemDisplayScale)
    }
}
