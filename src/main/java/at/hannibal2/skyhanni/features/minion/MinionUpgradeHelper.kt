package at.hannibal2.skyhanni.features.minion

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.GetFromSackApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SackApi.getAmountInSacksOrNull
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.MinionCloseEvent
import at.hannibal2.skyhanni.events.MinionOpenEvent
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.ItemUtils.setLoreString
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.withWrappedLines
import at.hannibal2.skyhanni.utils.compat.setCustomItemName
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.level.block.Blocks
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object MinionUpgradeHelper {
    private val config get() = SkyHanniMod.feature.misc.minions

    private var displayItem: SafeItemStack? = null
    private var itemsNeeded: Int = 0
    private var internalName: NeuInternalName? = null
    private var itemsInSacks: Int = 0

    /**
     * REGEX-TEST: You need 512 more Nether Quartz.
     * WRAPPED-REGEX-TEST: " You need 8 more Condensed Lily Pad."
     */
    private val requiredItemsPattern by RepoPattern.pattern(
        "minion.items.upgrade.colorless",
        "(?: +)?You need (?<amount>\\d+) more (?<itemName>.+)\\.",
    )

    private var lastMinionOpen = SimpleTimeMark.farPast()

    @HandleEvent
    private fun onMinionOpen(event: MinionOpenEvent) {
        if (!config.minionConfigHelper) return
        lastMinionOpen = SimpleTimeMark.now()
        val lore = event.inventoryItems[50]?.getCleanLore() ?: return
        requiredItemsPattern.firstMatcher(lore.withWrappedLines()) {
            internalName = NeuInternalName.fromItemName(group("itemName"))
            itemsNeeded = group("amount")?.toInt() ?: 0
        } ?: resetItems()

        val internalName = internalName ?: return
        if (itemsNeeded > 0) {
            itemsInSacks = internalName.getAmountInSacksOrNull() ?: 0
            displayItem = createDisplayItem(internalName)
        }
    }

    @HandleEvent
    private fun onMinionClose(event: MinionCloseEvent) {
        resetItems()
    }

    // TODO make this event not necessary here.
    @HandleEvent
    private fun onInventoryClose() {
        resetItems()
    }

    // TODO make this event not necessary here.
    @HandleEvent
    private fun onWorldChange() {
        resetItems()
    }

    // TODO make this event not necessary here.
    @HandleEvent
    private fun onInventoryFullyOpened() {
        if (lastMinionOpen.passedSince() > 2.seconds) {
            resetItems()
        }
    }

    private fun resetItems() {
        internalName = null
        itemsNeeded = 0
        itemsInSacks = 0
        displayItem = null
    }

    private fun createDisplayItem(internalName: NeuInternalName): SafeItemStack {
        val lore = createLore(internalName)
        return SafeItemStack(Blocks.DIAMOND_BLOCK).setLoreString(lore).setCustomItemName("§bGet Required Items")
    }

    private fun createLore(internalName: NeuInternalName): List<String> {
        val itemPrice = internalName.getPrice()
        val lore = buildList {
            val itemsRemaining = itemsNeeded - itemsInSacks
            val totalCost = itemsNeeded * itemPrice
            val remainingCost = itemsRemaining * itemPrice
            val itemName = internalName.repoItemName

            add("§8(From SkyHanni)")
            add("")

            if (itemsInSacks > 0) {
                add("§7In sacks: §a${itemsInSacks.addSeparators()}§7x §b$itemName")
            }

            if (itemsRemaining > 0) {
                add("§7From Bazaar: §a$itemsRemaining§7x §b$itemName")
                add("§7Cost: §6${remainingCost.shortFormat()} coins")
            } else {
                add("§7All needed items are already in sacks!")
            }

            add("")
            add("§7Total price: §6${totalCost.shortFormat()} coins")

            add("")
            add(if (itemsRemaining > 0) "§eClick to open Bazaar!" else "§eClick to retrieve items from the sacks!")
        }
        return lore
    }

    @HandleEvent
    private fun replaceItem(event: ReplaceItemEvent) {
        if (!config.minionConfigHelper) return
        if (event.inventory !is Inventory && event.slot == 51) {
            displayItem?.let { event.replace(it) }
        }
    }

    @HandleEvent(priority = HandleEvent.HIGH)
    private fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!config.minionConfigHelper || displayItem == null || event.slotId != 51) return
        event.cancel()
        val internalName = internalName ?: return
        val remainingItems = itemsNeeded - itemsInSacks
        if (remainingItems > 0) {
            BazaarApi.searchForBazaarItemOrRecipe(internalName, remainingItems)
        } else {
            GetFromSackApi.getFromSack(internalName, itemsNeeded)
        }
    }
}
