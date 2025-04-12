package at.hannibal2.skyhanni.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.drawSlotText
import at.hannibal2.skyhanni.utils.RenderUtils.highlight

@SkyHanniModule
object CFInventory {

    private val config get() = CFApi.config

    /**
     * REGEX-TEST: §7§aYou have 1 unclaimed reward!
     * REGEX-TEST: §7§aYou have 2 unclaimed rewards!
     */
    private val unclaimedRewardsPattern by CFApi.patternGroup.pattern(
        "unclaimedrewards",
        "§7§aYou have \\d+ unclaimed rewards?!",
    )

    @HandleEvent
    fun onForegroundDrawn(event: GuiContainerEvent.ForegroundDrawnEvent) {
        if (!CFApi.inChocolateFactory) return
        if (!config.highlightUpgrades) return


        for (slot in InventoryUtils.getItemsInOpenChest()) {
            if (slot.stack == null) continue
            val slotIndex = slot.slotNumber

            if (slotIndex == CFApi.bestPossibleSlot) {
                event.drawSlotText(slot.xDisplayPosition + 18, slot.yDisplayPosition, "§6✦", 1f)
            }
        }
    }

    @HandleEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!CFApi.inChocolateFactory) return
        if (!config.highlightUpgrades) return

        for (slot in InventoryUtils.getItemsInOpenChest()) {
            if (slot.stack == null) continue
            val slotIndex = slot.slotNumber

            val currentUpdates = CFApi.factoryUpgrades
            currentUpdates.find { it.slotIndex == slotIndex }?.let { upgrade ->
                if (upgrade.canAfford()) {
                    slot.highlight(event.context, LorenzColor.GREEN.addOpacity(75))
                }
            }
            if (slotIndex == CFApi.bestAffordableSlot) {
                slot.highlight(event.context, LorenzColor.GREEN.addOpacity(200))
            }

            if (slotIndex == CFApi.barnIndex && CFBarnManager.isBarnFull()) {
                slot.highlight(event.context, LorenzColor.RED)
            }
            if (slotIndex == CFApi.milestoneIndex) {
                unclaimedRewardsPattern.firstMatcher(slot.stack?.getLore().orEmpty()) {
                    slot.highlight(event.context, LorenzColor.RED)
                }
            }
            if (slotIndex == CFApi.timeTowerIndex) {
                if (CFTimeTowerManager.timeTowerActive()) {
                    slot.highlight(event.context, LorenzColor.LIGHT_PURPLE.addOpacity(200))
                }
                if (CFTimeTowerManager.timeTowerFull()) {
                    slot.highlight(event.context, LorenzColor.RED)
                }
            }
        }
    }

    @HandleEvent
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (!CFApi.inChocolateFactory) return
        if (!config.showStackSizes) return

        val upgradeInfo = CFApi.factoryUpgrades.find { it.slotIndex == event.slot.slotNumber } ?: return
        event.stackTip = upgradeInfo.stackTip()
    }

    @HandleEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!CFApi.inChocolateFactory) return
        val slot = event.slot ?: return
        val slotNumber = slot.slotNumber
        if (!config.useMiddleClick) return
        if (slotNumber in CFApi.noPickblockSlots &&
            (slotNumber != CFApi.timeTowerIndex || event.clickedButton == 1)
        ) return

        // this would break CFKeybinds otherwise
        if (event.clickType == GuiContainerEvent.ClickType.HOTBAR) return

        // if the user is holding shift, we don't want to pickblock, handled by hypixel as +10 levels for rabbits
        if (KeyboardManager.isShiftKeyDown() && slotNumber in CFApi.rabbitSlots.keys) return

        event.makePickblock()
    }
}
