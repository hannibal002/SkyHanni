package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object FairySoulApi {

    private val patternGroup = RepoPattern.group("misc.fairysouls.questmenu")

    /**
     * REGEX-TEST: Fairy Souls: 1/5
     * REGEX-TEST: Fairy Souls: 11/11
     * REGEX-TEST: Fairy Souls: 0/8
     */
    private val soulsIslandNamePattern by patternGroup.pattern(
        "inventory.island-souls.colorless",
        "Fairy Souls: (?<soulsFound>[0-9]+)/(?<soulsTotal>[0-9]+)",
    )

    class IslandSoulInfo(
        val soulsFound: Int,
        val soulsTotal: Int,
        val soulsRemaining: Int,
        val islandName: String,
        val islandType: IslandType,
    )

    fun getIslandSoulInfo(event: Map<Int, SafeItemStack>): Map<Int, IslandSoulInfo> {
        val remainingMap = mutableMapOf<Int, IslandSoulInfo>()

        for ((slot, item) in event) {
            val lore = item.getCleanLore()
            val islandName = item.cleanName
            val islandType = IslandType.getByNameOrNull(item.cleanName) ?: run {
                if (islandName == "Safari") {
                    IslandType.SAFARI
                } else if (islandName == "Miscellaneous") {
                    IslandType.NONE
                } else continue
            }

            if (islandName == "") continue

            var soulsTotal = 0
            var soulsFound = 0

            soulsIslandNamePattern.firstMatcher(lore) {
                soulsFound = group("soulsFound").toInt()
                soulsTotal = group("soulsTotal").toInt()
            }

            remainingMap[slot] = IslandSoulInfo(soulsFound, soulsTotal, soulsTotal - soulsFound, islandName, islandType)
        }

        return remainingMap.toMap()
    }
}
