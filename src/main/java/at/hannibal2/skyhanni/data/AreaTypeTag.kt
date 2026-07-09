package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EnumUtils
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import java.util.EnumSet
import kotlin.collections.component1
import kotlin.collections.component2

// TODO maybe rename this class to AreaTypeGroup
/**
 * Each [AreaTypeTag] consists of one or more [AreaType] or [AreaTypeTag]
 */
enum class AreaTypeTag(vararg types: Any) {
    GLACITE_TUNNELS(
        AreaType.GLACITE_TUNNELS,
        AreaType.DWARVEN_BASE_CAMP,
        AreaType.GREAT_GLACITE_LAKE,
        AreaType.FOSSIL_RESEARCH_CENTER,
    ),

    DOJO(
        AreaType.DOJO,
        AreaType.DOJO_ARENA
    ),

    REVENANT(
        AreaType.GRAVEYARD,
        AreaType.REVENANT_CAVE,
    ),

    TARANTULA(
        AreaType.SPIDER_MOUND,
        AreaType.ARACHNES_BURROW,
        AreaType.ARACHNES_SANCTUARY,
        AreaType.BURNING_DESERT,
    ),

    SVEN(
        AreaType.RUINS,
        AreaType.HOWLING_CAVE,
        AreaType.SOUL_CAVE,
        AreaType.SPIRIT_CAVE,
    ),

    VOID(
        AreaType.VOID_SEPULTURE,
        AreaType.ZEALOT_BRUISER_HIDEOUT,
        AreaType.DRAGONS_NEST,
    ),

    INFERNO(
        AreaType.STRONGHOLD,
        AreaType.THE_WASTELAND,
        AreaType.SMOLDERING_TOMB,
    ),

    VAMPIRE(
        AreaType.STILLGORE_CHATEAU,
        AreaType.OUBLIETTE,
    ),

    STILLGORE(
        AreaType.STILLGORE_CHATEAU,
        AreaType.OUBLIETTE,
    ),

    WEST_VILLAGE(
        AreaType.WEST_VILLAGE,
        AreaType.INFESTED_HOUSE,
    ),

    MOUNTAINTOP(
        AreaType.CONTINUUM,
        AreaType.THE_MOUNTAINTOP,
        AreaType.TRIAL_GROUNDS,
        AreaType.TIME_TORN_ISLES,
        AreaType.WIZARDMAN_BUREAU,
        AreaType.WIZARD_BRAWL,
        AreaType.WALK_OF_FAME,
        AreaType.TIME_CHAMBER,
    ),
    ;

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

    private val types: EnumSet<AreaType> = types.fold(
        EnumSet.noneOf(AreaType::class.java),
    ) { set, areaType ->
        set.apply {
            when (areaType) {
                is AreaTypeTag -> addAll(areaType.types)
                is AreaType -> add(areaType)
                else -> error("Invalid type: $areaType")
            }
        }
    }

    operator fun contains(type: AreaType) = type in types

    private fun update(newValues: List<String>) {
        types.clear()
        newValues.mapNotNullTo(types) { EnumUtils.enumValueOfOrNull<AreaType>(it.uppercase()) }
    }

    @SkyHanniModule
    companion object {
        private val repoReloadCoroutine = CoroutineSettings("area type tag repo reload")

        @HandleEvent
        fun onRepoReload(event: RepositoryReloadEvent) = repoReloadCoroutine.launch {
            event.getConstantAsync<Map<String, List<String>>>("AreaTypeTags").forEach { (name, values) ->
                EnumUtils.enumValueOfOrNull<AreaTypeTag>(name.uppercase())?.update(values)
            }
        }
    }
}
