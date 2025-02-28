package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.CollectionUpdateEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.CollectionUtils.put
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object CollectionApi {
    private val patternGroup = RepoPattern.group("data.collection.api")

    /**
     * REGEX-TEST: §2§l§m                      §f§l§m   §r §e43,649§6/§e50k
     */
    private val counterPattern by patternGroup.pattern(
        "counter",
        ".* §e(?<amount>.*)§6/.*",
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
     * REGEX-TEST: §7Progress to Nether Wart I: §e46§6%
     */
    private val collectionTier0Pattern by patternGroup.pattern(
        "tierzero",
        "§7Progress to .* I: .*",
    )

    val collectionValue get() = ProfileStorageData.profileSpecific?.collection

    private val YOUNGITE = "YOUNGITE".toInternalName()
    private val OBSOLITE = "OBSOLITE".toInternalName()
    private val TIMITE = "TIMITE".toInternalName()

    // TODO repo
    private val incorrectCollectionNames = mapOf(
        "Mushroom" to "RED_MUSHROOM".toInternalName(),
    )

    @HandleEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        collectionValue?.clear()
    }

    @HandleEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        val inventoryName = event.inventoryName
        if (inventoryName.endsWith(" Collection")) {
            val stack = event.inventoryItems[4] ?: return
            singleCounterPattern.firstMatcher(stack.getLore()) {
                val counter = group("amount").formatLong()
                val name = inventoryName.split(" ").dropLast(1).joinToString(" ")
                val internalName = incorrectCollectionNames[name] ?: NeuInternalName.fromItemName(name)
                collectionValue?.set(internalName, counter)
            }
            CollectionUpdateEvent.post()
        }

        if (inventoryName.endsWith(" Collections")) {
            // TODO add Boss Collection support
            if (inventoryName == "Boss Collections") return

            for ((_, stack) in event.inventoryItems) {
                var name = stack.name.removeColor()
                if (name.contains("Collections")) continue

                val lore = stack.getLore()
                if (!lore.any { it.contains("Click to view!") }) continue

                if (!isCollectionTier0(lore)) {
                    name = name.split(" ").dropLast(1).joinToString(" ")
                }

                val internalName = incorrectCollectionNames[name] ?: NeuInternalName.fromItemName(name)
                counterPattern.firstMatcher(lore) {
                    val counter = group("amount").formatLong()
                    collectionValue?.set(internalName, counter)
                }
            }
            CollectionUpdateEvent.post()
        }
    }

    @HandleEvent
    fun onItemAdd(event: ItemAddEvent) {
        if (event.source == ItemAddManager.Source.COMMAND) return
        val internalName = event.internalName
        val primitive = NeuItems.getPrimitiveMultiplier(internalName)

        val collectionName = when (primitive.internalName) {
            else -> primitive.internalName
        }

        // TODO add support for replenish (higher collection than actual items in inv)
        if (collectionName.getItemStackOrNull() == null) {
            ChatUtils.debug("CollectionAPI.addFromInventory: item is null for '$collectionName' from '$internalName'")
            return
        }
        collectionValue?.addOrPut(collectionName, event.amount.toLong() * primitive.amount)
    }

    fun isCollectionTier0(lore: List<String>) = lore.any { collectionTier0Pattern.matches(it) }
    fun getCollectionCounter(internalName: NeuInternalName): Long? =
        collectionValue?.get(NeuItems.getPrimitiveMultiplier(internalName).internalName)

    fun NeuInternalName.getMultipleMap() = findAllMultiples()[this] ?: mapOf(this to 1)

    fun findAllMultiples(): Map<NeuInternalName, MutableMap<NeuInternalName, Int>> {
        val entries = mutableMapOf<NeuInternalName, MutableMap<NeuInternalName, Int>>()
        NeuItems.allInternalNames.values.filter {
            it.getItemStackOrNull()?.getItemCategoryOrNull()?.let {
                ItemCategory.nonGear.contains(it)
            } == true
        }.map {
            it!! to NeuItems.getPrimitiveMultiplier(it)
        }.forEach {
            entries.compute(it.second.internalName) { _, v ->
                val pair = it.first to it.second.amount
                if (v == null) {
                    mutableMapOf(pair)
                } else {
                    v.put(pair)
                    v
                }
            }
        }
        return entries.filter { it.value.size > 1 }
    }
}
