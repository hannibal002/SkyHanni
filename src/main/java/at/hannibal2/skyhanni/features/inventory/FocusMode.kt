package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.SkyhanniTickEvent
import at.hannibal2.skyhanni.events.SkyhanniToolTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object FocusMode {

    private val config get() = SkyHanniMod.feature.inventory.focusMode

    private var toggle = true

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onLorenzToolTip(event: SkyhanniToolTipEvent) {
        if (!isEnabled() || !toggle) return
        if(event.toolTip.isEmpty()) return
        event.toolTip = mutableListOf(event.toolTip.first())
    }

    @SubscribeEvent
    fun onLorenzTick(event: SkyhanniTickEvent) {
        if (!isEnabled()) return
        if (!config.toggleKey.isKeyClicked()) return
        toggle = !toggle
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && InventoryUtils.inContainer() && config.enabled
}
