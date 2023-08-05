package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard

class TestShowSlotNumber {

    @SubscribeEvent
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (Keyboard.isKeyDown(SkyHanniMod.feature.dev.showSlotNumberKey)) {
            val slotIndex = event.slot.slotIndex
            event.stackTip = "$slotIndex"
        }
    }
}
