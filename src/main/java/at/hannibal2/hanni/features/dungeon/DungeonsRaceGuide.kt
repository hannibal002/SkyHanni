package at.hannibal2.hanni.features.dungeon

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.jsonobjects.repo.DungeonHubRacesJson
import at.hannibal2.hanni.events.ActionBarUpdateEvent
import at.hannibal2.hanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.hanni.events.RepositoryReloadEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ConditionalUtils
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.hanni.utils.ParkourHelper
import at.hannibal2.hanni.utils.RegexUtils.findMatcher
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object DungeonsRaceGuide {

    private val config get() = HanniMod.feature.dungeon.dungeonsRaceGuide

    private val CANCEL_RACE_ITEM = "CANCEL_RACE_ITEM".toInternalName()

    /**
     * REGEX-TEST: §D§LPRECURSOR RUINS RACE §e00:05.443            §b1577/1577✎ Mana
     */
    private val raceActivePattern by RepoPattern.pattern(
        "dungeon.race.active",
        "§.§.(?<race>[\\w ]+) RACE §.[\\d:.]+",
    )

    private val parkourHelpers: MutableMap<String, ParkourHelper> = mutableMapOf()

    private var inRace: Boolean = false
    private var currentRace: String? = null

    @HandleEvent
    fun onIslandChange() {
        reset()
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<DungeonHubRacesJson>("DungeonHubRaces")
        for ((key, map) in data.data) {
            val nothingNoReturn = map["nothing:no_return"]
            parkourHelpers[key] = ParkourHelper(
                nothingNoReturn?.locations.orEmpty(),
                nothingNoReturn?.shortCuts.orEmpty(),
                platformSize = 1.0,
                detectionRange = 7.0,
                depth = false,
            )
        }
        updateConfig()
    }

    @HandleEvent
    fun onConfigLoad() {
        ConditionalUtils.onToggle(config.rainbowColor, config.monochromeColor, config.lookAhead) {
            updateConfig()
        }
    }

    @HandleEvent(OwnInventoryItemUpdateEvent::class, onlyOnIsland = IslandType.DUNGEON_HUB)
    fun onOwnInventoryItemUpdate() {
        val menuStack = InventoryUtils.getItemsInOwnInventoryWithNull()?.get(8)
        val nowInRace = menuStack?.getInternalNameOrNull() == CANCEL_RACE_ITEM
        if (inRace && !nowInRace) {
            reset()
        }
        inRace = nowInRace
    }

    @HandleEvent(onlyOnIsland = IslandType.DUNGEON_HUB)
    fun onActionBarUpdate(event: ActionBarUpdateEvent) {
        if (!config.enabled) return
        raceActivePattern.findMatcher(event.actionBar) {
            currentRace = group("race").replace(" ", "_").lowercase()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.DUNGEON_HUB)
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!config.enabled || !inRace) return
        currentRace?.let { parkourHelpers[it]?.render(event) }
    }

    private fun updateConfig() {
        parkourHelpers.values.forEach {
            it.rainbowColor = config.rainbowColor.get()
            it.monochromeColor = config.monochromeColor.get()
            it.lookAhead = config.lookAhead.get() + 1
        }
    }

    private fun reset() {
        inRace = false
        currentRace = null
        parkourHelpers.forEach { it.value.reset() }
    }
}
