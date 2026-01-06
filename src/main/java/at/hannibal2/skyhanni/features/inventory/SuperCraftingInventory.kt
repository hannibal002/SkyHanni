package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.BitsApi
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.getSingleLineLore
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
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
    private val materialSlots = listOf(
        10, 11, 12, 19, 20, 21, 28, 29, 30,
    )
    private val pickaxeSlotIndex = 32
    private val resultSlotIndex = 25
    private val wasteConfig = SkyHanniMod.feature.inventory.superCrafting.waste

    private val craftingPatternGroup = RepoPatternGroup("supercrafting-inventory")
    private val craftingCount by craftingPatternGroup.pattern(
        "crafting.count",
        ".*Crafting (?<count>[0-9,]+) item.*",
    )
    private val craftingResourcePattern by craftingPatternGroup.pattern(
        "crafting.resource",
        " *([✔✖]) [0-9,]+/[0-9,]+ \\((?<amount>[0-9,]+)x\\) (?<resource>.+)",
    )

    private val inventoryPattern by craftingPatternGroup.pattern(
        "inventory.name",
        "(?<itemname>.*) Recipe",
    )
    private val invDetector = InventoryDetector(
        onOpenInventory = { },
        checkInventoryName = { name ->
            inventoryPattern.matches(name)
        },
        onCloseInventory = {
        },
    )

    fun getWarnAmount(): Double {
        return if (BitsApi.hasCookieBuff()) wasteConfig.normal
        else wasteConfig.withoutCookieValues.normal
    }

    fun getBulkWarnAmount(): Double {
        return if (BitsApi.hasCookieBuff()) wasteConfig.maxResource
        else wasteConfig.withoutCookieValues.maxResource
    }

    @HandleEvent
    fun onClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!invDetector.isInside()) return
        if (!wasteConfig.enabled) return
        if (HypixelData.noTrade) return
        if (event.clickedButton != 0) return
        val craftingAmount = getSuperCraftingCount() ?: return
        val maxCraftingAmount = getSuperCraftingMaxCount()
        val profit = getProfit(craftingAmount) ?: return
        if (blockWasteClick(profit, craftingAmount, maxCraftingAmount)) {
            SoundUtils.playErrorSound()
            TitleManager.sendTitle(
                "§cCraft-click Prevented (Big Loss Detected)",
                subtitleText = "§7Hold §eControl §7to bypass. You could save §c${String.format("%,.1f", -profit)}§6 Coins§7 by " +
                    "selling the required resources directly to the §6Bazaar§7 and then instant-buying the finished item.",
                duration = 2.seconds,
                location = TitleManager.TitleLocation.INVENTORY,
            )
            ChatUtils.chatAndOpenConfig(
                "Blocked a craft since instant selling the materials and instant buying the item(s) directly is " +
                    "significantly cheaper. You can hold §cControl §ewhile clicking to bypass this warning. ",
                wasteConfig::enabled,
            )
            event.cancel()
        }
    }

    private fun getSuperCraftingMaxCount(): Int {
        val slots = InventoryUtils.getItemsInOpenChestWithNull()
        val pickaxeSlot = slots[32]
        val minimum = pickaxeSlot.item.getLore().mapNotNull { loreLine ->
            val loreLine = loreLine.removeColor()
            return@mapNotNull craftingResourcePattern.matchMatcher(loreLine) {
                groupOrNull("amount")?.formatIntOrNull()
            }
        }.min()
        return minimum
    }

    fun getProfit(craftingAmount: Int): Double? {
        val materials = getRecipeMaterials() ?: return null
        val resultItem = getResultItem() ?: return null

        val recipeMultiplier = resultItem.second

        val itemsPrice = materials.mapValues {
            it.value * (craftingAmount / recipeMultiplier)
        }.mapValues {
            //The materials are always summed up already by the getRecipeMaterials function
            val key = it.key
            val price = BazaarApi.calculatePriceOffAvailableOrders(key, it.value, BazaarApi.SimpleTransactionType.BUY_ORDER)
            return@mapValues price ?: return null
        }.sumAllValues()

        val totalResultPrice = BazaarApi.calculatePriceOffAvailableOrders(
            resultItem.first,
            craftingAmount,
            BazaarApi.SimpleTransactionType.SELL_OFFER,
        ) ?: return null

        return totalResultPrice - itemsPrice
    }

    fun getRecipeMaterials(): Map<NeuInternalName, Int>? {
        val slots = InventoryUtils.getItemsInOpenChestWithNull()
        return materialSlots.map { slots[it] }.map {
            val name = it.item.getInternalNameOrNull()
            if (name != null) return@map name to it.item.count
            if (it.item.item is AirItem) return@map NeuInternalName.NONE to 0
            else return null
        }.groupBy { it.first }.mapValues { it ->
            it.value.sumOf { it.second }
        }.filter { it.key != NeuInternalName.NONE }
    }

    fun getSuperCraftingCount(): Int? {
        val slots = InventoryUtils.getItemsInOpenChestWithNull()
        val pickaxeSlot = slots[pickaxeSlotIndex]
        val lore = pickaxeSlot.item.getSingleLineLore().removeColor()
        return craftingCount.matchMatcher(lore) {
            groupOrNull("count")?.formatIntOrNull()
        }
    }

    fun getResultItem(): Pair<NeuInternalName, Int>? {
        val slots = InventoryUtils.getItemsInOpenChestWithNull()
        val resultSlot = slots[resultSlotIndex]
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
