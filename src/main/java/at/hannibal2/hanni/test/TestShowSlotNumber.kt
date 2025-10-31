package at.hannibal2.hanni.test

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.RenderInventoryItemTipEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.KeyboardManager.isKeyHeld

@HanniModule
object TestShowSlotNumber {

    @HandleEvent
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (HanniMod.feature.dev.showSlotNumberKey.isKeyHeld()) {
            val slotIndex = event.slot.slotIndex
            event.stackTip = "$slotIndex"
        }
    }
}
