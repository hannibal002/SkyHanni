package at.hannibal2.skyhanni.features.nether.reputationhelper.miniboss

import at.hannibal2.skyhanni.config.Storage
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.CrimsonIsleReputationJson.ReputationQuest
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.features.combat.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.features.nether.reputationhelper.CrimsonIsleReputationHelper
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.MiniBossQuest
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.QuestState
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DailyMiniBossHelper(private val reputationHelper: CrimsonIsleReputationHelper) {

    val miniBosses = mutableListOf<CrimsonMiniBoss>()

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!IslandType.CRIMSON_ISLE.isInIsland()) return
        if (!reputationHelper.config.enabled) return

        val message = event.message
        for (miniBoss in miniBosses) {
            miniBoss.pattern.matchMatcher(message) {
                finished(miniBoss)
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (LorenzUtils.skyBlockIsland != IslandType.CRIMSON_ISLE) return
        if (!reputationHelper.config.enabled) return
        if (!reputationHelper.showLocations()) return

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
        reputationHelper.questHelper.getQuest<MiniBossQuest>()?.let {
            it.miniBoss == miniBoss && it.state == QuestState.ACCEPTED
        } ?: false

    private fun finished(miniBoss: CrimsonMiniBoss) {
        reputationHelper.questHelper.finishMiniBoss(miniBoss)
        miniBoss.doneToday = true
        reputationHelper.update()
    }

    fun render(display: MutableList<List<Any>>) {
        val done = miniBosses.count { it.doneToday }
        display.addAsSingletonList("")
        display.addAsSingletonList("§7Daily Bosses (§e$done§8/§e5 killed§7)")
        if (done != 5) {
            for (miniBoss in miniBosses) {
                val result = if (miniBoss.doneToday) "§aDone" else "§bTodo"
                val displayName = miniBoss.displayName
                val displayItem = miniBoss.displayItem
                if (displayItem == null) {
                    display.addAsSingletonList("  $displayName: $result")
                } else {
                    val lineList = mutableListOf<Any>()
                    lineList.add(" ")
                    lineList.add(getItemStack(displayItem))
                    lineList.add("§5$displayName§7: $result")
                    display.add(lineList)
                }
            }
        }
    }

    fun reset() {
        for (miniBoss in miniBosses) {
            miniBoss.doneToday = false
        }
    }

    fun saveConfig(storage: Storage.ProfileSpecific.CrimsonIsleStorage) {
        storage.miniBossesDoneToday.clear()

        miniBosses.filter { it.doneToday }
            .forEach { storage.miniBossesDoneToday.add(it.displayName) }
    }

    fun onRepoReload(data: Map<String, ReputationQuest>) {
        miniBosses.clear()
        for ((displayName, quest) in data) {
            val displayItem = quest.item
            val pattern = "§f *§r§6§l${displayName.uppercase()} DOWN!".toPattern()
            val location = reputationHelper.readLocationData(quest.location)
            miniBosses.add(CrimsonMiniBoss(displayName, displayItem, location, pattern))
        }
    }

    fun loadData(storage: Storage.ProfileSpecific.CrimsonIsleStorage) {
        if (miniBosses.isEmpty()) return
        for (name in storage.miniBossesDoneToday) {
            getByDisplayName(name)!!.doneToday = true
        }
    }

    private fun getByDisplayName(name: String) = miniBosses.firstOrNull { it.displayName == name }
}
