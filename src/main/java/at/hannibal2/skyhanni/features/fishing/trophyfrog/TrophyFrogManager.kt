package at.hannibal2.skyhanni.features.fishing.trophyfrog

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object TrophyFrogManager {

    private val patternGroup = RepoPattern.group("fishing.trophyfrog")

    /**
     * REGEX-TEST: Diamond ✔ (2)
     * REGEX-TEST: Bronze ✔ (615)
     */
    private val riberyRankPattern by patternGroup.pattern(
        "ribery.rank.colorless",
        "(?: +)?(?<rarity>.*) ✔ \\((?<amount>.*)\\)",
    )

    /**
     * REGEX-TEST: Diamond ✖
     * REGEX-TEST: Gold ✖
     */
    private val riberyRankEmptyPattern by patternGroup.pattern(
        "ribery.rank.empty.colorless",
        "(?: +)?(?<rarity>.*) ✖",
    )

    /**
     * REGEX-TEST: Trophy Frogs
     */
    private val riberyInventoryNamePattern by patternGroup.pattern(
        "ribery.inventory",
        "Trophy Frogs",
    )

    val riberyInventory = InventoryDetector { riberyInventoryNamePattern }

    /**
     * REGEX-TEST: How to Catch
     */
    private val howToCatchPattern by patternGroup.pattern(
        "ribery.howtocatch",
        "How to Catch",
    )

    // keyed by the clean frog name, e.g. "Common Frog"
    val frog: MutableMap<String, MutableMap<TrophyRarity, Int>>?
        get() = ProfileStorageData.profileSpecific?.lotusAtoll?.trophyFrogs

    val frogDescriptions: MutableMap<String, String>?
        get() = ProfileStorageData.profileSpecific?.lotusAtoll?.trophyFrogDescriptions

    // Frogs are real NEU items (e.g. COMMON_FROG_BRONZE), so icon/name/rarity come from the repo.
    // Bronze always exists and shares the frog's item rarity across all trophy tiers.
    fun getInternalName(rawName: String): NeuInternalName =
        "${rawName.uppercase().replace(" ", "_")}_BRONZE".toInternalName()

    fun getDisplayName(rawName: String): String =
        getInternalName(rawName).repoItemName.split(" ").dropLast(1).joinToString(" ")

    // Fetch when talking with Researcher Ribery
    @HandleEvent(onlyOnSkyblock = true)
    private fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!riberyInventoryNamePattern.matches(event.inventoryName)) return

        val savedFrogs = frog ?: return
        val savedDescriptions = frogDescriptions ?: return
        var updatedFrogs = 0

        for (stack in event.inventoryItems.values) {
            val cleanName = stack.cleanName
            val lore = stack.getCleanLore()

            fun getRarity(rawRarity: String, line: String): TrophyRarity =
                TrophyRarity.getByName(rawRarity) ?: ErrorManager.skyHanniError(
                    "unknown trophy frog rarity in Ribery inventory",
                    "rawRarity" to rawRarity,
                    "line" to line,
                    "stack.name" to cleanName,
                )

            val parsed = mutableMapOf<TrophyRarity, Int>()
            for (line in lore) {
                val (rarity, amount) = riberyRankPattern.matchMatcher(line) {
                    getRarity(group("rarity"), line) to group("amount").formatInt()
                } ?: riberyRankEmptyPattern.matchMatcher(line) {
                    getRarity(group("rarity"), line) to 0
                } ?: continue
                parsed[rarity] = amount
            }
            // not a frog item (empty slot, filler, decoration)
            if (parsed.isEmpty()) continue

            readDescription(lore)?.let { savedDescriptions[cleanName] = it }

            val counts = savedFrogs.getOrPut(cleanName) { mutableMapOf() }
            var updated = false
            for ((rarity, amount) in parsed) {
                if (counts[rarity] != amount) {
                    counts[rarity] = amount
                    updated = true
                }
            }
            if (updated) updatedFrogs++
        }

        if (updatedFrogs > 0) {
            ChatUtils.chat("Updated $updatedFrogs Trophy Frogs from Researcher Ribery.")
        }
        TrophyFrogDisplay.update()
    }

    // Grabs the "How to Catch" description: the lines after that header until the next blank line.
    private fun readDescription(lore: List<String>): String? {
        val start = lore.indexOfFirst { howToCatchPattern.matches(it.trim()) }
        if (start == -1) return null
        val lines = lore.drop(start + 1).takeWhile { it.isNotBlank() }.map { it.trim() }
        return lines.joinToString(" ").ifBlank { null }
    }
}
