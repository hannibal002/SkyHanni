package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.SkyHanniToolTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked
import at.hannibal2.skyhanni.utils.LorenzUtils

@SkyHanniModule
object FocusMode {

    private val config get() = SkyHanniMod.feature.inventory.focusMode

    private var toggle = true

    @HandleEvent(priority = HandleEvent.LOWEST)
    fun onLorenzToolTip(event: SkyHanniToolTipEvent) {
        if (!isEnabled() || !toggle) return
        if(event.toolTip.isEmpty()) return
        event.toolTip = mutableListOf(event.toolTip.first())
    }

    @HandleEvent
    fun onLorenzTick(event: SkyHanniTickEvent) {
        if (!isEnabled()) return
        if (!config.toggleKey.isKeyClicked()) return
        toggle = !toggle
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && InventoryUtils.inContainer() && config.enabled
}
