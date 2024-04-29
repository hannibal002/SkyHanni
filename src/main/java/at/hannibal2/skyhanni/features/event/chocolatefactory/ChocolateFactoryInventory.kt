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

    private val config get() = ChocolateFactoryAPI.config
    private val profileStorage get() = ChocolateFactoryAPI.profileStorage

    private val rabbitAmountPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "rabbit.amount",
        "Rabbit \\S+ - \\[(?<amount>\\d+)].*"
    )
    private val upgradeTierPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "upgradetier",
        ".*\\s(?<tier>[IVXLC]+)"
    )
    private val unclaimedRewardsPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "unclaimedrewards",
        "§7§aYou have \\d+ unclaimed rewards?!"
    )

    @SubscribeEvent
    fun onRenderItemOverlayPost(event: GuiRenderItemEvent.RenderOverlayEvent.GuiRenderItemPost) {
        if (!ChocolateFactoryAPI.inChocolateFactory) return
        if (!config.highlightUpgrades) return

        val item = event.stack ?: return
        val itemName = item.name
        if (itemName != ChocolateFactoryAPI.bestRabbitUpgrade) return

        event.drawSlotText(event.x + 18, event.y, "§6✦", .8f)
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!ChocolateFactoryAPI.inChocolateFactory) return
        if (!config.highlightUpgrades) return

        for (slot in InventoryUtils.getItemsInOpenChest()) {
            if (slot.slotIndex in ChocolateFactoryAPI.upgradeableSlots) {
                if (slot.slotIndex == ChocolateFactoryAPI.bestUpgrade) {
                    slot highlight LorenzColor.GREEN.addOpacity(200)
                } else {
                    slot highlight LorenzColor.GREEN.addOpacity(75)
                }
            }
            if (slot.slotIndex == ChocolateFactoryAPI.barnIndex && ChocolateFactoryBarnManager.barnFull) {
                slot highlight LorenzColor.RED
            }
            if (slot.slotIndex == ChocolateFactoryAPI.clickRabbitSlot) {
                slot highlight LorenzColor.RED
            }
            if (slot.slotIndex == ChocolateFactoryAPI.milestoneIndex) {
                slot.stack?.getLore()?.matchFirst(unclaimedRewardsPattern) {
                    slot highlight LorenzColor.RED
                }
            }
            if (slot.slotIndex == ChocolateFactoryAPI.timeTowerIndex) {
                if (ChocolateFactoryTimeTowerManager.timeTowerActive()) {
                    slot highlight LorenzColor.LIGHT_PURPLE
                }
                if (ChocolateFactoryTimeTowerManager.timeTowerFull()) {
                    slot highlight LorenzColor.RED
                }
            }
        }
    }

    @SubscribeEvent
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (!ChocolateFactoryAPI.inChocolateFactory) return
        if (!config.showStackSizes) return
        val profileStorage = profileStorage ?: return

        val item = event.stack
        val itemName = item.name.removeColor()
        val slotNumber = event.slot.slotNumber

        if (slotNumber in ChocolateFactoryAPI.rabbitSlots) {
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
        if (slotNumber in ChocolateFactoryAPI.otherUpgradeSlots) {
            upgradeTierPattern.matchMatcher(itemName) {
                val level = group("tier").romanToDecimal()

                if (slotNumber == ChocolateFactoryAPI.timeTowerIndex) profileStorage.timeTowerLevel = level

                event.stackTip = level.toString()
            }
        }
    }

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!ChocolateFactoryAPI.inChocolateFactory) return
        val slot = event.slot ?: return
        if (!config.useMiddleClick) return
        if (slot.slotNumber in ChocolateFactoryAPI.noPickblockSlots) return

        event.makePickblock()
    }
}
