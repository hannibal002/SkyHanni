package at.hannibal2.skyhanni.features.nether.reputationhelper

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailykuudra.DailyKuudraBossHelper
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.DailyQuestHelper
import at.hannibal2.skyhanni.features.nether.reputationhelper.miniboss.DailyMiniBossHelper
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.TabListData
import com.google.gson.JsonObject
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class CrimsonIsleReputationHelper(skyHanniMod: SkyHanniMod) {

    val questHelper = DailyQuestHelper(this)
    val miniBossHelper = DailyMiniBossHelper(this)
    val kuudraBossHelper = DailyKuudraBossHelper(this)

    var repoData: JsonObject = JsonObject()
    var factionType = FactionType.NONE

    private var display = listOf<List<Any>>()
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

        tryLoadConfig()
        update()
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        tryLoadConfig()
    }

    private fun tryLoadConfig() {
        ProfileStorageData.profileSpecific?.crimsonIsle?.let {
            miniBossHelper.load(it)
            kuudraBossHelper.load(it)
            questHelper.load(it)
        }
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
        val newList = mutableListOf<List<Any>>()

        //TODO test
        if (factionType == FactionType.NONE) return

        newList.addAsSingletonList("Reputation Helper:")
        questHelper.render(newList)
        miniBossHelper.render(newList)
        if (factionType == FactionType.MAGE) {
            kuudraBossHelper.render(newList)
        }

        display = newList
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun renderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!SkyHanniMod.feature.misc.crimsonIsleReputationHelper) return

        if (!LorenzUtils.inSkyBlock) return
        if (LorenzUtils.skyBlockIsland != IslandType.CRIMSON_ISLE) return

        SkyHanniMod.feature.misc.crimsonIsleReputationHelperPos.renderStringsAndItems(display, posLabel = "Crimson Isle Reputation Helper")
    }

    fun update() {
        ProfileStorageData.profileSpecific?.crimsonIsle?.let {
            questHelper.saveConfig(it)
            miniBossHelper.saveConfig(it)
            kuudraBossHelper.saveConfig(it)
        }

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