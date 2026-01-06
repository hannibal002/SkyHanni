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
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.getSingleLineLore
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.NumberUtil.formatLongOrNull
import at.hannibal2.skyhanni.utils.PrimitiveItemStack
import at.hannibal2.skyhanni.utils.PrimitiveItemStack.Companion.toPrimitiveStackOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
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
    private val config get() = SkyHanniMod.feature.inventory.superCrafting.waste

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
        checkInventoryName = { name -> inventoryPattern.matches(name) },
    )

    private fun getWarnAmount(): Double {
        return if (BitsApi.hasCookieBuff()) config.normal
        else config.withoutCookieValues.normal
    }

    private fun getBulkWarnAmount(): Double {
        return if (BitsApi.hasCookieBuff()) config.maxResource
        else config.withoutCookieValues.maxResource
    }

    @HandleEvent
    fun onClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!invDetector.isInside()) return
        if (!config.enabled) return
        if (HypixelData.noTrade) return
        if (event.clickedButton != 0) return
        val slots = InventoryUtils.getItemsInOpenChestWithNull()
        val craftingAmount = getSuperCraftingCount(slots) ?: return
        val profit = getProfit(slots, craftingAmount) ?: return
        val maxCraftingAmount = getSuperCraftingMaxCount(slots)
        if (!blockWasteClick(profit, craftingAmount, maxCraftingAmount)) return
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
            config::enabled,
        )
        event.cancel()
    }

    private fun getSuperCraftingMaxCount(slots: List<Slot>) = slots[PICKAXE_SLOT].item.getLore().mapNotNull {
        craftingResourcePattern.matchMatcher(it.removeColor()) {
            groupOrNull("amount")?.formatLongOrNull()
        }
    }.minOrNull() ?: ErrorManager.skyHanniError(
        "crafting resource line not found",
        "lore" to slots.map { slot -> slot.item.getLore().map { line -> line.removeColor() } },
    )

    private fun getProfit(slots: List<Slot>, craftingAmount: Long): Double? {
        val materials = getRecipeMaterials(slots)
        val resultItem = getResultItem(slots)

        val recipeMultiplier = resultItem.amount

        val itemsPrice = materials.sumOf { material ->
            val totalAmount = material.amount * (craftingAmount / recipeMultiplier)
            BazaarApi.calculatePriceOffAvailableOrders(
                material.internalName, totalAmount, BazaarApi.SimpleTransactionType.BUY_ORDER,
            ) ?: return null
        }

        val totalResultPrice = BazaarApi.calculatePriceOffAvailableOrders(
            resultItem.internalName,
            craftingAmount,
            BazaarApi.SimpleTransactionType.SELL_OFFER,
        ) ?: return null

        return totalResultPrice - itemsPrice
    }

    private fun getRecipeMaterials(slots: List<Slot>) = materialSlots.mapNotNull { slotIndex ->
        val item = slots[slotIndex].item
        if (item.isEmpty) return@mapNotNull null
        item.toPrimitiveStackOrNull()
            ?: error("Unknown item in crafting slot: ${item.displayName}")
    }.groupBy { it.internalName }.map { (name, stacks) ->
        PrimitiveItemStack(name, stacks.sumOf { it.amount })
    }

    private fun getSuperCraftingCount(slots: List<Slot>): Long? {
        val lore = slots[PICKAXE_SLOT].item.getSingleLineLore().removeColor()
        return craftingCount.matchMatcher(lore) {
            groupOrNull("count")?.formatLongOrNull()
        }
    }

    private fun getResultItem(slots: List<Slot>): PrimitiveItemStack {
        val item = slots[RESULT_SLOT].item
        if (item.isEmpty) error("Result slot is empty")
        return item.toPrimitiveStackOrNull()
            ?: error("internal name is null: ${item.displayName}")
    }

    private fun blockWasteClick(profit: Double, craftingAmount: Long, maxCraftingAmount: Long) = when {
        KeyboardManager.isControlKeyDown() -> false
        profit < -getWarnAmount() * 1_000_000L -> true
        profit < -getBulkWarnAmount() * 1_000_000L && craftingAmount == maxCraftingAmount -> true
        else -> false
    }
}
