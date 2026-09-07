package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object FairySoulApi {

    private val groupPattern = RepoPattern.group("misc.fairysoulquestoverlay")

    /**
     * REGEX-TEST: Fairy Souls: 1/5
     * REGEX-TEST: Fairy Souls: 11/11
     * REGEX-TEST: Fairy Souls: 0/8
     */
    val visitedPattern by groupPattern.pattern(
        "souls.islandname",
        "Fairy Souls: (?<soulsFound>[0-9]+)/(?<soulsTotal>[0-9]+)",
    )

    class remainingMapData(
        val soulsFound: Int,
        val soulsTotal: Int,
        val soulsRemaining: Int,
        val islandName: String,
        val genericName: IslandType,
    )

    fun getRemainingMutableMap(event: InventoryFullyOpenedEvent): MutableMap<Int, remainingMapData> {
        val remainingMap = mutableMapOf<Int, remainingMapData>()

        for ((slot, item) in event.inventoryItems) {
            val lore = item.getCleanLore()
            val islandName = item.cleanName
            val genericName = IslandType.getByNameOrNull(item.cleanName) ?: run {
                if (islandName == "Safari") {
                    IslandType.SAFARI
                } else if (islandName == "Miscellaneous") {
                    IslandType.NONE
                } else continue
            }

            if (islandName == "") continue

            var soulsTotal = 0
            var soulsFound = 0

            visitedPattern.firstMatcher(lore) {
                soulsFound = group("soulsFound").toInt()
                soulsTotal = group("soulsTotal").toInt()
            }

            remainingMap[slot] = remainingMapData(soulsFound, soulsTotal, soulsTotal - soulsFound, islandName, genericName)
        }

        return remainingMap
    }

    fun getRemainingMap(event: InventoryFullyOpenedEvent): Map<Int, remainingMapData> {
        return getRemainingMutableMap(event).toMap()
    }
}
