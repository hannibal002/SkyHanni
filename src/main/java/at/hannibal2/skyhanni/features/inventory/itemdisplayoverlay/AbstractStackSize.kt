package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.inventory.InventoryConfig
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeMenuConfig
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.repopatterns.RepoPatternGroup
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

abstract class AbstractStackSize {
    val configItemStackSize: InventoryConfig get() = SkyHanniMod.feature.inventory
    @SubscribeEvent
    open fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }
    abstract fun getStackTip(item: ItemStack): String
    val itemStackSizeGroup: RepoPatternGroup = RepoPattern.group("item.stack.size")
    val greenCheckmark: String = "§a✔"
    val bigRedCross: String = "§c§l✖"
}

abstract class AbstractMenuStackSize : AbstractStackSize()  {
    val configMenuStackSize: StackSizeMenuConfig get() = configItemStackSize.stackSize.menu
    fun String.convertPercentToGreenCheckmark(): String {
        return this.replace("100", super.greenCheckmark)
    }
}
