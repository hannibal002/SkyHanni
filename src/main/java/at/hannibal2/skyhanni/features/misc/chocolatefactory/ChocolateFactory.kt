package at.hannibal2.skyhanni.features.misc.chocolatefactory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.utils.CollectionUtils.nextAfter
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.StringUtils.matchFirst
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ChocolateFactory {

    private val config get() = SkyHanniMod.feature.misc.chocolateFactory

    private val patternGroup = RepoPattern.group("misc.chocolatefactory")
    private val rabbitAmountPattern by patternGroup.pattern(
        "rabbitamount",
        "Rabbit \\S+ - \\[(?<amount>\\d+)].*"
    )
    private val unclaimedRewardsPattern by patternGroup.pattern(
        "unclaimedrewards",
        "§7§aYou have \\d+ unclaimed rewards?!"
    )
    private val chocolateAmountPattern by patternGroup.pattern(
        "chocolateamount",
        "(?<chocolate>[\\d,]+) Chocolate"
    )
    private val upgradeTierPattern by patternGroup.pattern(
        "upgradetier",
        ".*\\s(?<tier>[IVXL]+)"
    )

    private var inChocolateFactory = false
    private var currentChocolate = 0L

    private val slotsToHighlight: MutableSet<Int> = mutableSetOf()
    private val otherUpgradeSlots = setOf(
        28, 34, 39, 40, 41
    )

    // todo update slot highlight and chocolate here as well
    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "Chocolate Factory") return
        inChocolateFactory = true
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        clearData()
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        clearData()
    }

    private fun clearData() {
        inChocolateFactory = false
        slotsToHighlight.clear()
    }

    @SubscribeEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!inChocolateFactory) return

        val chocolateItem = InventoryUtils.getItemsInOpenChest().find { it.slotIndex == 13 }?.stack ?: return
        chocolateAmountPattern.matchMatcher(chocolateItem.name.removeColor()) {
            currentChocolate = group("chocolate").formatLong()
        }

        slotsToHighlight.clear()
        for ((slotIndex, item) in event.inventoryItems) {
            val nextLine = item.getLore().nextAfter({ it == "§7Cost" }) ?: continue
            chocolateAmountPattern.matchMatcher(nextLine.removeColor()) {
                val amount = group("chocolate").formatLong()
                if (amount < currentChocolate) {
                    slotsToHighlight.add(slotIndex)
                }
            }
        }
    }

    @SubscribeEvent
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (!inChocolateFactory) return
        if (!config.showStackSizes) return

        val item = event.stack
        val itemName = item.name.removeColor()
        val slotNumber = event.slot.slotNumber

        if (slotNumber in 29..33) {
            rabbitAmountPattern.matchMatcher(itemName) {
                val rabbitTip = when (val rabbitAmount = group("amount").formatInt()) {
                    in (0..9) -> "$rabbitAmount"
                    // todo confirm it changes at 74
                    in (10..74) -> "§a$rabbitAmount"
                    // todo get more data for colours
                    else -> "§c$rabbitAmount"
                }

                event.stackTip = rabbitTip
            }
        }
        if (slotNumber in otherUpgradeSlots) {
            upgradeTierPattern.matchMatcher(itemName) {
                event.stackTip = group("tier").romanToDecimal().toString()
            }
        }
        if (slotNumber == 53) {
            item.getLore().matchFirst(unclaimedRewardsPattern) {
                event.stackTip = "§c!!!"
            }
        }
    }

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!inChocolateFactory) return
        val slot = event.slot ?: return
        if (slot.slotNumber == 40) return

        event.makePickblock()
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!inChocolateFactory) return
        if (!config.highlightUpgrades) return

        for (slot in InventoryUtils.getItemsInOpenChest()) {
            if (slot.slotIndex in slotsToHighlight) {
                slot highlight LorenzColor.GREEN.addOpacity(100)
            }
        }
    }
}
