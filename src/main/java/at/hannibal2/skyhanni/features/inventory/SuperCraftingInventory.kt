package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.BitsApi
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemPriceSource
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getTotalPriceForCount
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.getSingleLineLore
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.repopatterns.RepoPatternGroup
import net.minecraft.world.item.AirItem
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SuperCraftingInventory {
    val config = SkyHanniMod.feature.inventory.superCraftingCoinWaste

    val craftingPatternGroup = RepoPatternGroup("supercraftinginventory")
    val craftingCount by craftingPatternGroup.pattern(
        "crafting.count",
        ".*Crafting (?<count>[0-9,]+) item.*",
    )
    val craftingResourcePattern by craftingPatternGroup.pattern(
        "crafting.resource",
        " *([✔✖]) [0-9,]+/[0-9,]+ \\((?<amount>[0-9,]+)x\\) (?<resource>.+)",
    )

    val inventoryPattern by craftingPatternGroup.pattern(
        "inventory.name",
        "(?<itemname>.*) Recipe",
    )
    val invDetector = InventoryDetector(
        onOpenInventory = { },
        checkInventoryName = { name ->
            inventoryPattern.matches(name)
        },
        onCloseInventory = {
        },
    )

    fun getWarnAmount(): Double {
        return if (BitsApi.hasCookieBuff()) config.warnCoinWasteWithCookie
        else config.withoutCookie.warnCoinWaste
    }

    fun getBulkWarnAmount(): Double {
        return if (BitsApi.hasCookieBuff()) config.warnCoinWasteMaxResourcesWithCookie
        else config.withoutCookie.warnCoinWasteMaxResources
    }

    @HandleEvent
    fun onClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!invDetector.isInside()) return
        if (!config.warnCoinWasteEnabled) return
        if (HypixelData.noTrade) return
        val craftingAmount = getSuperCraftingCount() ?: return
        val maxCraftingAmount = getSuperCraftingMaxCount() ?: return
        val profit = getProfit(craftingAmount, maxCraftingAmount) ?: return
        if (event.clickedButton != 0) return
        if (blockWasteClick(profit, craftingAmount, maxCraftingAmount)) {
            SoundUtils.playErrorSound()
            TitleManager.sendTitle(
                "§cCraft-click Prevented (Big Loss Detected)",
                subtitleText = "§7Hold §eControl §7to bypass. You could save §c${String.format("%,.1f", -profit)}§6 Coins§7 by " +
                    "selling the required resources directly to the §6Bazaar§7 and then instant-buying the finished item.",
                duration = 2.seconds,
                location = TitleManager.TitleLocation.INVENTORY,
            )
            event.cancel()
        }
    }

    private fun getSuperCraftingMaxCount(): Int {
        val slots = InventoryUtils.getItemsInOpenChestWithNull()
        val pickaxeSlot = slots.get(32)
        val minimum = pickaxeSlot.item.getLore().mapNotNull {
            val it = it.removeColor()
            return@mapNotNull craftingResourcePattern.matchMatcher(it) {
                groupOrNull("amount")?.replace(",", "")?.toIntOrNull()
            }
        }.min()
        return minimum
    }

    fun getProfit(craftingAmount: Int, maxCraftingAmount: Int): Double? {
        val materials = getRecipeMaterials()
        if (materials.containsKey(null)) return null
        val resultItem = getResultItem() ?: return null

        val recipeMultiplier = resultItem.second ?: return null

        val itemsPrice = materials.mapValues {
            it.value * (craftingAmount / recipeMultiplier)
        }.mapValues {
            //The materials are always summed up already by the getRecipeMaterials function
            val key = it.key!!
            return key.getTotalPriceForCount(it.value, ItemPriceSource.BAZAAR_INSTANT_SELL)
        }.sumAllValues()

        val totalResultPrice = resultItem.first.getTotalPriceForCount(craftingAmount, ItemPriceSource.BAZAAR_INSTANT_BUY) ?: return null

        return totalResultPrice - itemsPrice
    }

    fun getRecipeMaterials(): Map<NeuInternalName?, Int> {
        val slots = InventoryUtils.getItemsInOpenChestWithNull()
        return listOf(
            slots.get(10), slots.get(11), slots.get(12),
            slots.get(19), slots.get(20), slots.get(21),
            slots.get(28), slots.get(29), slots.get(30),
        ).map {
            val name = it.item.getInternalNameOrNull()
            if (name != null) return@map name to it.item.count
            if (it.item.item is AirItem) return@map NeuInternalName.NONE to 0
            else return@map null to it.item.count
        }.groupBy { it.first }.mapValues {
            it.value.sumOf {
                it.second
            }
        }.filter { it.key != NeuInternalName.NONE }
    }

    fun getSuperCraftingCount(): Int? {
        val slots = InventoryUtils.getItemsInOpenChestWithNull()
        val pickaxeSlot = slots.get(32)
        val lore = pickaxeSlot.item.getSingleLineLore().removeColor()
        val craftingCount = craftingCount.matchMatcher(lore) {
            groupOrNull("count")?.replace(",", "")?.toIntOrNull()
        }
        return craftingCount
    }

    fun getResultItem(): Pair<NeuInternalName, Int>? {
        val slots = InventoryUtils.getItemsInOpenChestWithNull()
        val resultSlot = slots.get(25)
        return resultSlot.item.getInternalNameOrNull().let {
            if (resultSlot.item.item is AirItem) return null
            (it ?: NeuInternalName.NONE) to resultSlot.item.count
        }
    }

    private fun blockWasteClick(profit: Double, craftingAmount: Int, maxCraftingAmount: Int): Boolean {
        if (KeyboardManager.isControlKeyDown()) return false
        if (profit < -getWarnAmount() * 1_000_000L) return true
        if (profit < -getBulkWarnAmount() * 1_000_000L && craftingAmount == maxCraftingAmount) return true
        return false
    }
}
