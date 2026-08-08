package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.misc.TooltipPriceInfoConfig.PriceTypes
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi.getBazaarData
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getLowestBinOrNull
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getNpcPriceOrNull
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getRawCraftCostOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getItemUuid
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@SkyHanniModule
object TooltipPriceInfo {

    private val config get() = SkyHanniMod.feature.misc.tooltipPriceInfo

    private val format = DecimalFormat("#,##0.#", DecimalFormatSymbols(Locale.US))

    @HandleEvent(onlyOnSkyblock = true)
    private fun onTooltip(event: ToolTipTextEvent) {
        if (!config.showPriceInLore || config.priceTypes.isEmpty()) return

        val isShiftDown = Minecraft.getInstance().hasShiftDown()
        val shiftMultiplier = getShiftMultiplier(event.itemStack)

        val internalName = event.itemStack.getInternalName()
        val multiplier = if (isShiftDown) shiftMultiplier else 1
        val priceLines = getPriceLines(internalName, multiplier)

        if (priceLines.isNotEmpty()) {
            if (!isShiftDown && shiftMultiplier > 1) priceLines.add(Component.literal("§8[SHIFT show x$shiftMultiplier]"))
            event.toolTip.add(Component.literal(""))
            event.toolTip.addAll(priceLines)
        }
    }

    private fun getShiftMultiplier(itemStack: ItemStack): Int {
        if (itemStack.getItemUuid() != null) return 1
        val shiftMultiplier = if (itemStack.count > 1) itemStack.count else itemStack.maxStackSize
        return shiftMultiplier
    }

    private fun getPriceLines(internalName: NeuInternalName, multiplier: Int): MutableList<Component> {
        val lines = mutableListOf<Component>()
        for (priceType in config.priceTypes) {
            when (priceType) {
                PriceTypes.LBIN -> internalName.getLowestBinOrNull()?.let {
                    val text = "§eLowest BIN: §6${format.format(it * multiplier)}"
                    lines.add(Component.literal(text))
                }
                PriceTypes.INSTA_BUY -> internalName.getBazaarData()?.let {
                    val text = "§eInsta Buy: §6${format.format(it.instantBuyPrice * multiplier)}"
                    lines.add(Component.literal(text))
                }
                PriceTypes.INSTA_SELL -> internalName.getBazaarData()?.let {
                    val text = "§eInsta Sell: §6${format.format(it.instantSellPrice * multiplier)}"
                    lines.add(Component.literal(text))
                }
                PriceTypes.NPC -> internalName.getNpcPriceOrNull()?.let {
                    val text = "§eNPC Price: §6${format.format(it * multiplier)}"
                    lines.add(Component.literal(text))
                }
                PriceTypes.CRAFT_COST -> internalName.getRawCraftCostOrNull()?.let {
                    val text = "§eCraft Cost: §6${format.format(it * multiplier)}"
                    lines.add(Component.literal(text))
                }
            }
        }
        return lines
    }
}
