package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getItemUuid
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ShowItemUuid {

    @SubscribeEvent
    fun onItemTooltipLow(event: ItemTooltipEvent) {
        if (!SkyHanniMod.feature.dev.debug.showItemUuid) return
        val itemStack = event.itemStack
        if (itemStack != null) {
            itemStack.getItemUuid()?.let {
                event.toolTip.add("§7Item UUID: '$it'")
            }
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "dev.showItemUuid", "dev.debug.showItemUuid")
    }
}
