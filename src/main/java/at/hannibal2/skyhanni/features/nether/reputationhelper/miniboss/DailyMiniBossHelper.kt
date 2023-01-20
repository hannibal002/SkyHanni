package at.hannibal2.skyhanni.features.nether.reputationhelper.miniboss

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HyPixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.nether.reputationhelper.CrimsonIsleReputationHelper
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.*
import java.util.regex.Pattern

class DailyMiniBossHelper(private val reputationHelper: CrimsonIsleReputationHelper) {

    val miniBosses = mutableListOf<CrimsonMiniBoss>()

    fun init() {
        val repoData = reputationHelper.repoData
        val jsonElement = repoData["MINIBOSS"]
        for ((displayName, extraData) in jsonElement.asJsonObject.entrySet()) {
            val data = extraData.asJsonObject
            val displayItem = data["item"]?.asString
            val patterns = " *§r§6§l${displayName.uppercase()} DOWN!"
            miniBosses.add(CrimsonMiniBoss(displayName, displayItem, Pattern.compile(patterns)))
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!HyPixelData.skyBlock) return
        if (LorenzUtils.skyBlockIsland != IslandType.CRIMSON_ISLE) return
        if (!SkyHanniMod.feature.misc.crimsonIsleReputationHelper) return

        val message = event.message
        for (miniBoss in miniBosses) {
            if (miniBoss.pattern.matcher(message).matches()) {
                finished(miniBoss)
            }
        }
    }

    private fun finished(miniBoss: CrimsonMiniBoss) {
        LorenzUtils.debug("Detected mini boss death: ${miniBoss.displayName}")
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

    fun loadConfig() {
        for (name in SkyHanniMod.feature.hidden.crimsonIsleMiniBossesDoneToday) {
            getByDisplayName(name)!!.doneToday = true
        }
    }

    private fun getByDisplayName(name: String) = miniBosses.firstOrNull { it.displayName == name }
}