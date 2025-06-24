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
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
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
     * REGEX-TEST: §2§l§m                      §f§l§m   §r §e43,649§6/§e50k
     * REGEX-TEST: §7Total collected: §e277,252
     */
    private val counterPattern by patternGroup.pattern(
        "counter",
        ".* §e(?<amount>[\\d,]*)(?:§6/.*)?",
    )

    /**
     * REGEX-TEST: §7Total collected: §e261,390
     * REGEX-TEST: §7Total Collected: §e2,012,418
     */
    private val singleCounterPattern by patternGroup.pattern(
        "singlecounter",
        "§7Total [c|C]ollected: §e(?<amount>.*)",
    )

    /**
     * REGEX-TEST: §b[MVP§f+§b] oxsss§7: §e1.9M
     * REGEX-TEST: §a[VIP] oxsss§7: §e0
     * REGEX-TEST: §7oxsss§7: §e0
     */
    val playerCounterPattern by patternGroup.pattern(
        "playercounter",
        "(?:§.\\[[^]]+(?:§\\++§b)?] |§7)(?<name>[^§]{2,16})§7: §e(?<amount>.+)",
    )

    /**
     * REGEX-TEST: §7Progress to Raw Chicken IX: §e25.7§6%
     * REGEX-TEST: §7Total Collected: §e1,917,287
     */
    val collectionNotMaxedPattern by patternGroup.pattern(
        "collections.notmaxed",
        "§7(?:Progress to .+|Total Collected: .+)",
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
            val lore = stack.getLore()

            singleCounterPattern.firstMatcher(lore) {
                val counter = group("amount").formatLong()
                val internalName = stack.getInternalName().getCorrectedName()
                collectionValue[internalName] = counter
            }

            CollectionUpdateEvent.post()
        }

        if (inventoryName.endsWith("s") && inventoryName != "Boss Collections") {
            for ((_, stack) in event.inventoryItems) {
                val name = stack.displayName.removeColor()
                if ("Collections" in name) continue

                val lore = stack.getLore()
                if (lore.none { it.contains("Click to view!") }) continue

                val internalName = stack.getInternalName().getCorrectedName()

                val isCoop = playerCounterPattern.anyMatches(lore)
                val isNotMaxed = collectionNotMaxedPattern.anyMatches(lore)

                if (!isCoop || isNotMaxed) {
                    counterPattern.firstMatcher(lore) {
                        val counter = group("amount").formatLong()
                        collectionValue[internalName] = counter
                    }
                } else {
                    val coopIndex = lore.indexOf("§7Co-op Contributions:")
                    if (coopIndex == -1) continue

                    var totalCollected = 0L
                    lore.drop(coopIndex).forEach { line ->
                        if (line.isBlank()) return@forEach

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
