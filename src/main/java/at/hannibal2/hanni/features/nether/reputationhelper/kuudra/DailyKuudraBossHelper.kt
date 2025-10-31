package at.hannibal2.hanni.features.nether.reputationhelper.kuudra

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.storage.ProfileSpecificStorage
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.jsonobjects.repo.ReputationQuest
import at.hannibal2.hanni.events.kuudra.KuudraCompleteEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.features.nether.kuudra.KuudraTier
import at.hannibal2.hanni.features.nether.reputationhelper.CrimsonIsleReputationHelper
import at.hannibal2.hanni.features.nether.reputationhelper.dailyquest.DailyQuestHelper
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.NeuItems.getItemStack
import at.hannibal2.hanni.utils.collection.RenderableCollectionUtils.addItemStack
import at.hannibal2.hanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.hanni.utils.renderables.Renderable
import at.hannibal2.hanni.utils.renderables.addLine

@HanniModule
object DailyKuudraBossHelper {

    val kuudraTiers get() = KuudraTier.entries

    private var kuudraLocation: LorenzVec? = null
    private var allKuudraDone = true

    private val config get() = HanniMod.feature.crimsonIsle.reputationHelper

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!config.enabled.get()) return
        if (!CrimsonIsleReputationHelper.showLocations()) return
        if (allKuudraDone) return

        kuudraLocation?.let {
            event.drawWaypointFilled(it, LorenzColor.WHITE.toColor())
            event.drawDynamicText(it, "Kuudra", 1.5)
        }
    }

    @HandleEvent
    fun onKuudraDone(event: KuudraCompleteEvent) {
        val tier = event.kuudraTier
        val kuudraTier = getByTier(tier) ?: return
        ChatUtils.debug("Detected kuudra tier done: ${kuudraTier.getTieredDisplayName()}")
        DailyQuestHelper.finishKuudra(kuudraTier)
        kuudraTier.doneToday = true
        updateAllKuudraDone()
        CrimsonIsleReputationHelper.update()
    }

    fun MutableList<Renderable>.addKuudraBoss() {
        val done = kuudraTiers.count { it.doneToday }
        addString("")
        addString("§7Daily Kuudra (§e$done§8/§e5 killed§7)")
        if (done < 5) {
            for (tier in kuudraTiers) {
                if (config.hideComplete.get() && tier.doneToday) continue
                val result = if (tier.doneToday) "§aDone" else "§bTodo"
                val displayName = tier.getTieredDisplayName()
                val displayItem = tier.displayItem

                addLine {
                    addString(" ")
                    addItemStack(displayItem.getItemStack())
                    addString("$displayName: $result")
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

    fun saveConfig(storage: ProfileSpecificStorage.CrimsonIsleStorage) {
        storage.kuudraTiersDone.clear()

        kuudraTiers.filter { it.doneToday }
            .forEach { storage.kuudraTiersDone.add(it.name) }
    }

    fun onRepoReload(data: Map<String, ReputationQuest>) {
        var tier = 1
        for ((displayName, kuudraTier) in data) {
            val displayItem = kuudraTier.item
            val location = CrimsonIsleReputationHelper.readLocationData(kuudraTier.location)
            if (location != null) {
                kuudraLocation = location
            }
            KuudraTier.addRepoData(displayName, displayItem, location, tier)

            tier++
        }
    }

    fun loadData(storage: ProfileSpecificStorage.CrimsonIsleStorage) {
        if (kuudraTiers.isEmpty()) return
        for (name in storage.kuudraTiersDone) {
            getByDisplayName(name)?.doneToday = true
        }
        updateAllKuudraDone()
    }

    private fun updateAllKuudraDone() {
        allKuudraDone = !kuudraTiers.any { !it.doneToday }
    }

    private fun getByDisplayName(name: String) = kuudraTiers.firstOrNull { it.name == name }

    private fun getByTier(number: Int) = kuudraTiers.firstOrNull { it.tierNumber == number }
}
