package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.BitsApi
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.features.chat.ShortenCoins.formatChatCoins
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
import at.hannibal2.skyhanni.utils.NeuItemStackProvider
import at.hannibal2.skyhanni.utils.NumberUtil.formatLongOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.repopatterns.RepoPatternGroup
import net.minecraft.world.inventory.Slot
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SuperCraftingInventory {
    private val materialSlots = listOf(
        10, 11, 12, 19, 20, 21, 28, 29, 30,
    )
    private const val PICKAXE_SLOT = 32
    private const val RESULT_SLOT = 25
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

    private fun getWarnAmount(): Double {
        return if (BitsApi.hasCookieBuff()) wasteConfig.normal
        else wasteConfig.withoutCookieValues.normal
    }

    private fun getBulkWarnAmount(): Double {
        return if (BitsApi.hasCookieBuff()) wasteConfig.maxResource
        else wasteConfig.withoutCookieValues.maxResource
    }

    @HandleEvent
    fun onClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!invDetector.isInside()) return
        if (!wasteConfig.enabled) return
        if (HypixelData.noTrade) return
        if (event.clickedButton != 0) return
        val slots = InventoryUtils.getItemsInOpenChestWithNull()
        val craftingAmount = getSuperCraftingCount(slots) ?: return
        val maxCraftingAmount = getSuperCraftingMaxCount(slots)
        val profit = getProfit(slots, craftingAmount) ?: return
        if (blockWasteClick(profit, craftingAmount, maxCraftingAmount)) {
            SoundUtils.playErrorSound()
            TitleManager.sendTitle(
                "§cCraft-click Prevented (Big Loss Detected)",
                subtitleText = "§7Hold §eControl §7to bypass. You could save §c${(-profit).formatChatCoins()} Coins §7by " +
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

    private fun getSuperCraftingMaxCount(slots: List<Slot>): Long {
        val pickaxeSlot = slots[32]
        val minimum = pickaxeSlot.item.getLore().mapNotNull {
            val loreLine = it.removeColor()
            return@mapNotNull craftingResourcePattern.matchMatcher(loreLine) {
                groupOrNull("amount")?.formatLongOrNull()
            }
        }.min()
        return minimum
    }

    private fun getProfit(slots: List<Slot>, craftingAmount: Long): Double? {
        val materials = getRecipeMaterials(slots)
        val resultItem = getResultItem(slots)

        val recipeMultiplier = resultItem.second

        val itemsPrice = materials.mapValues {
            it.value * (craftingAmount / recipeMultiplier)
        }.mapValues {
            // The materials are always summed up already by the getRecipeMaterials function
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

    private fun getRecipeMaterials(slots: List<Slot>) = materialSlots.mapNotNull { slotIndex ->
        val item = slots[slotIndex].item
        if (item.isEmpty) return@mapNotNull null
        val name = item.getInternalNameOrNull()
            ?: error("Unknown item in crafting slot: ${item.displayName}")
        name to item.count
    }.groupBy { it.first }.mapValues { entry ->
        entry.value.sumOf { it.second }
    }

    private fun getSuperCraftingCount(slots: List<Slot>): Long? {
        val lore = slots[PICKAXE_SLOT].item.getSingleLineLore().removeColor()
        return craftingCount.matchMatcher(lore) {
            groupOrNull("count")?.formatLongOrNull()
        }
    }

    private fun getResultItem(slots: List<Slot>): Pair<NeuInternalName, Int> {
        val item = slots[RESULT_SLOT].item
        if (item.isEmpty) error("Result slot is empty")
        val name = item.getInternalNameOrNull() ?: error("internal name is null: ${item.displayName}")
        return name to item.count
    }

    private fun blockWasteClick(profit: Double, craftingAmount: Long, maxCraftingAmount: Long): Boolean {
        if (KeyboardManager.isControlKeyDown()) return false
        if (profit < -getWarnAmount() * 1_000_000L) return true
        if (profit < -getBulkWarnAmount() * 1_000_000L && craftingAmount == maxCraftingAmount) return true
        return false
    }
}
