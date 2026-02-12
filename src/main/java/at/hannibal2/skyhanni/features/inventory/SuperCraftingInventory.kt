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

    /**
     * REGEX-TEST: Crafting 1,111 items into your sacks!
     */
    private val craftingCount by craftingPatternGroup.pattern(
        "crafting.count",
        ".*Crafting (?<count>[0-9,]+) item.*",
    )

    /**
     * REGEX-TEST: ✔ 177,889/32 (5,559x) Enchanted Glowstone Dust
     * REGEX-TEST: ✔ 2,067/320 Enchanted Cocoa Beans
     * REGEX-TEST: ✔ 1,747/1,600 Enchanted Cocoa Beans
     */
    private val craftingResourcePattern by craftingPatternGroup.pattern(
        "crafting.resource",
        " *✔ (?<owned>[0-9,]+)/(?<used>[0-9,]+) (?:\\([0-9,]+x\\) )?(?<resource>.+)",
    )

    /**
     * REGEX-TEST: Enchanted Redstone Recipe
     */
    private val inventoryPattern by craftingPatternGroup.pattern(
        "inventory.name",
        "(?<itemname>.*) Recipe",
    )
    private val invDetector = InventoryDetector(
        checkInventoryName = { name -> inventoryPattern.matches(name) },
    )

    private fun getWarnAmount() = if (BitsApi.hasCookieBuff()) {
        config.threshold
    } else {
        config.withoutCookie.threshold
    } * 1_000_000L

    private fun getBulkWarnAmount() = if (BitsApi.hasCookieBuff()) {
        config.bulkThreshold
    } else {
        config.withoutCookie.bulkThreshold
    } * 1_000_000L

    @HandleEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!invDetector.isInside()) return
        if (!config.enabled) return
        if (HypixelData.noTrade) return
        if (event.clickedButton != 0) return
        if (event.slotId != PICKAXE_SLOT) return
        val slots = InventoryUtils.getItemsInOpenChestWithNull()
        val craftingAmount = getSuperCraftingCount(slots) ?: return
        val profit = getProfit(slots, craftingAmount) ?: return
        val maxCraftingAmount = getSuperCraftingMaxCount(slots, craftingAmount)
        if (!blockWasteClick(profit, craftingAmount, maxCraftingAmount)) return
        SoundUtils.playErrorSound()
        val diff = (-profit).formatChatCoins()
        TitleManager.sendTitle(
            "§cSuper Crafting Blocked (Potential Loss)",
            subtitleText = "§7Hold §e${KeyboardManager.getModifierKeyName()} §7to bypass. Potential loss: §c$diff",
            duration = 2.seconds,
            location = TitleManager.TitleLocation.INVENTORY,
        )
        ChatUtils.chatAndOpenConfig(
            "Super Craft Blocked: Instant selling the materials and instant buying the item(s) directly is " +
                "significantly cheaper (§c$diff§e)",
            config::enabled,
        )
        event.cancel()
    }

    private fun getSuperCraftingMaxCount(slots: List<Slot>, craftingAmount: Long) = slots[PICKAXE_SLOT].item.getLore()
        .mapNotNull { calculateMaxPossible(it, craftingAmount) }
        .minOrNull() ?: ErrorManager.skyHanniError(
        "Super Crafting resource line not found",
        "lore" to slots.map { slot -> slot.item.getLore().map { line -> line.removeColor() } },
    )

    private fun calculateMaxPossible(string: String, craftingAmount: Long) = craftingResourcePattern.matchMatcher(string.removeColor()) {
        val owned = groupOrNull("owned")?.formatLongOrNull() ?: return null
        val used = groupOrNull("used")?.formatLongOrNull() ?: return null
        if (used == 0L || owned == 0L) return null
        val matsPerCraft = used / craftingAmount
        if (matsPerCraft == 0L) return null
        owned / matsPerCraft
    }

    private fun getProfit(slots: List<Slot>, craftingAmount: Long): Double? {
        val materials = getRecipeMaterials(slots)
        val resultItem = getResultItem(slots)

        val recipeMultiplier = resultItem.amount
        if (recipeMultiplier == 0) ErrorManager.skyHanniError(
            "Result item amount is 0",
            "item" to resultItem,
        )

        val itemsPrice = materials.sumOf { material ->
            val totalAmount = material.amount * (craftingAmount / recipeMultiplier)
            BazaarApi.calculatePriceOfAvailableOrders(
                material.internalName, totalAmount, BazaarApi.SimpleTransactionType.BUY_ORDER,
            ) ?: return null
        }

        val totalResultPrice = BazaarApi.calculatePriceOfAvailableOrders(
            resultItem.internalName,
            craftingAmount,
            BazaarApi.SimpleTransactionType.SELL_OFFER,
        ) ?: return null

        return totalResultPrice - itemsPrice
    }

    private fun getRecipeMaterials(slots: List<Slot>) = materialSlots.mapNotNull { slotIndex ->
        val item = slots[slotIndex].item
        if (item.isEmpty) return@mapNotNull null
        item.toPrimitiveStackOrNull() ?: ErrorManager.skyHanniError(
            "Could not resolve internal name",
            "item" to item,
        )
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
        if (item.isEmpty) ErrorManager.skyHanniError("Result slot is empty")
        return item.toPrimitiveStackOrNull()
            ?: ErrorManager.skyHanniError(
                "Unknown item in result slot",
                "item" to item,
            )
    }

    private fun blockWasteClick(profit: Double, craftingAmount: Long, maxCraftingAmount: Long) = when {
        KeyboardManager.isModifierKeyDown() -> false
        profit < -getWarnAmount() -> true
        profit < -getBulkWarnAmount() && craftingAmount == maxCraftingAmount -> true
        else -> false
    }
}
