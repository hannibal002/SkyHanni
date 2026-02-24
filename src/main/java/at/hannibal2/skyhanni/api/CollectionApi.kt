package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.events.CollectionUpdateEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object CollectionApi {
    private val patternGroup = RepoPattern.group("data.collection.api")

    /**
     * REGEX-TEST: Farming Collections
     * REGEX-TEST: Carrot Collection
     */
    private val collectionInventoryPattern by patternGroup.pattern(
        "collections",
        ".+ Collections?",
    )

    /**
     * REGEX-TEST:                           43,649/50k
     * REGEX-TEST: Total collected: 277,252
     */
    private val counterPattern by patternGroup.pattern(
        "counter.new",
        ".* (?<amount>[\\d,]*)(?:/.*)?",
    )

    /**
     * REGEX-TEST: Total collected: 261,390
     * REGEX-TEST: Total Collected: 2,012,418
     */
    private val singleCounterPattern by patternGroup.pattern(
        "singlecounter.new",
        "Total [c|C]ollected: (?<amount>.*)",
    )

    /**
     * REGEX-TEST: [MVP+] oxsss: 1.9M
     * REGEX-TEST: [VIP] oxsss: 0
     * REGEX-TEST: oxsss: 0
     */
    val playerCounterPattern by patternGroup.pattern(
        "playercounter.new",
        "(?:\\[[^]]+(?:\\+)?] |)(?<name>[^§]{2,16}): (?<amount>.+)",
    )

    /**
     * REGEX-TEST: Progress to Raw Chicken IX: 25.7§6%
     * REGEX-TEST: Total Collected: 1,917,287
     */
    val collectionNotMaxedPattern by patternGroup.pattern(
        "collections.notmaxed.new",
        "Progress to .+|Total Collected: .+",
    )

    /**
     * REGEX-TEST: §7Progress to Nether Wart I: §e46§6%
     */
    private val collectionTier0Pattern by patternGroup.pattern(
        "tierzero",
        "§7Progress to .* I: .*",
    )

    val collectionInventory = InventoryDetector { name -> collectionInventoryPattern.matches(name) }
    val collectionValue = mutableMapOf<NeuInternalName, Long>()

    // TODO repo
    private val incorrectCollectionNames = mapOf(
        "MUSHROOM_COLLECTION".toInternalName() to "RED_MUSHROOM".toInternalName(),
    )

    @HandleEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        collectionValue.clear()
    }

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        val inventoryName = event.inventoryName
        if (!collectionInventory.isInside()) return

        if (inventoryName.endsWith("n")) {
            val stack = event.inventoryItems[4] ?: return
            val lore = stack.getLoreComponent()

            singleCounterPattern.firstMatcher(lore.map { it.string }) {
                val counter = group("amount").formatLong()
                val internalName = stack.getInternalName().getCorrectedName()
                collectionValue[internalName] = counter
            }

            CollectionUpdateEvent.post()
        }

        if (inventoryName.endsWith("s") && inventoryName != "Boss Collections") {
            for ((_, stack) in event.inventoryItems) {
                val name = stack.cleanName()
                if ("Collections" in name) continue

                val lore = stack.getLoreComponent()
                if (lore.none { it.string.contains("Click to view!") }) continue

                val internalName = stack.getInternalName().getCorrectedName()

                val isCoop = playerCounterPattern.anyMatches(lore.map { it.string })
                val isNotMaxed = collectionNotMaxedPattern.anyMatches(lore.map { it.string })

                if (!isCoop || isNotMaxed) {
                    counterPattern.firstMatcher(lore.map { it.string }) {
                        val counter = group("amount").formatLong()
                        collectionValue[internalName] = counter
                    }
                } else {
                    val coopIndex = lore.indexOfFirst { it.string == "Co-op Contributions:" }
                    if (coopIndex == -1) continue

                    var totalCollected = 0L
                    lore.drop(coopIndex).forEach { line ->
                        if (line.string.isBlank()) return@forEach

                        playerCounterPattern.matchMatcher(line) {
                            totalCollected += group("amount").formatLong()
                        }
                    }

                    collectionValue[internalName] = totalCollected
                }
            }
            CollectionUpdateEvent.post()
        }
    }

    @HandleEvent
    fun onItemAdd(event: ItemAddEvent) {
        if (event.source == ItemAddManager.Source.COMMAND) return
        val internalName = event.internalName
        val amount = NeuItems.getPrimitiveMultiplier(internalName).amount
        if (amount > 1) return

        // TODO add support for replenish (higher collection than actual items in inv)
        if (internalName.getItemStackOrNull() == null) {
            ChatUtils.debug("CollectionAPI.addFromInventory: item is null for '$internalName'")
            return
        }
        collectionValue.addOrPut(internalName, event.amount.toLong())
    }

    fun isCollectionTier0(lore: List<String>) = lore.any { collectionTier0Pattern.matches(it) }
    fun NeuInternalName.getCorrectedName() = incorrectCollectionNames.getOrElse(this) { this }
    fun getCollectionCounter(internalName: NeuInternalName): Long? = collectionValue[internalName]
}
