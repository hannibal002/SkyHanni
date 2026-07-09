package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.utils.SkyBlockUtils
import java.util.EnumSet

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
    fun isInScoreboardArea(): Boolean = SkyBlockUtils.inSkyBlock && SkyBlockUtils.scoreboardArea?.let { contains(it) } ?: false
    fun isInGraphAreas(): Boolean = SkyBlockUtils.inSkyBlock && SkyBlockUtils.graphArea?.let { contains(it) } ?: false

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
}
