package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.annotations.Expose

@SkyHanniModule
object FairySoulsApi {
    private val storage: MutableSet<IslandFairySoulsData>
        get() = ProfileStorageData.profileSpecific?.fairySouls ?: mutableSetOf()

    class IslandFairySoulsData(
        @Expose
        val island: IslandType,
        @Expose
        val onlyInAreas: MutableSet<String> = mutableSetOf(),
    ) {
        @Expose
        var amountFound: Int = 0

        @Expose
        val foundSouls: MutableSet<LorenzVec> = mutableSetOf()

        fun add(vec: LorenzVec) {
            foundSouls.add(vec)
            amountFound = foundSouls.size
        }

        fun addAll(vecs: Collection<LorenzVec>) {
            foundSouls.addAll(vecs)
            amountFound = foundSouls.size
        }

        fun remove(vec: LorenzVec) {
            foundSouls.remove(vec)
            amountFound = foundSouls.size
        }

        fun reset() {
            foundSouls.clear()
            amountFound = 0
        }

        fun has(vec: LorenzVec): Boolean = foundSouls.contains(vec)

        fun onlyInArea(area: String): Boolean = onlyInAreas.contains(area)

        override fun toString(): String {
            return "IslandFairySoulsData(island=$island, onlyInAreas=$onlyInAreas, amountFound=$amountFound, foundSouls=$foundSouls)"
        }

    }

    val patternGroup = RepoPattern.group("misc.fairy-souls")

    /**
     * REGEX-TEST: §7Fairy Souls: §e11§7/§d11
     */
    private val loreSoulPattern by patternGroup.pattern(
        "new",
        "§7Fairy Souls: §e(?<have>.*)§7\\/§d(?<total>.*)",
    )

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "Fairy Souls Guide") return

        for (stack in event.inventoryItems.values) {
            val island = IslandType.getByNameOrNull(stack.displayName.removeColor()) ?: continue
            val amountFound = stack.getLore().firstOrNull()?.let {
                loreSoulPattern.matchMatcher(it) {
                    group("have").toIntOrNull()
                }
            } ?: continue

            println("Found $amountFound souls on island $island")
            val islandData = getIslandData(island)
            islandData.amountFound = amountFound
        }
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Fairy Souls Data")

        event.addData {
            add("Total Found Souls ${storage.sumOf { it.amountFound }}")
            add("Souls on Current Island ${currentIslandData.amountFound}")
            add("Souls on Islands:")
            for (islandData in storage) {
                add("  ${islandData.island.displayName}: ${islandData.amountFound}")
            }
        }
    }

    val currentIslandData: IslandFairySoulsData
        get() = getIslandData(SkyBlockUtils.currentIsland)

    fun getIslandData(island: IslandType): IslandFairySoulsData {
        val result = storage.firstOrNull { it.island == island }
        if (result != null) return result
        val newData = IslandFairySoulsData(island)
        storage.add(newData)
        return newData
    }
}
