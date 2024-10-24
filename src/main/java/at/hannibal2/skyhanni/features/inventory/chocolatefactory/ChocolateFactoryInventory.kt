package at.hannibal2.skyhanni.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.drawBorder
import at.hannibal2.skyhanni.utils.RegexUtils.matchFirst
import at.hannibal2.skyhanni.utils.RenderUtils.drawSlotText
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object ChocolateFactoryInventory {

    private val config get() = ChocolateFactoryAPI.config

    private val unclaimedRewardsPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "unclaimedrewards",
        "§7§aYou have \\d+ unclaimed rewards?!"
    )

    @SubscribeEvent
    fun onForegroundDrawn(event: GuiContainerEvent.ForegroundDrawnEvent) {
        if (!ChocolateFactoryAPI.inChocolateFactory) return
        if (!config.highlightUpgrades) return

        for (slot in InventoryUtils.getItemsInOpenChest()) {
            if (slot.stack == null) continue
            val slotIndex = slot.slotNumber

            if (slotIndex == ChocolateFactoryAPI.bestPossibleSlot) {
                slot drawBorder LorenzColor.GOLD.addOpacity(255)
            }

            if (config.showAllBestUpgrades && slotIndex in ChocolateFactoryAPI.allBestPossibleUpgrades.keys) {
                val current = ChocolateFactoryAPI.factoryUpgrades.find { it.slotIndex == slotIndex }
                val upgrades = ChocolateFactoryAPI.allBestPossibleUpgrades[slotIndex] ?: continue
                if (upgrades.isEmpty()) continue

                val upgrade = upgrades.last()

                if (current?.isMaxed == true) continue

                val dif = upgrade.level - (current?.level ?: 0) + 1

                val color = LorenzColor.AQUA
                event.drawSlotText(slot.xDisplayPosition + 18, slot.yDisplayPosition, color.getChatColor() + dif, 1f)
            }
        }
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!ChocolateFactoryAPI.inChocolateFactory) return
        if (!config.highlightUpgrades) return

        for (slot in InventoryUtils.getItemsInOpenChest()) {
            if (slot.stack == null) continue
            val slotIndex = slot.slotNumber

            val currentUpdates = ChocolateFactoryAPI.factoryUpgrades
            currentUpdates.find { it.slotIndex == slotIndex }?.let { upgrade ->
                if (upgrade.canAfford()) {
                    slot highlight LorenzColor.GREEN.addOpacity(75)
                }
            }

            if (slotIndex == ChocolateFactoryAPI.bestAffordableSlot) {
                slot highlight LorenzColor.GREEN.addOpacity(200)
            }

            if (slotIndex == ChocolateFactoryAPI.barnIndex && ChocolateFactoryBarnManager.barnFull) {
                slot highlight LorenzColor.RED
            }
            if (slotIndex == ChocolateFactoryAPI.clickRabbitSlot) {
                slot highlight LorenzColor.RED
            }
            if (slotIndex == ChocolateFactoryAPI.milestoneIndex) {
                slot.stack?.getLore()?.matchFirst(unclaimedRewardsPattern) {
                    slot highlight LorenzColor.RED
                }
            }
            if (slotIndex == ChocolateFactoryAPI.timeTowerIndex) {
                if (ChocolateFactoryTimeTowerManager.timeTowerActive()) {
                    slot highlight LorenzColor.LIGHT_PURPLE.addOpacity(200)
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

        val upgradeInfo = ChocolateFactoryAPI.factoryUpgrades.find { it.slotIndex == event.slot.slotNumber } ?: return
        event.stackTip = upgradeInfo.stackTip()
    }

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!ChocolateFactoryAPI.inChocolateFactory) return
        val slot = event.slot ?: return
        val slotNumber = slot.slotNumber
        if (!config.useMiddleClick) return
        if (slotNumber in ChocolateFactoryAPI.noPickblockSlots &&
            (slotNumber != ChocolateFactoryAPI.timeTowerIndex || event.clickedButton == 1)
        ) return

        // this would break ChocolateFactoryKeybinds otherwise
        if (event.clickTypeEnum == GuiContainerEvent.ClickType.HOTBAR) return

        event.makePickblock()
    }
}
