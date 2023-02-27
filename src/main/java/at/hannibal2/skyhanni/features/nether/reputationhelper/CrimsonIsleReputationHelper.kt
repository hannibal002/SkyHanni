package at.hannibal2.skyhanni.features.nether.reputationhelper

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailykuudra.DailyKuudraBossHelper
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.DailyQuestHelper
import at.hannibal2.skyhanni.features.nether.reputationhelper.miniboss.DailyMiniBossHelper
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.TabListData
import com.google.gson.JsonObject
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.*

class CrimsonIsleReputationHelper(skyHanniMod: SkyHanniMod) {

    val questHelper = DailyQuestHelper(this)
    val miniBossHelper = DailyMiniBossHelper(this)
    val kuudraBossHelper = DailyKuudraBossHelper(this)

    var repoData: JsonObject = JsonObject()
    var factionType = FactionType.NONE

    private val display = mutableListOf<List<Any>>()
    private var dirty = true
    private var tick = 0

    init {
        skyHanniMod.loadModule(questHelper)
        skyHanniMod.loadModule(miniBossHelper)
        skyHanniMod.loadModule(kuudraBossHelper)
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        repoData = event.getConstant("CrimsonIsleReputation")!!

        miniBossHelper.load()
        kuudraBossHelper.load()

        questHelper.load()
        update()
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (LorenzUtils.skyBlockIsland != IslandType.CRIMSON_ISLE) return
        if (!SkyHanniMod.feature.misc.crimsonIsleReputationHelper) return
        if (dirty) {
            dirty = false
            updateRender()
        }

        tick++
        if (tick % 60 == 0) {
            TabListData.getTabList()
                .filter { it.contains("Reputation:") }
                .forEach {
                    factionType = if (it.contains("Mage")) {
                        FactionType.MAGE
                    } else if (it.contains("Barbarian")) {
                        FactionType.BARBARIAN
                    } else {
                        FactionType.NONE
                    }
                }
        }
    }

    private fun updateRender() {
        display.clear()

        //TODO test
        if (factionType == FactionType.NONE) return

        display.add(Collections.singletonList("Reputation Helper:"))
        questHelper.render(display)
        miniBossHelper.render(display)
        if (factionType == FactionType.MAGE) {
            kuudraBossHelper.render(display)
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun renderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!SkyHanniMod.feature.misc.crimsonIsleReputationHelper) return

        if (!LorenzUtils.inSkyBlock) return
        if (LorenzUtils.skyBlockIsland != IslandType.CRIMSON_ISLE) return

        SkyHanniMod.feature.misc.crimsonIsleReputationHelperPos.renderStringsAndItems(display)
    }

    fun update() {
        questHelper.saveConfig()
        miniBossHelper.saveConfig()
        kuudraBossHelper.saveConfig()

        dirty = true
    }

    fun reset() {
        LorenzUtils.chat("§e[SkyHanni] Reset Reputation Helper.")

        questHelper.reset()
        miniBossHelper.reset()
        kuudraBossHelper.reset()
        update()
    }

    fun readLocationData(data: JsonObject): LorenzVec? {
        val locationData = data["location"]?.asJsonArray ?: return null
        if (locationData.size() == 0) return null

        val x = locationData[0].asDouble - 1
        val y = locationData[1].asDouble
        val z = locationData[2].asDouble - 1
        return LorenzVec(x, y, z)
    }
}