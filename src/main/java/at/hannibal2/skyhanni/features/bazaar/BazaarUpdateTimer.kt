package at.hannibal2.skyhanni.features.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.BazaarUpdateEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.text.DecimalFormat

class BazaarUpdateTimer {
    private var lastBazaarUpdateTime = 0L

    @SubscribeEvent
    fun onBazaarUpdate(event: BazaarUpdateEvent) {
        if (!isEnabled()) return
        lastBazaarUpdateTime = System.currentTimeMillis()
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun renderOverlay(event: GuiScreenEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return
        if (!BazaarApi.isBazaarInventory(InventoryUtils.openInventoryName())) return

        val duration = System.currentTimeMillis() - lastBazaarUpdateTime
        val durationSeconds = duration.toDouble() / 1000
        val nextUpdateIn = 10 - durationSeconds
        val format = if (nextUpdateIn < 0) {
            "Updating"
        } else {
            DecimalFormat("0.0").format(nextUpdateIn)
        }

        val list = mutableListOf<String>()
        list.add("Next update in:")
        list.add(format)
        SkyHanniMod.feature.bazaar.updateTimerPos.renderStrings(list, center = true)
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyBlock && SkyHanniMod.feature.bazaar.updateTimer
    }
}