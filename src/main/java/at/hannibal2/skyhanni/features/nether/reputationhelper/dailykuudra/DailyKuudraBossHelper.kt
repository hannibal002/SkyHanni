package at.hannibal2.skyhanni.features.nether.reputationhelper.dailykuudra

import at.hannibal2.skyhanni.config.Storage
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.nether.reputationhelper.CrimsonIsleReputationHelper
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DailyKuudraBossHelper(private val reputationHelper: CrimsonIsleReputationHelper) {
    val kuudraTiers = mutableListOf<KuudraTier>()

    private var kuudraLocation: LorenzVec? = null
    private var allKuudraDone = true

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (LorenzUtils.skyBlockIsland != IslandType.CRIMSON_ISLE) return
        if (!reputationHelper.config.enabled) return
        if (!reputationHelper.showLocations()) return
        if (allKuudraDone) return

        kuudraLocation?.let {
            event.drawWaypointFilled(it, LorenzColor.WHITE.toColor())
            event.drawDynamicText(it, "Kuudra", 1.5)
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (LorenzUtils.skyBlockIsland != IslandType.KUUDRA_ARENA) return
        if (!reputationHelper.config.enabled) return

        val message = event.message
        if (!message.contains("KUUDRA DOWN!") || message.contains(":")) return

        for (line in ScoreboardData.sidebarLines) {
            if (line.contains("Kuudra's") && line.contains("Hollow") && line.contains("(")) {
                val tier = line.substringAfter("(T").substring(0, 1).toInt()
                val kuudraTier = getByTier(tier)!!
                finished(kuudraTier)
                return
            }
        }
    }

    private fun finished(kuudraTier: KuudraTier) {
        LorenzUtils.debug("Detected kuudra tier done: $kuudraTier")
        reputationHelper.questHelper.finishKuudra(kuudraTier)
        kuudraTier.doneToday = true
        updateAllKuudraDone()
        reputationHelper.update()
    }

    fun render(display: MutableList<List<Any>>) {
        val done = kuudraTiers.count { it.doneToday }
        display.addAsSingletonList("")
        display.addAsSingletonList("§7Daily Kuudra (§e$done§8/§e5 killed§7)")
        if (done < 5) {
            for (tier in kuudraTiers) {
                val result = if (tier.doneToday) "§aDone" else "§bTodo"
                val displayName = tier.getDisplayName()
                val displayItem = tier.displayItem
                if (displayItem == null) {
                    display.addAsSingletonList("  $displayName: $result")
                } else {
                    val lineList = mutableListOf<Any>()
                    lineList.add(" ")
                    lineList.add(NEUItems.getItemStack(displayItem))
                    lineList.add("$displayName: $result")
                    display.add(lineList)
                }
            }
        }
    }

    fun reset() {
        for (miniBoss in kuudraTiers) {
            miniBoss.doneToday = false
        }
        updateAllKuudraDone()
    }

    fun saveConfig(storage: Storage.ProfileSpecific.CrimsonIsleStorage) {
        storage.kuudraTiersDone.clear()

        kuudraTiers.filter { it.doneToday }
            .forEach { storage.kuudraTiersDone.add(it.name) }
    }

    fun load(storage: Storage.ProfileSpecific.CrimsonIsleStorage) {
        kuudraTiers.clear()

        // Repo
        val repoData = reputationHelper.repoData ?: return
        val jsonElement = repoData["KUUDRA"]
        var tier = 1
        for ((displayName, extraData) in jsonElement.asJsonObject.entrySet()) {
            val data = extraData.asJsonObject
            val displayItem = data["item"]?.asString
            val location = reputationHelper.readLocationData(data)
            kuudraTiers.add(KuudraTier(displayName, displayItem, location, tier))
            if (location != null) {
                kuudraLocation = location
            }

            tier++
        }

        // Config
        for (name in storage.kuudraTiersDone) {
            getByDisplayName(name)!!.doneToday = true
        }
        updateAllKuudraDone()
    }

    private fun updateAllKuudraDone() {
        allKuudraDone = !kuudraTiers.any { !it.doneToday }
    }

    private fun getByDisplayName(name: String) = kuudraTiers.firstOrNull { it.name == name }

    private fun getByTier(number: Int) = kuudraTiers.firstOrNull { it.tierNumber == number }
}
