package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.ItemClass
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ItemClassErrorMaker {
    @SubscribeEvent
    fun onRenderItem(event: RenderItemTipEvent) {
        event.stackTip = ItemClass.readItemClass(event.stack)?.name?.take(3) ?: ""
    }
}
