package at.hannibal2.skyhanni.features.nether.reputationhelper.miniboss

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.ReputationQuest
import at.hannibal2.skyhanni.events.combat.CrimsonMiniBossEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.combat.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.features.nether.miniboss.CrimsonMiniBoss
import at.hannibal2.skyhanni.features.nether.reputationhelper.CrimsonIsleReputationHelper
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.DailyQuestHelper
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.MiniBossQuest
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.QuestState
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addItemStack
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.addLine

@SkyHanniModule
object DailyMiniBossHelper {

    val miniBosses get() = CrimsonMiniBoss.entries

    private val config get() = SkyHanniMod.feature.crimsonIsle.reputationHelper

    @HandleEvent
    fun onCrimsonMiniBossDeath(event: CrimsonMiniBossEvent.Death) {
        val miniBoss = event.miniBoss
        ChatUtils.debug("Detected crimson miniboss done: ${miniBoss.displayName}")
        DailyQuestHelper.finishMiniBoss(miniBoss)
        miniBoss.doneToday = true
        CrimsonIsleReputationHelper.update()
    }

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.enabled.get()) return
        if (!CrimsonIsleReputationHelper.showLocations()) return

        val playerLocation = LocationUtils.playerLocation()
        for (miniBoss in miniBosses) {
            if (miniBoss.doneToday && !needMiniBossQuest(miniBoss)) continue
            val location = miniBoss.location ?: continue
            if (DamageIndicatorManager.getNearestDistanceTo(location) < 40 && playerLocation.distance(location) < 40) continue

            event.drawWaypointFilled(location, LorenzColor.WHITE.toColor())
            event.drawDynamicText(location, miniBoss.displayName, 1.5)
        }
    }

    private fun needMiniBossQuest(miniBoss: CrimsonMiniBoss) =
        DailyQuestHelper.getQuest<MiniBossQuest>()?.let {
            it.miniBoss == miniBoss && it.state == QuestState.ACCEPTED
        } ?: false

    fun MutableList<Renderable>.addDailyMiniBoss() {
        val done = miniBosses.count { it.doneToday }
        val total = miniBosses.size

        addString("")
        addString("§7Daily Bosses (§e$done§8/§e$total killed§7)")

        if (done < total) {
            for (miniBoss in miniBosses) {
                if (config.hideComplete.get() && miniBoss.doneToday) continue
                val result = if (miniBoss.doneToday) "§aDone" else "§bTodo"
                val displayName = miniBoss.displayName
                val displayItem = miniBoss.displayItem

                addLine {
                    addString(" ")
                    addItemStack(displayItem.getItemStack())
                    addString("§5$displayName§7: $result")
                }
            }
        }
    }

    fun reset() {
        for (miniBoss in miniBosses) {
            miniBoss.doneToday = false
        }
    }

    fun saveConfig(storage: ProfileSpecificStorage.CrimsonIsleStorage) {
        storage.miniBossesDoneToday.clear()

        miniBosses.filter { it.doneToday }
            .forEach { storage.miniBossesDoneToday.add(it.name) }
    }

    fun processRepoData(data: Map<String, ReputationQuest>) {
        for ((displayName, miniBoss) in data) {
            val displayItem = miniBoss.item
            val location = CrimsonIsleReputationHelper.readLocationData(miniBoss.location)
            CrimsonMiniBoss.addRepoData(displayName, displayItem, location)
        }
    }

    fun loadData(storage: ProfileSpecificStorage.CrimsonIsleStorage) {
        if (miniBosses.isEmpty()) return
        for (name in storage.miniBossesDoneToday) {
            getByName(name)?.doneToday = true
        }
    }

    private fun getByName(name: String) = miniBosses.firstOrNull { it.name == name }
}
