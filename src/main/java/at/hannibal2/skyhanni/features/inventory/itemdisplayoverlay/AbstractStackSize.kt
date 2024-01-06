package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

abstract class AbstractStackSize {
    val configItemStackSize get() = SkyHanniMod.feature.inventory
    @SubscribeEvent
    open fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }
    abstract fun getStackTip(item: ItemStack): String
}

abstract class AbstractMenuStackSize : AbstractStackSize()  {
    val configMenuStackSize get() = configItemStackSize.stackSize.menu
    fun String.convertPercentToGreenCheckmark(): String {
        return this.replace("100", "§a✔")
    }
}
