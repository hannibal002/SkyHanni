package at.hannibal2.skyhanni.features.nether.reputationhelper

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.crimsonisle.ReputationHelperConfig
import at.hannibal2.skyhanni.config.features.crimsonisle.ReputationHelperConfig.ShowLocationEntry
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.repo.CrimsonIsleReputationJson
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailykuudra.DailyKuudraBossHelper
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.DailyQuestHelper
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.QuestLoader
import at.hannibal2.skyhanni.features.nether.reputationhelper.miniboss.DailyMiniBossHelper
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.TabListData
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CrimsonIsleReputationHelper(skyHanniMod: SkyHanniMod) {
    val config get() = SkyHanniMod.feature.crimsonIsle.reputationHelper

    val questHelper = DailyQuestHelper(this)
    val miniBossHelper = DailyMiniBossHelper(this)
    val kuudraBossHelper = DailyKuudraBossHelper(this)

    var factionType = FactionType.NONE

    private var display = emptyList<List<Any>>()
    private var dirty = true

    init {
        skyHanniMod.loadModule(questHelper)
        skyHanniMod.loadModule(miniBossHelper)
        skyHanniMod.loadModule(kuudraBossHelper)
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<CrimsonIsleReputationJson>("CrimsonIsleReputation")
        miniBossHelper.onRepoReload(data.MINIBOSS)
        kuudraBossHelper.onRepoReload(data.KUUDRA)

        QuestLoader.quests.clear()
        QuestLoader.loadQuests(data.FISHING, "FISHING")
        QuestLoader.loadQuests(data.RESCUE, "RESCUE")
        QuestLoader.loadQuests(data.FETCH, "FETCH")
        QuestLoader.loadQuests(data.DOJO, "DOJO")

        update()
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ProfileStorageData.profileSpecific?.crimsonIsle?.let {
            miniBossHelper.loadData(it)
            kuudraBossHelper.loadData(it)
            questHelper.load(it)
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!IslandType.CRIMSON_ISLE.isInIsland()) return
        if (!config.enabled) return
        if (!dirty && display.isEmpty()) {
            dirty = true
        }
        if (dirty) {
            dirty = false
            updateRender()
        }

        if (event.repeatSeconds(3)) {
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
    fun renderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.enabled) return
        if (!IslandType.CRIMSON_ISLE.isInIsland()) return

        if (config.useHotkey && !config.hotkey.isKeyHeld()) {
            return
        }

        config.position.renderStringsAndItems(
            display,
            posLabel = "Crimson Isle Reputation Helper"
        )
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.crimsonIsleReputationHelper", "crimsonIsle.reputationHelper.enabled")
        event.move(2, "misc.reputationHelperUseHotkey", "crimsonIsle.reputationHelper.useHotkey")
        event.move(2, "misc.reputationHelperHotkey", "crimsonIsle.reputationHelper.hotkey")
        event.move(2, "misc.crimsonIsleReputationHelperPos", "crimsonIsle.reputationHelper.position")
        event.move(2, "misc.crimsonIsleReputationShowLocation", "crimsonIsle.reputationHelper.showLocation")
        // TODO Replace with transform when PR 769 is merged
        event.move(14, "crimsonIsle.reputationHelper.showLocation") { element ->
            ConfigUtils.migrateIntToEnum(element, ShowLocationEntry::class.java)
        }
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
        LorenzUtils.chat("Reset Reputation Helper.")

        questHelper.reset()
        miniBossHelper.reset()
        kuudraBossHelper.reset()
        update()
    }

    fun readLocationData(locations: List<Double>): LorenzVec? {
        if (locations.isEmpty()) return null
        val (x, y, z) = locations
        return LorenzVec(x, y, z).add(-1, 0, -1)
    }

    fun showLocations() = when (config.showLocation) {
        ReputationHelperConfig.ShowLocationEntry.ALWAYS -> true
        ReputationHelperConfig.ShowLocationEntry.ONLY_HOTKEY -> config.hotkey.isKeyHeld()
        else -> false
    }
}
