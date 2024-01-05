package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

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
    
    fun Pattern.returnPercentFromLoreLineAsStackSize(line: String): String {
        this.matchMatcher(line) {
            return group("percent").replace("100", "§a✔")
        }
        return ""
    }
}
