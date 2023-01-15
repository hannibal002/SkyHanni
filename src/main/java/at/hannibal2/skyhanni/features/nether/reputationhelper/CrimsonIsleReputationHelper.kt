package at.hannibal2.skyhanni.features.nether.reputationhelper

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HyPixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.FirstConfigLoadedEvent
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.DailyQuestHelper
import at.hannibal2.skyhanni.features.nether.reputationhelper.miniboss.CrimsonMiniBoss
import at.hannibal2.skyhanni.features.nether.reputationhelper.miniboss.DailyMiniBossHelper
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class CrimsonIsleReputationHelper(skyHanniMod: SkyHanniMod) {

    val questHelper = DailyQuestHelper(this)
    private val miniBossHelper = DailyMiniBossHelper(this)

    val miniBosses = mutableListOf<CrimsonMiniBoss>()

    private val display = mutableListOf<String>()
    private var dirty = true

    init {
        skyHanniMod.loadModule(questHelper)
        skyHanniMod.loadModule(miniBossHelper)

        miniBosses.add(CrimsonMiniBoss("Magma Boss"))
        miniBosses.add(CrimsonMiniBoss("Mage Outlaw"))
        miniBosses.add(CrimsonMiniBoss("Barbarian Duke X"))

        miniBosses.add(CrimsonMiniBoss("Bladesoul"))
        miniBosses.add(CrimsonMiniBoss("Ashfang"))

        miniBossHelper.init()
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!HyPixelData.skyBlock) return
        if (LorenzUtils.skyBlockIsland != IslandType.CRIMSON_ISLE) return
        if (dirty) {
            dirty = false
            updateRender()
        }
    }

    @SubscribeEvent
    fun onFirstConfigLoaded(event: FirstConfigLoadedEvent) {
        questHelper.loadConfig()
        miniBossHelper.loadConfig()
    }

    private fun updateRender() {
        display.clear()

        display.add("Reputation Helper:")
        questHelper.render(display)
        miniBossHelper.render(display)
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

        questHelper.saveConfig()
        miniBossHelper.saveConfig()
    }

    fun reset() {
        LorenzUtils.chat("§e[SkyHanni] Reset Reputation Helper.")

        questHelper.reset()
        miniBossHelper.reset()
        update()
    }
}