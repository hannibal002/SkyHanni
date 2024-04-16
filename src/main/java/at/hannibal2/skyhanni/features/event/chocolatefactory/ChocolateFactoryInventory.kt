package at.hannibal2.skyhanni.features.event.chocolatefactory

import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.RenderUtils.drawSlotText
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.StringUtils.matchFirst
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ChocolateFactoryInventory {

    private val config get() = ChocolateFactoryApi.config

    private val rabbitAmountPattern by ChocolateFactoryApi.patternGroup.pattern(
        "rabbit.amount",
        "Rabbit \\S+ - \\[(?<amount>\\d+)].*"
    )
    private val upgradeTierPattern by ChocolateFactoryApi.patternGroup.pattern(
        "upgradetier",
        ".*\\s(?<tier>[IVXLC]+)"
    )
    private val unclaimedRewardsPattern by ChocolateFactoryApi.patternGroup.pattern(
        "unclaimedrewards",
        "§7§aYou have \\d+ unclaimed rewards?!"
    )

    @SubscribeEvent
    fun onRenderItemOverlayPost(event: GuiRenderItemEvent.RenderOverlayEvent.GuiRenderItemPost) {
        if (!ChocolateFactoryApi.inChocolateFactory) return
        if (!config.highlightUpgrades) return

        val item = event.stack ?: return
        val itemName = item.name
        if (itemName != ChocolateFactoryApi.bestRabbitUpgrade) return

        event.drawSlotText(event.x + 18, event.y, "§6✦", .8f)
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!ChocolateFactoryApi.inChocolateFactory) return
        if (!config.highlightUpgrades) return

        for (slot in InventoryUtils.getItemsInOpenChest()) {
            if (slot.slotIndex in ChocolateFactoryApi.upgradeableSlots) {
                if (slot.slotIndex == ChocolateFactoryApi.bestUpgrade) {
                    slot highlight LorenzColor.GREEN.addOpacity(200)
                } else {
                    slot highlight LorenzColor.GREEN.addOpacity(75)
                }
            }
            if (slot.slotIndex == ChocolateFactoryApi.barnIndex && ChocolateFactoryBarnManager.barnFull) {
                slot highlight LorenzColor.RED
            }
            if (slot.slotIndex == ChocolateFactoryApi.clickRabbitSlot) {
                slot highlight LorenzColor.RED
            }
        }
    }

    @SubscribeEvent
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (!ChocolateFactoryApi.inChocolateFactory) return
        if (!config.showStackSizes) return

        val item = event.stack
        val itemName = item.name.removeColor()
        val slotNumber = event.slot.slotNumber

        if (slotNumber in ChocolateFactoryApi.rabbitSlots) {
            rabbitAmountPattern.matchMatcher(itemName) {
                val rabbitTip = when (val rabbitAmount = group("amount").formatInt()) {
                    in (0..9) -> "$rabbitAmount"
                    in (10..74) -> "§a$rabbitAmount"
                    in (75..124) -> "§9$rabbitAmount"
                    in (125..174) -> "§5$rabbitAmount"
                    in (175..199) -> "§6$rabbitAmount"
                    200 -> "§d$rabbitAmount"
                    else -> "§c$rabbitAmount"
                }

                event.stackTip = rabbitTip
            }
        }
        if (slotNumber in ChocolateFactoryApi.otherUpgradeSlots) {
            upgradeTierPattern.matchMatcher(itemName) {
                event.stackTip = group("tier").romanToDecimal().toString()
            }
        }
        if (slotNumber == ChocolateFactoryApi.milestoneIndex) {
            item.getLore().matchFirst(unclaimedRewardsPattern) {
                event.stackTip = "§c!!!"
            }
        }
    }

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!ChocolateFactoryApi.inChocolateFactory) return
        val slot = event.slot ?: return
        if (!config.useMiddleClick) return
        if (slot.slotNumber in ChocolateFactoryApi.noPickblockSlots) return

        event.makePickblock()
    }
}
