package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.EntityClickEvent
import at.hannibal2.skyhanni.events.ItemClickInHandEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import org.lwjgl.input.Mouse

class ItemClickData {

    @SubscribeEvent
    fun onClick(event: InputEvent.MouseInputEvent) {
        if (!Mouse.getEventButtonState()) return

        val clickType = when (Mouse.getEventButton()) {
            0 -> ClickType.LEFT_CLICK
            1 -> ClickType.RIGHT_CLICK
            else -> return
        }

        val itemStack = Minecraft.getMinecraft().thePlayer.heldItem
        ItemClickInHandEvent(clickType, itemStack).postAndCatch()
    }

    @SubscribeEvent
    fun onEntityClick(event: InputEvent.MouseInputEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val minecraft = Minecraft.getMinecraft()
        val clickedEntity = minecraft.pointedEntity
        if (minecraft.thePlayer == null) return
        if (clickedEntity == null) return

        val clickType = when (Mouse.getEventButton()) {
            0 -> ClickType.LEFT_CLICK
            1 -> ClickType.RIGHT_CLICK
            else -> return
        }

        EntityClickEvent(clickType, clickedEntity).postAndCatch()
    }
}