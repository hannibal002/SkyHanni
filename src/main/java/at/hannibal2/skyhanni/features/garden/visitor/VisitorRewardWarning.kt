package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.data.ItemRenderBackground.Companion.background
import at.hannibal2.skyhanni.data.ItemRenderBackground.Companion.borderLine
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.visitor.VisitorAPI.lastClickedNpc
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import io.github.moulberry.notenoughupdates.events.SlotClickEvent
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard


class VisitorRewardWarning {
    private val config get() = VisitorAPI.config.rewardWarning

    private val ACCEPT_SLOT = 29
    private val REFUSE_SLOT = 33

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!VisitorAPI.inInventory) return
        if (!config.preventRefusing && !config.preventRefusingCopper && !config.preventAcceptingCopper) return

        val visitor = VisitorAPI.getVisitor(lastClickedNpc) ?: return
        val blockReason = visitor.blockReason(true, true, true)
        val refuseOfferStack = event.gui.inventorySlots.getSlot(REFUSE_SLOT).stack ?: return
        val acceptOfferStack = event.gui.inventorySlots.getSlot(ACCEPT_SLOT).stack ?: return

        if (blockReason == VisitorBlockReason.RARE_REWARD || blockReason == VisitorBlockReason.CHEAP_COPPER) {
            if (!config.bypassKey.isKeyHeld()) refuseOfferStack.background = LorenzColor.DARK_GRAY.addOpacity(config.opacity).rgb
            if (config.optionOutline) acceptOfferStack.borderLine = LorenzColor.GREEN.addOpacity(200).rgb
            return
        }
        if (blockReason == VisitorBlockReason.EXPENSIVE_COPPER) {
            if (!config.bypassKey.isKeyHeld()) acceptOfferStack.background = LorenzColor.DARK_GRAY.addOpacity(config.opacity).rgb
            if (config.optionOutline) refuseOfferStack.borderLine = LorenzColor.RED.addOpacity(200).rgb
            return
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onStackClick(event: SlotClickEvent) {
        if (!VisitorAPI.inInventory) return

        val visitor = VisitorAPI.getVisitor(lastClickedNpc) ?: return
        val blockReason = visitor.blockReason(event.slotId == ACCEPT_SLOT, event.slotId == REFUSE_SLOT)

        if (blockReason == VisitorBlockReason.RARE_REWARD || blockReason == VisitorBlockReason.CHEAP_COPPER) {
            event.isCanceled = true
            return
        }

        if (blockReason == VisitorBlockReason.EXPENSIVE_COPPER) {
            event.isCanceled = true
            return
        }

        if (event.slotId == REFUSE_SLOT) {
            if (event.slot.stack?.name != "§cRefuse Offer") return
            VisitorAPI.changeStatus(visitor, VisitorAPI.VisitorStatus.REFUSED, "refused")
            return
        }
        if (event.slotId == ACCEPT_SLOT) {
            if (event.slot.stack?.name != "§eClick to give!") return
            VisitorAPI.changeStatus(visitor, VisitorAPI.VisitorStatus.ACCEPTED, "accepted")
            return
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onTooltip(event: ItemTooltipEvent) {
        if (!GardenAPI.onBarnPlot) return
        if (!VisitorAPI.inInventory) return
        val visitor = VisitorAPI.getVisitor(lastClickedNpc) ?: return

        val isRefuseSlot = event.itemStack.toString() == "1xtile.clayHardenedStained@14" // red
        val isAcceptSlot = event.itemStack.toString() == "1xtile.clayHardenedStained@13" // green

        val blockReason = visitor.blockReason(isAcceptSlot, isRefuseSlot)
        if (blockReason == VisitorBlockReason.NONE) return

        val copiedTooltip = event.toolTip.toList()
        event.toolTip.clear()

        for (line in copiedTooltip) {
            if (line.contains("§aAccept Offer§r")) {
                event.toolTip.add(line.replace("§aAccept Offer§r", "§7Accept Offer§8"))
            }
            else if (line.contains("§cRefuse Offer§r")) {
                event.toolTip.add(line.replace("§cRefuse Offer§r", "§7Refuse Offer§8"))
            }
            else if (!line.contains("minecraft:") && !line.contains("NBT:")) {
                event.toolTip.add("§8" + line.removeColor())
            }
        }
        event.toolTip.add("")
        event.toolTip.add(blockReason.description)
        event.toolTip.add("  §7(Bypass by holding ${Keyboard.getKeyName(config.bypassKey)})")
    }

    fun VisitorAPI.Visitor.blockReason(
        isAcceptSlot: Boolean,
        isRefuseSlot: Boolean,
        ignoreBypassKey: Boolean = false
    ): VisitorBlockReason {
        if (config.bypassKey.isKeyHeld() && !ignoreBypassKey) return VisitorBlockReason.NONE
        val visitorHasReward = this.hasReward() != null && config.preventRefusing
        return when {
            visitorHasReward && isRefuseSlot -> VisitorBlockReason.RARE_REWARD
            this.pricePerCopper <= config.coinsPerCopperPrice && isRefuseSlot && config.preventRefusingCopper -> VisitorBlockReason.CHEAP_COPPER
            this.pricePerCopper > config.coinsPerCopperPrice && isAcceptSlot && config.preventAcceptingCopper && !visitorHasReward -> VisitorBlockReason.EXPENSIVE_COPPER
            else -> VisitorBlockReason.NONE
        }
    }

    enum class VisitorBlockReason(val description: String) {
        RARE_REWARD("§aRare visitor reward found"),
        CHEAP_COPPER("§cCheap copper"),
        EXPENSIVE_COPPER("§cExpensive copper"),
        NONE("Error")
    }
}
