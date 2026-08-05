package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.event.HandleEvent.Companion.HIGHEST
import at.hannibal2.skyhanni.data.jsonobjects.repo.AreaTypeJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.toEnumName
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings

class AreaType private constructor(
    val identifier: String,
    private val nameFallback: String,
) {
    var areaData: AreaData? = null
        private set

    val displayName: String
        get() = areaData?.name ?: nameFallback

    fun isInScoreboardArea(): Boolean = SkyBlockUtils.scoreboardArea == displayName
    fun isInGraphArea(): Boolean = SkyBlockUtils.graphArea == displayName
    fun isInArea(): Boolean = SkyBlockUtils.area == this

    override fun toString(): String = identifier
    override fun equals(other: Any?): Boolean = this === other || (other is AreaType && identifier == other.identifier)
    override fun hashCode(): Int = identifier.hashCode()

    @SkyHanniModule
    companion object {
        private val entriesList = mutableListOf<AreaType>()

        private fun create(nameFallback: String): AreaType = create(nameFallback.toEnumName(), nameFallback)

        private fun create(identifier: String, nameFallback: String): AreaType =
            AreaType(
                identifier = identifier,
                nameFallback = nameFallback,
            ).also {
                entriesList.add(it)
            }

        val BELLY_OF_THE_BEAST = create("Belly of the Beast")
        val THE_END = create("The End")
        val KUUDRA = create("Kuudra")
        val CRIMSON_ISLE = create("Crimson Isle")
        val DWARVEN_MINES = create("Dwarven Mines")
        val DUNGEON_HUB = create("Dungeon Hub")
        val GLACIAL_CAVE = create("Glacial Cave")
        val THE_MIST = create("The Mist")
        val CARNIVAL = create("Carnival")
        val CRYSTAL_NUCLEUS = create("Crystal Nucleus")
        val GLACITE_TUNNELS = create("Glacite Tunnels")
        val DWARVEN_BASE_CAMP = create("Dwarven Base Camp")
        val GREAT_GLACITE_LAKE = create("Great Glacite Lake")
        val FOSSIL_RESEARCH_CENTER = create("Fossil Research Center")
        val FORGOTTEN_SKULL = create("Forgotten Skull")
        val MINES_OF_DIVAN = create("Mines of Divan")
        val COMMUNITY_CENTER = create("Community Center")
        val FASHION_SHOP = create("Fashion Shop")
        val SHENS_AUCTION = create("Shen's Auction")
        val BLAZING_VOLCANO = create("Blazing Volcano")
        val BAZAAR_ALLEY = create("Bazaar Alley")
        val FARM = create("Farm")
        val GRAVEYARD = create("Graveyard")
        val REVENANT_CAVE = create("Revenant Cave")
        val SPIDER_MOUND = create("Spider Mound")
        val ARACHNES_BURROW = create("Arachne's Burrow")
        val ARACHNES_SANCTUARY = create("Arachne's Sanctuary")
        val BURNING_DESERT = create("Burning Desert")
        val RUINS = create("Ruins")
        val HOWLING_CAVE = create("Howling Cave")
        val SOUL_CAVE = create("Soul Cave")
        val SPIRIT_CAVE = create("Spirit Cave")
        val VOID_SEPULTURE = create("Void Sepulture")
        val ZEALOT_BRUISER_HIDEOUT = create("Zealot Bruiser Hideout")
        val DRAGONS_NEST = create("Dragon's Nest")
        val STRONGHOLD = create("Stronghold")
        val THE_WASTELAND = create("The Wasteland")
        val SMOLDERING_TOMB = create("Smoldering Tomb")
        val STILLGORE_CHATEAU = create("Stillgore Château")
        val OUBLIETTE = create("Oubliette")
        val DOJO = create("Dojo")
        val DOJO_ARENA = create("Dojo Arena")
        val GUNPOWDER_MINES = create("Gunpowder Mines")
        val OBSIDIAN_SANCTUARY = create("Obsidian Sanctuary")
        val ROYAL_PALACE = create("Royal Palace")
        val DRAGONTAIL = create("Dragontail")
        val LIVING_CAVE = create("Living Cave")
        val LIVING_STILLNESS = create("Living Stillness")
        val COLOSSEUM = create("Colosseum")
        val DREADFARM = create("Dreadfarm")
        val WEST_VILLAGE = create("West Village")
        val INFESTED_HOUSE = create("Infested House")
        val CONTINUUM = create("Continuum")
        val THE_MOUNTAINTOP = create("The Mountaintop")
        val TRIAL_GROUNDS = create("Trial Grounds")
        val TIME_TORN_ISLES = create("Time-Torn Isles")
        val WIZARDMAN_BUREAU = create("Wizardman Bureau")
        val WIZARD_BRAWL = create("Wizard Brawl")
        val WALK_OF_FAME = create("Walk of Fame")
        val TIME_CHAMBER = create("Time Chamber")
        val ICY_BIOME = create("Icy Biome")
        val YOUR_ISLAND = create("Your Island")
        val VILLAGE = create("Village")

        val NONE = create("")
        val UNKNOWN = create("???")

        val entries: List<AreaType>
            get() = entriesList

        private val repoReloadCoroutine = CoroutineSettings("area type repo reload")

        @HandleEvent(priority = HIGHEST)
        private fun onRepoReload(event: RepositoryReloadEvent) = repoReloadCoroutine.launch {
            val data = event.getConstantAsync<AreaTypeJson>("misc/AreaType")

            entries.forEach { areaType ->
                areaType.areaData = data.areas[areaType.identifier]?.let { area ->
                    AreaData(area.name)
                }
            }
        }

        fun getByName(name: String): AreaType =
            getByNameOrNull(name) ?: error("AreaType not found: '$name'")

        fun getByNameOrUnknown(name: String): AreaType =
            getByNameOrNull(name) ?: UNKNOWN

        fun getByNameOrNull(name: String): AreaType? =
            entries.find { it.displayName == name }
    }
}

data class AreaData(
    val name: String,
)
