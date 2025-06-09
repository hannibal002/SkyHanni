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

@SkyHanniModule
object FairySoulsApi {
    private val foundSouls get() = ProfileStorageData.profileSpecific?.fairySouls?.found ?: mutableMapOf()
    private val totalFound get() = ProfileStorageData.profileSpecific?.fairySouls?.totalFound ?: mutableMapOf()

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
            totalFound[island] = amountFound
        }
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Fairy Souls Data")

        event.addData {
            add("Total Found Souls ${totalFound.values.sum()}")
            add("Souls on Current Island ${amountFoundOnCurrentIsland()}")
            add("Souls on Islands:")
            for ((island, amount) in totalFound) {
                add("  $island: $amount")
            }
        }
    }

    fun resetFoundOnCurrentIsland() = resetFoundOnIsland(SkyBlockUtils.currentIsland)
    fun resetFoundOnIsland(island: IslandType) {
        totalFound[island] = 0
        foundSouls[island]?.clear()
    }

    fun amountFoundOnCurrentIsland(): Int = amountFoundOnIsland(SkyBlockUtils.currentIsland)
    fun amountFoundOnIsland(island: IslandType): Int = totalFound.getOrDefault(island, 0)

    fun foundSoulsOnCurrentIsland(): MutableSet<LorenzVec> = foundSoulsOnIsland(SkyBlockUtils.currentIsland)
    fun foundSoulsOnIsland(island: IslandType): MutableSet<LorenzVec> = foundSouls.getOrPut(island) { mutableSetOf() }
}
