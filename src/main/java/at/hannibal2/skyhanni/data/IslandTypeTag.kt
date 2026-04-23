package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EnumUtils
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import java.util.EnumSet

/**
 * Each [IslandTypeTag] consists of one or more [SkyHanniIslandType]
 */
enum class IslandTypeTag(vararg types: SkyHanniIslandType) : SkyHanniIslandType {

    PRIVATE_ISLAND(IslandType.PRIVATE_ISLAND, IslandType.PRIVATE_ISLAND_GUEST),
    GARDEN_ISLAND(IslandType.GARDEN, IslandType.GARDEN_GUEST),
    PERSONAL_ISLAND(PRIVATE_ISLAND, GARDEN_ISLAND),

    IS_COLD(IslandType.DWARVEN_MINES, IslandType.MINESHAFT),
    NORMAL_MINING(IslandType.GOLD_MINES, IslandType.DEEP_CAVERNS),
    ADVANCED_MINING(IS_COLD, IslandType.CRYSTAL_HOLLOWS),
    MINING(NORMAL_MINING, ADVANCED_MINING),
    CUSTOM_MINING(ADVANCED_MINING, IslandType.THE_END, IslandType.CRIMSON_ISLE, IslandType.SPIDER_DEN),

    FORAGING(IslandType.THE_PARK, IslandType.GALATEA),
    FORAGING_CUSTOM_TREES(IslandType.GALATEA),

    HOPPITY_DISALLOWED(IslandType.THE_RIFT, IslandType.KUUDRA_ARENA, IslandType.CATACOMBS, IslandType.MINESHAFT),
    HAS_SHOWCASES(PRIVATE_ISLAND, IslandType.HUB, IslandType.CRIMSON_ISLE),
    CONTESTS_SHOWN(IslandType.GARDEN, IslandType.HUB, IslandType.THE_FARMING_ISLANDS),

    /** Busy islands are islands where a player is doing something considered 'important'. */
    BUSY(IslandType.DARK_AUCTION, IslandType.MINESHAFT, IslandType.THE_RIFT, IslandType.NONE, IslandType.UNKNOWN),

    /** islands without npc locations that are fixed. */
    NO_FIXED_NPC_LOCATIONS(
        IslandType.PRIVATE_ISLAND, IslandType.PRIVATE_ISLAND_GUEST, IslandType.KUUDRA_ARENA,
        IslandType.CATACOMBS,
        IslandType.GARDEN,
        IslandType.GARDEN_GUEST,
        IslandType.MINESHAFT,
        IslandType.NONE,
        IslandType.ANY,
        IslandType.UNKNOWN,
    ),
    ;

    private val types: EnumSet<IslandType> = types.fold(
        EnumSet.noneOf(IslandType::class.java),
    ) { set, islandType ->
        set.apply {
            when (islandType) {
                is IslandTypeTag -> addAll(islandType.types)
                is IslandType -> add(islandType)
                else -> error("Invalid type: $islandType")
            }
        }
    }

    private fun update(newValues: List<String>) {
        types.clear()
        newValues.mapNotNullTo(types) { EnumUtils.enumValueOfOrNull<IslandType>(it.uppercase()) }
    }

    override fun isInIsland(): Boolean = SkyBlockUtils.inSkyBlock && contains(SkyBlockUtils.currentIsland)

    operator fun contains(type: IslandType) = type in types

    @SkyHanniModule
    companion object {

        fun Collection<IslandTypeTag>.isInAnyIsland(): Boolean = any { it.isInIsland() }

        private val repoReloadCoroutine = CoroutineSettings("island type tag repo reload")

        @HandleEvent
        fun onRepoReload(event: RepositoryReloadEvent) = repoReloadCoroutine.launch {
            event.getConstantAsync<Map<String, List<String>>>("IslandTypeTags").forEach { (name, values) ->
                EnumUtils.enumValueOfOrNull<IslandTypeTag>(name.uppercase())?.update(values)
            }
        }
    }
}
