package at.hannibal2.skyhanni.features.nether.reputationhelper.miniboss

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.features.nether.reputationhelper.CrimsonIsleReputationHelper
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.MiniBossQuest
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.QuestState
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.*
import java.util.regex.Pattern

class DailyMiniBossHelper(private val reputationHelper: CrimsonIsleReputationHelper) {

    val miniBosses = mutableListOf<CrimsonMiniBoss>()

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (LorenzUtils.skyBlockIsland != IslandType.CRIMSON_ISLE) return
        if (!SkyHanniMod.feature.misc.crimsonIsleReputationHelper) return

        val message = event.message
        for (miniBoss in miniBosses) {
            if (miniBoss.pattern.matcher(message).matches()) {
                finished(miniBoss)
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (LorenzUtils.skyBlockIsland != IslandType.CRIMSON_ISLE) return
        if (!SkyHanniMod.feature.misc.crimsonIsleReputationHelper) return
        if (!SkyHanniMod.feature.misc.crimsonIsleReputationLocation) return

        val playerLocation = LocationUtils.playerLocation()
        for (miniBoss in miniBosses) {
            if (miniBoss.doneToday && !needMiniBossQuest(miniBoss)) continue
            val location = miniBoss.location ?: continue
            if (DamageIndicatorManager.getNearestDistanceTo(location) < 40 && playerLocation.distance(location) < 40) continue

            event.drawWaypointFilled(location, LorenzColor.WHITE.toColor())
            event.drawDynamicText(location, miniBoss.displayName, 1.5)
        }
    }

    private fun needMiniBossQuest(miniBoss: CrimsonMiniBoss): Boolean {
        val bossQuest = reputationHelper.questHelper.getQuest<MiniBossQuest>()
        if (bossQuest != null) {
            if (bossQuest.miniBoss == miniBoss) {
                if (bossQuest.state == QuestState.ACCEPTED) {
                    return true
                }
            }
        }

        return false
    }

    private fun finished(miniBoss: CrimsonMiniBoss) {
        reputationHelper.questHelper.finishMiniBoss(miniBoss)
        miniBoss.doneToday = true
        reputationHelper.update()
    }

    fun render(display: MutableList<List<Any>>) {
        val done = miniBosses.count { it.doneToday }
        display.add(Collections.singletonList(""))
        display.add(Collections.singletonList("Daily Bosses ($done/5 killed)"))
        if (done != 5) {
            for (miniBoss in miniBosses) {
                val result = if (miniBoss.doneToday) "§7Done" else "§bTodo"
                val displayName = miniBoss.displayName
                val displayItem = miniBoss.displayItem
                if (displayItem == null) {
                    display.add(Collections.singletonList("  $displayName: $result"))
                } else {
                    val lineList = mutableListOf<Any>()
                    lineList.add(" ")
                    lineList.add(NEUItems.readItemFromRepo(displayItem))
                    lineList.add("$displayName: $result")
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

    fun saveConfig() {
        SkyHanniMod.feature.hidden.crimsonIsleMiniBossesDoneToday.clear()

        miniBosses.filter { it.doneToday }
            .forEach { SkyHanniMod.feature.hidden.crimsonIsleMiniBossesDoneToday.add(it.displayName) }
    }

    fun load() {
        miniBosses.clear()

        //Repo
        val repoData = reputationHelper.repoData
        val jsonElement = repoData["MINIBOSS"]
        for ((displayName, extraData) in jsonElement.asJsonObject.entrySet()) {
            val data = extraData.asJsonObject
            val displayItem = data["item"]?.asString
            val patterns = " *§r§6§l${displayName.uppercase()} DOWN!"
            val location = reputationHelper.readLocationData(data)
            miniBosses.add(CrimsonMiniBoss(displayName, displayItem, location, Pattern.compile(patterns)))
        }

        //Config
        for (name in SkyHanniMod.feature.hidden.crimsonIsleMiniBossesDoneToday) {
            getByDisplayName(name)!!.doneToday = true
        }
    }

    private fun getByDisplayName(name: String) = miniBosses.firstOrNull { it.displayName == name }
}