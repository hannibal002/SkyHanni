package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings

// TODO maybe rename this class to AreaTypeGroup
/**
 * Each [AreaTypeTag] consists of one or more [AreaType] or [AreaTypeTag]
 */
class AreaTypeTag private constructor(
    val name: String,
    private val defaultTypes: List<Any>,
) {
    private val types: MutableSet<AreaType> = defaultTypes.fold(
        mutableSetOf(),
    ) { set, areaType ->
        set.apply {
            when (areaType) {
                is AreaTypeTag -> addAll(areaType.types)
                is AreaType -> add(areaType)
                else -> error("Invalid type: $areaType")
            }
        }
    }

    private fun update(newValues: List<String>) {
        types.clear()
        newValues.mapNotNullTo(types) {
            AreaType.getByNameOrNull(it.uppercase())
        }
    }

    internal val allTypes: Set<AreaType>
        get() = types

    fun isInArea(): Boolean = SkyBlockUtils.inSkyBlock && contains(SkyBlockUtils.area)

    fun isInScoreboardArea(): Boolean {
        if (!SkyBlockUtils.inSkyBlock) return false
        val scoreboardArea = SkyBlockUtils.scoreboardArea ?: return false
        val areaType = AreaType.getByNameOrUnknown(scoreboardArea)
        return contains(areaType)
    }

    fun isInGraphArea(): Boolean {
        if (!SkyBlockUtils.inSkyBlock) return false
        val graphArea = SkyBlockUtils.graphArea ?: return false
        val areaType = AreaType.getByNameOrUnknown(graphArea)
        return contains(areaType)
    }

    operator fun contains(type: AreaType): Boolean = type in types

    override fun toString(): String = name

    @SkyHanniModule
    companion object {
        private val entriesList = mutableListOf<AreaTypeTag>()

        private fun create(identifier: String, vararg types: Any): AreaTypeTag {
            return AreaTypeTag(identifier, types.toList())
                .also(entriesList::add)
        }

        val MINABLE_GLACITE_TUNNELS = create(
            "MINABLE_GLACITE_TUNNELS",
            AreaType.GLACITE_TUNNELS,
            AreaType.GREAT_GLACITE_LAKE,
        )

        val GLACITE_TUNNELS = create(
            "GLACITE_TUNNELS",
            AreaType.DWARVEN_BASE_CAMP,
            AreaType.FOSSIL_RESEARCH_CENTER,
            MINABLE_GLACITE_TUNNELS,
        )

        val DOJO = create(
            "DOJO",
            AreaType.DOJO,
            AreaType.DOJO_ARENA,
        )

        val REVENANT = create(
            "REVENANT",
            AreaType.GRAVEYARD,
            AreaType.REVENANT_CAVE,
        )

        val TARANTULA = create(
            "TARANTULA",
            AreaType.SPIDER_MOUND,
            AreaType.ARACHNES_BURROW,
            AreaType.ARACHNES_SANCTUARY,
            AreaType.BURNING_DESERT,
        )

        val SVEN = create(
            "SVEN",
            AreaType.RUINS,
            AreaType.HOWLING_CAVE,
            AreaType.SOUL_CAVE,
            AreaType.SPIRIT_CAVE,
        )

        val VOID = create(
            "VOID",
            AreaType.VOID_SEPULTURE,
            AreaType.ZEALOT_BRUISER_HIDEOUT,
            AreaType.DRAGONS_NEST,
        )

        val INFERNO = create(
            "INFERNO",
            AreaType.STRONGHOLD,
            AreaType.THE_WASTELAND,
            AreaType.SMOLDERING_TOMB,
        )

        val VAMPIRE = create(
            "VAMPIRE",
            AreaType.STILLGORE_CHATEAU,
            AreaType.OUBLIETTE,
        )

        val STILLGORE = create(
            "STILLGORE",
            AreaType.STILLGORE_CHATEAU,
            AreaType.OUBLIETTE,
        )

        val WEST_VILLAGE = create(
            "WEST_VILLAGE",
            AreaType.WEST_VILLAGE,
            AreaType.INFESTED_HOUSE,
        )

        val MOUNTAINTOP = create(
            "MOUNTAINTOP",
            AreaType.CONTINUUM,
            AreaType.THE_MOUNTAINTOP,
            AreaType.TRIAL_GROUNDS,
            AreaType.TIME_TORN_ISLES,
            AreaType.WIZARDMAN_BUREAU,
            AreaType.WIZARD_BRAWL,
            AreaType.WALK_OF_FAME,
            AreaType.TIME_CHAMBER,
        )

        val IS_COLD = create(
            "IS_COLD",
            MINABLE_GLACITE_TUNNELS,
            AreaType.ICY_BIOME
        )

        val entries: List<AreaTypeTag>
            get() = entriesList

        private val repoReloadCoroutine = CoroutineSettings("area type tag repo reload")

        @HandleEvent
        private fun onRepoReload(event: RepositoryReloadEvent) = repoReloadCoroutine.launch {
            event.getConstantAsync<Map<String, List<String>>>("AreaTypeTags")
                .forEach { (name, values) ->
                    entriesList
                        .find { it.name.equals(name, ignoreCase = true) }
                        ?.update(values)
                }
        }
    }
}
