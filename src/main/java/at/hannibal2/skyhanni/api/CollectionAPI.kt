package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.events.CollectionUpdateEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.ProfileApiDataLoadedEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.features.bazaar.BazaarApi
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class CollectionAPI {
    private val counterPattern = Pattern.compile("(?:.*) §e(.*)§6\\/(?:.*)")
    private val singleCounterPattern = Pattern.compile("§7Total Collected: §e(.*)")

//    private val hypixelApiHasWrongItems = listOf(
//        "WOOL",
//        "CORRUPTED_FRAGMENT",
//        "EGG",
//        "POISONOUS_POTATO",
//        "REDSTONE_BLOCK",
//        "MUSHROOM_COLLECTION",
//        "RAW_SOULFLOW",
//        "GEMSTONE_COLLECTION",
//    )

    @SubscribeEvent
    fun onProfileDataLoad(event: ProfileApiDataLoadedEvent) {
        val profileData = event.profileData
        val jsonElement = profileData["collection"] ?: return
        val asJsonObject = jsonElement.asJsonObject ?: return
        for ((hypixelId, rawCounter) in asJsonObject.entrySet()) {
            val counter = rawCounter.asLong
            val neuItemId = NEUItems.transHypixelNameToInternalName(hypixelId)
            val itemName = BazaarApi.getBazaarDataByInternalName(neuItemId)?.displayName
            // Hypixel moment
//            if (hypixelApiHasWrongItems.contains(neuItemId)) continue

            if (itemName == null) {
//                LorenzUtils.debug("collection name is null for '$neuItemId'")
                continue
            }
            collectionValue[neuItemId] = counter
        }

        CollectionUpdateEvent().postAndCatch()
    }

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        collectionValue.clear()
    }

    @SubscribeEvent
    fun onTick(event: InventoryOpenEvent) {
        val inventoryName = event.inventoryName
        if (inventoryName.endsWith(" Collection")) {
            val stack = event.inventoryItems[4] ?: return
            for (line in stack.getLore()) {
                val matcher = singleCounterPattern.matcher(line)
                if (matcher.matches()) {
                    val counter = matcher.group(1).replace(",", "").toLong()
                    val name = inventoryName.split(" ").dropLast(1).joinToString(" ")
                    collectionValue[name] = counter
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

                for (line in lore) {
                    val matcher = counterPattern.matcher(line)
                    if (matcher.matches()) {
                        val counter = matcher.group(1).replace(",", "").toLong()
                        collectionValue[name] = counter
                    }
                }
            }
            CollectionUpdateEvent().postAndCatch()
        }
    }

    companion object {
        private val collectionValue = mutableMapOf<String, Long>()
        private val collectionTier0Pattern = Pattern.compile("§7Progress to .* I: .*")

        fun isCollectionTier0(lore: List<String>) = lore.map { collectionTier0Pattern.matcher(it) }.any { it.matches() }

        fun getCollectionCounter(searchName: String): Pair<String, Long>? {
            for ((collectionName, counter) in collectionValue) {
                if (collectionName.equals(searchName, true)) {
                    return Pair(collectionName, counter)
                }
            }
            return null
        }

        // TODO add support for replenish (higher collection than actual items in inv)
        fun addFromInventory(internalName: String, amount: Int) {
            val stack = NEUItems.getItemStackOrNull(internalName)
            if (stack == null) {
                LorenzUtils.debug("CollectionAPI.addFromInventory: internalName is null for '$internalName'")
                return
            }

            val name = stack.name!!.removeColor()
            val oldValue = collectionValue[name] ?: return

            val newValue = oldValue + amount
            collectionValue[name] = newValue
        }
    }
}