package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.data.ItemRenderBackground.Companion.background
import at.hannibal2.skyhanni.data.ItemRenderBackground.Companion.borderLine
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.visitor.VisitorAPI.ACCEPT_SLOT
import at.hannibal2.skyhanni.features.garden.visitor.VisitorAPI.REFUSE_SLOT
import at.hannibal2.skyhanni.features.garden.visitor.VisitorAPI.lastClickedNpc
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard


class VisitorRewardWarning {
    private val config get() = VisitorAPI.config.rewardWarning

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!VisitorAPI.inInventory) return
        if (!config.preventRefusing && !config.preventRefusingCopper && !config.preventAcceptingCopper) return

        val visitor = VisitorAPI.getVisitor(lastClickedNpc) ?: return
        val blockReason = visitor.blockReason(isAcceptSlot = true, isRefuseSlot = true, ignoreBypassKey = true)?: return
        val refuseOfferStack = event.gui.inventorySlots.getSlot(REFUSE_SLOT).stack ?: return
        val acceptOfferStack = event.gui.inventorySlots.getSlot(ACCEPT_SLOT).stack ?: return

        if (blockReason.blockRefusing) {
            renderColor(refuseOfferStack, acceptOfferStack, LorenzColor.GREEN)
        } else {
            renderColor(acceptOfferStack, refuseOfferStack, LorenzColor.RED)
        }
    }

    private fun renderColor(backgroundStack: ItemStack, outlineStack: ItemStack, outlineColor: LorenzColor) {
        if (!config.bypassKey.isKeyHeld()) backgroundStack.background = LorenzColor.DARK_GRAY.addOpacity(config.opacity).rgb
        if (config.optionOutline) outlineStack.borderLine = outlineColor.addOpacity(200).rgb
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onStackClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!VisitorAPI.inInventory) return
        val slot = event.slot ?: return

        val visitor = VisitorAPI.getVisitor(lastClickedNpc) ?: return
        val blockReason = visitor.blockReason(event.slotId == ACCEPT_SLOT, event.slotId == REFUSE_SLOT)

        if (blockReason != null) {
            event.isCanceled = true
            return
        }

        if (event.slotId == REFUSE_SLOT) {
            if (slot.stack?.name != "§cRefuse Offer") return
            VisitorAPI.changeStatus(visitor, VisitorAPI.VisitorStatus.REFUSED, "refused")
            return
        }
        if (event.slotId == ACCEPT_SLOT) {
            if (slot.stack?.name != "§eClick to give!") return
            VisitorAPI.changeStatus(visitor, VisitorAPI.VisitorStatus.ACCEPTED, "accepted")
            return
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onTooltip(event: ItemTooltipEvent) {
        if (!GardenAPI.onBarnPlot) return
        if (!VisitorAPI.inInventory) return
        val visitor = VisitorAPI.getVisitor(lastClickedNpc) ?: return

        val isRefuseSlot = event.itemStack.name == "§cRefuse Offer"
        val isAcceptSlot = event.itemStack.name == "§aAccept Offer"

        val blockReason = visitor.blockReason(isAcceptSlot, isRefuseSlot)?: return

        if (visitor.blockedLore.isEmpty()) {
            println("Rendering blocked tooltip now")
            val copiedTooltip = event.toolTip.toList()
            val blockedToolTip = mutableListOf<String>()

            for (line in copiedTooltip) {
                if (line.contains("§aAccept Offer§r")) {
                    blockedToolTip.add(line.replace("§aAccept Offer§r", "§7Accept Offer§8"))
                } else if (line.contains("§cRefuse Offer§r")) {
                    blockedToolTip.add(line.replace("§cRefuse Offer§r", "§7Refuse Offer§8"))
                } else if (!line.contains("minecraft:") && !line.contains("NBT:")) {
                    blockedToolTip.add("§8" + line.removeColor())
                }
            }
            blockedToolTip.add("")
            val pricePerCopper = visitor.pricePerCopper?.let { NumberUtil.format(it) }
            if (blockReason == VisitorBlockReason.CHEAP_COPPER || blockReason == VisitorBlockReason.EXPENSIVE_COPPER) {
                blockedToolTip.add("${blockReason.description} §7(§6$pricePerCopper §7per)")
            } else blockedToolTip.add(blockReason.description)
            blockedToolTip.add("  §7(Bypass by holding ${Keyboard.getKeyName(config.bypassKey)})")

            visitor.blockedLore = blockedToolTip
        }
        event.toolTip.clear()
        event.toolTip.addAll(visitor.blockedLore)
    }

    private fun VisitorAPI.Visitor.blockReason(
        isAcceptSlot: Boolean,
        isRefuseSlot: Boolean,
        ignoreBypassKey: Boolean = false
    ): VisitorBlockReason? {
        if (!ignoreBypassKey && config.bypassKey.isKeyHeld()) return null

        val visitorHasReward = config.preventRefusing && this.hasReward() != null
        if (visitorHasReward && isRefuseSlot) {
            return VisitorBlockReason.RARE_REWARD
        }
        else if (config.preventRefusingNew && this.offersAccepted == 0) {
            return VisitorBlockReason.NEVER_ACCEPTED
        }
        val pricePerCopper = this.pricePerCopper ?: return VisitorBlockReason.EXPENSIVE_COPPER
        return if (config.preventRefusingCopper && isRefuseSlot && pricePerCopper <= config.coinsPerCopperPrice) {
            VisitorBlockReason.CHEAP_COPPER
        }
        else if (config.preventAcceptingCopper && isAcceptSlot && pricePerCopper > config.coinsPerCopperPrice && !visitorHasReward) {
            VisitorBlockReason.EXPENSIVE_COPPER
        }
        else null
    }

    enum class VisitorBlockReason(val description: String, val blockRefusing: Boolean) {
        NEVER_ACCEPTED("§cNever accepted", true),
        RARE_REWARD("§aRare visitor reward found", true,),
        CHEAP_COPPER("§aCheap copper", true),
        EXPENSIVE_COPPER("§cExpensive copper", false)
    }
}
