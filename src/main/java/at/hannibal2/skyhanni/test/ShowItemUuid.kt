package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.events.minecraft.add
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getItemUuid

@SkyHanniModule
object ShowItemUuid {

    @HandleEvent
    fun onToolTip(event: ToolTipTextEvent) {
        if (!DevApi.config.debug.showItemUuid) return
        event.itemStack.getItemUuid()?.let {
            event.toolTip.add("§7Item UUID: '$it'")
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "dev.showItemUuid", "dev.debug.showItemUuid")
    }
}
