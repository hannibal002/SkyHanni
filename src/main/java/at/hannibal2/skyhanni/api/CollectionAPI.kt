package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.events.CollectionUpdateEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addOrPut
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NEUItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CollectionAPI {
    private val counterPattern = "(?:.*) §e(?<amount>.*)§6\\/(?:.*)".toPattern()
    private val singleCounterPattern = "§7Total Collected: §e(?<amount>.*)".toPattern()

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        collectionValue.clear()
    }

    @SubscribeEvent
    fun onTick(event: InventoryFullyOpenedEvent) {
        val inventoryName = event.inventoryName
        if (inventoryName.endsWith(" Collection")) {
            val stack = event.inventoryItems[4] ?: return
            loop@ for (line in stack.getLore()) {
                singleCounterPattern.matchMatcher(line) {
                    val counter = group("amount").replace(",", "").toLong()
                    val name = inventoryName.split(" ").dropLast(1).joinToString(" ")
                    val internalName = NEUItems.getInternalNameOrNull(name) ?: continue@loop
                    collectionValue[internalName] = counter
                }
            }
            CollectionUpdateEvent().postAndCatch()
        }

        if (inventoryName.endsWith(" Collections")) {
            if (inventoryName == "Boss Collections") return

            for ((_, stack) in event.inventoryItems) {
                var name = stack.name?.removeColor() ?: continue
                if (name.contains("Collections")) continue

                val lore = stack.getLore()
                if (!lore.any { it.contains("Click to view!") }) continue

                if (!isCollectionTier0(lore)) {
                    name = name.split(" ").dropLast(1).joinToString(" ")
                }

                loop@ for (line in lore) {
                    counterPattern.matchMatcher(line) {
                        val counter = group("amount").replace(",", "").toLong()
                        val internalName = NEUItems.getInternalNameOrNull(name) ?: continue@loop
                        collectionValue[internalName] = counter
                    }
                }
            }
            CollectionUpdateEvent().postAndCatch()
        }
    }

    companion object {
        val collectionValue = mutableMapOf<NEUInternalName, Long>()
        private val collectionTier0Pattern = "§7Progress to .* I: .*".toPattern()

        fun isCollectionTier0(lore: List<String>) = lore.map { collectionTier0Pattern.matcher(it) }.any { it.matches() }

        fun getCollectionCounter(internalName: NEUInternalName): Long? = collectionValue[internalName]

        // TODO add support for replenish (higher collection than actual items in inv)
        fun addFromInventory(internalName: NEUInternalName, amount: Int) {
            if (internalName.getItemStackOrNull() == null) {
                LorenzUtils.debug("CollectionAPI.addFromInventory: item is null for '$internalName'")
                return
            }
            collectionValue.addOrPut(internalName, amount.toLong())
        }
    }
}