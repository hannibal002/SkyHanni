package at.hannibal2.skyhanni.features.nether.reputationhelper

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HyPixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.DailyQuestHelper
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class CrimsonIsleReputationHelper(skyHanniMod: SkyHanniMod) {

    private val questHelper = DailyQuestHelper(this)

    init {
        skyHanniMod.loadModule(questHelper)
    }

    private val display = mutableListOf<String>()
    private var dirty = true

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!HyPixelData.skyBlock) return
        if (LorenzUtils.skyBlockIsland != IslandType.CRIMSON_ISLE) return
        if (dirty) {
            dirty = false
            updateRender()
        }
    }

    private fun updateRender() {
        display.clear()

        display.add("Reputation Helper:")
        display.add("")
        questHelper.renderAllQuests(display)
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun renderOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return

        if (!HyPixelData.skyBlock) return
        if (LorenzUtils.skyBlockIsland != IslandType.CRIMSON_ISLE) return

        SkyHanniMod.feature.dev.debugPos.renderStrings(display)
    }

    fun update() {
        dirty = true
    }
}