package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.DungeonHubRacesJson
import at.hannibal2.skyhanni.events.ActionBarUpdateEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.ParkourHelper
import at.hannibal2.skyhanni.utils.StringUtils.findMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DungeonsRaceGuide {

    private val config get() = SkyHanniMod.feature.dungeon.dungeonsRaceGuide
    private val raceActivePattern by RepoPattern.pattern(
        "dungeon.race.active",
        "§.§.(?<race>[\\w ]+) RACE §.[\\d:.]+"
    )

    private val parkourHelpers: MutableMap<String, ParkourHelper> = mutableMapOf()

    private var currentRace: String? = null

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        parkourHelpers.forEach { it.value.reset() }
        currentRace = null
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<DungeonHubRacesJson>("DungeonHubRaces")
        data.data.forEach {
            val nothingNoReturn = it.value["nothing:no_return"]
            parkourHelpers[it.key] = ParkourHelper(
                nothingNoReturn?.locations ?: listOf(),
                nothingNoReturn?.shortCuts ?: listOf(),
                platformSize = 1.0,
                detectionRange = 7.0,
                depth = false,
            )
        }
        updateConfig()
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.rainbowColor, config.monochromeColor, config.lookAhead) {
            updateConfig()
        }
    }

    @SubscribeEvent
    fun onActionBarUpdate(event: ActionBarUpdateEvent) {
        if (!isEnabled()) return
        currentRace = null
        raceActivePattern.findMatcher(event.actionBar) {
            currentRace = group("race").replace(" ", "_").lowercase()
        }
        if (currentRace == null) {
            parkourHelpers.forEach {
                it.value.reset()
            }
        }
    }

    private fun updateConfig() {
        parkourHelpers.forEach {
            it.value.rainbowColor = config.rainbowColor.get()
            it.value.monochromeColor = config.monochromeColor.get().toChromaColor()
            it.value.lookAhead = config.lookAhead.get() + 1
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        if (currentRace == null) return

        parkourHelpers[currentRace]?.render(event)
    }

    fun isEnabled() = IslandType.DUNGEON_HUB.isInIsland() && config.enabled
}
