package at.hannibal2.skyhanni.features.nether.reputationhelper

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HyPixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailykuudra.DailyKuudraBossHelper
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.DailyQuestHelper
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.UnknownQuest
import at.hannibal2.skyhanni.features.nether.reputationhelper.miniboss.DailyMiniBossHelper
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import com.google.gson.JsonObject
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.*

class CrimsonIsleReputationHelper(skyHanniMod: SkyHanniMod) {

    val questHelper = DailyQuestHelper(this)
    val miniBossHelper = DailyMiniBossHelper(this)
    val kuudraBossHelper = DailyKuudraBossHelper(this)

    var repoData: JsonObject = JsonObject()

    private val display = mutableListOf<List<Any>>()
    private var dirty = true
    private var loaded = false

    init {
        skyHanniMod.loadModule(questHelper)
        skyHanniMod.loadModule(miniBossHelper)
        skyHanniMod.loadModule(kuudraBossHelper)
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        questHelper.quests.removeIf { it is UnknownQuest}
        repoData = event.getConstant("CrimsonIsleReputation")!!
        if (!loaded) {
            loaded = true

            miniBossHelper.init()
            kuudraBossHelper.init()

            questHelper.loadConfig()
            miniBossHelper.loadConfig()
            kuudraBossHelper.loadConfig()
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!HyPixelData.skyBlock) return
        if (LorenzUtils.skyBlockIsland != IslandType.CRIMSON_ISLE) return
        if (!SkyHanniMod.feature.misc.crimsonIsleReputationHelper) return
        if (dirty) {
            dirty = false
            updateRender()
        }
    }

    private fun updateRender() {
        display.clear()

        display.add(Collections.singletonList("Reputation Helper:"))
        questHelper.render(display)
        miniBossHelper.render(display)
        //TODO check if mage
        kuudraBossHelper.render(display)
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun renderOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return
        if (!SkyHanniMod.feature.misc.crimsonIsleReputationHelper) return

        if (!HyPixelData.skyBlock) return
        if (LorenzUtils.skyBlockIsland != IslandType.CRIMSON_ISLE) return

        SkyHanniMod.feature.misc.crimsonIsleReputationHelperPos.renderStringsAndItems(display)
    }

    fun update() {
        dirty = true

        questHelper.saveConfig()
        miniBossHelper.saveConfig()
        kuudraBossHelper.saveConfig()
    }

    fun reset() {
        LorenzUtils.chat("§e[SkyHanni] Reset Reputation Helper.")

        questHelper.reset()
        miniBossHelper.reset()
        kuudraBossHelper.reset()
        update()
    }
}