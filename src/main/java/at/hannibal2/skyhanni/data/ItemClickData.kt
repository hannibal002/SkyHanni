package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.ItemClickInHandEvent
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import org.lwjgl.input.Mouse

class ItemClickData {

    @SubscribeEvent
    fun onClick(event: InputEvent.MouseInputEvent) {
        if (!Mouse.getEventButtonState()) return

        val clickType = when (Mouse.getEventButton()) {
            0 -> ItemClickInHandEvent.ClickType.LEFT_CLICK
            1 -> ItemClickInHandEvent.ClickType.RIGHT_CLICK
            else -> return
        }

        val itemStack = Minecraft.getMinecraft().thePlayer.heldItem
        ItemClickInHandEvent(clickType, itemStack).postAndCatch()
    }
}