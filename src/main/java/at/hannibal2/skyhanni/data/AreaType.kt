package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.event.HandleEvent.Companion.HIGHEST
import at.hannibal2.skyhanni.data.jsonobjects.repo.AreaTypeJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.skyblock.AreaChangeEvent
import at.hannibal2.skyhanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.skyhanni.events.skyblock.ScoreboardAreaChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings

// TODO: Repofiy
enum class AreaType(private val nameFallback: String) {
    BELLY_OF_THE_BEAST("Belly of the Beast"),
    THE_END("The End"),
    KUUDRA("Kuudra"),
    CRIMSON_ISLE("Crimson Isle"),
    DWARVEN_MINES("Dwarven Mines"),
    DUNGEON_HUB("Dungeon Hub"),
    GLACIAL_CAVE("Glacial Cave"),
    THE_MIST("The Mist"),
    CARNIVAL("Carnival"),
    CRYSTAL_NUCLEUS("Crystal Nucleus"),
    GLACITE_TUNNELS("Glacite Tunnels"),
    DWARVEN_BASE_CAMP("Dwarven Base Camp"),
    GREAT_GLACITE_LAKE("Great Glacite Lake"),
    FOSSIL_RESEARCH_CENTER("Fossil Research Center"),
    FORGOTTEN_SKULL("Forgotten Skull"),
    MINES_OF_DIVAN("Mines of Divan"),
    COMMUNITY_CENTER("Community Center"),
    FASHION_SHOP("Fashion Shop"),
    SHENS_AUCTION("Shen's Auction"),
    BLAZING_VOLCANO("Blazing Volcano"),
    BAZAAR_ALLEY("Bazaar Alley"),
    FARM("Farm"),
    GRAVEYARD("Graveyard"),
    REVENANT_CAVE("Revenant Cave"),
    SPIDER_MOUND("Spider Mound"),
    ARACHNES_BURROW("Arachne's Burrow"),
    ARACHNES_SANCTUARY("Arachne's Sanctuary"),
    BURNING_DESERT("Burning Desert"),
    RUINS("Ruins"),
    HOWLING_CAVE("Howling Cave"),
    SOUL_CAVE("Soul Cave"),
    SPIRIT_CAVE("Spirit Cave"),
    VOID_SEPULTURE("Void Sepulture"),
    ZEALOT_BRUISER_HIDEOUT("Zealot Bruiser Hideout"),
    DRAGONS_NEST("Dragon's Nest"),
    STRONGHOLD("Stronghold"),
    THE_WASTELAND("The Wasteland"),
    SMOLDERING_TOMB("Smoldering Tomb"),
    STILLGORE_CHATEAU("Stillgore Château"),
    OUBLIETTE("Oubliette"),
    DOJO("Dojo"),
    DOJO_ARENA("Dojo Arena"),
    GUNPOWDER_MINES("Gunpowder Mines"),
    OBSIDIAN_SANCTUARY("Obsidian Sanctuary"),
    ROYAL_PALACE("Royal Palace"),
    DRAGONTAIL("Dragontail"),
    LIVING_CAVE("Living Cave"),
    LIVING_STILLNESS("Living Stillness"),
    COLOSSEUM("Colosseum"),
    DREADFARM("Dreadfarm"),
    WEST_VILLAGE("West Village"),
    INFESTED_HOUSE("Infested House"),
    CONTINUUM("Continuum"),
    THE_MOUNTAINTOP("The Mountaintop"),
    TRIAL_GROUNDS("Trial Grounds"),
    TIME_TORN_ISLES("Time-Torn Isles"),
    WIZARDMAN_BUREAU("Wizardman Bureau"),
    WIZARD_BRAWL("Wizard Brawl"),
    WALK_OF_FAME("Walk of Fame"),
    TIME_CHAMBER("Time Chamber"),

    NONE(""),
    UNKNOWN("???"),
    ;

    var areaData: AreaData? = null
        private set

    val displayName: String get() = areaData?.name ?: nameFallback

    fun isInScoreboardArea(): Boolean = SkyBlockUtils.scoreboardArea == nameFallback
    fun isInGraphArea(): Boolean = SkyBlockUtils.graphArea == nameFallback
    fun isInArea(): Boolean = SkyBlockUtils.area == this

    @SkyHanniModule
    companion object {
        private var currentArea = NONE

        fun getByName(name: String): AreaType = getByNameOrNull(name) ?: error("AreaType not found: '$name'")
        fun getByNameOrUnknown(name: String): AreaType = getByNameOrNull(name) ?: UNKNOWN
        fun getByNameOrNull(name: String): AreaType? = AreaType.entries.find { it.displayName == name }

        val currentScoreboardArea get() = SkyBlockUtils.scoreboardArea?.let { getByNameOrUnknown(it) } ?: UNKNOWN
        val currentGraphArea get() = SkyBlockUtils.graphArea?.let { getByNameOrUnknown(it) } ?: UNKNOWN

        private val repoReloadCoroutine = CoroutineSettings("area type repo reload")

        @HandleEvent(priority = HIGHEST)
        fun onGraphAreaChange(event: GraphAreaChangeEvent) {
            val areaType = getByNameOrUnknown(event.area)
            if (currentArea == areaType) return
            currentArea = areaType
            AreaChangeEvent(areaType, currentArea).post()
        }

        @HandleEvent(priority = HIGHEST)
        fun onScoreboardAreaChange(event: ScoreboardAreaChangeEvent) {
            val areaType = getByNameOrUnknown(event.area)
            if (currentArea == areaType) return
            currentArea = areaType
            AreaChangeEvent(areaType, currentArea).post()
        }

        @HandleEvent(priority = HIGHEST)
        fun onRepoReload(event: RepositoryReloadEvent) = repoReloadCoroutine.launch {
            val data = event.getConstantAsync<AreaTypeJson>("misc/AreaType")

            AreaType.entries.forEach { islandType ->
                islandType.areaData = data.areas[islandType.name]?.let { area ->
                    AreaData(area.name)
                }
            }
        }
    }
}

data class AreaData(
    val name: String,
)
