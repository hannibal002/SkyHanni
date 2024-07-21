package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object TrackerManager {

    private var hasChanged = false
    var dirty = false

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        val config = SkyHanniMod.feature.misc.tracker.hideCheapItems
        ConditionalUtils.onToggle(config.alwaysShowBest, config.minPrice, config.enabled) {
            hasChanged = true
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onRenderOverlayFirst(event: GuiRenderEvent) {
        if (hasChanged) {
            dirty = true
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onRenderOverlayLast(event: GuiRenderEvent) {
        if (hasChanged) {
            dirty = false
            hasChanged = false
        }
    }
}
