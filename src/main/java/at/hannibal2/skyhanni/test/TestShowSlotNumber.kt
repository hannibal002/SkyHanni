package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class TestShowSlotNumber {

    @SubscribeEvent
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (SkyHanniMod.feature.dev.showSlotNumberKey.isKeyHeld()) {
            val slotIndex = event.slot.slotIndex
            event.stackTip = "$slotIndex"
        }
    }
}
