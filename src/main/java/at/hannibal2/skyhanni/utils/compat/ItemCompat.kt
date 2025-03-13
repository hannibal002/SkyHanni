package at.hannibal2.skyhanni.utils.compat

import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
//#if MC > 1.21
//$$ import net.minecraft.item.Item
//$$ import net.minecraft.item.tooltip.TooltipType
//#endif

fun ItemStack.getTooltipCompat(advanced: Boolean): MutableList<String> {
    //#if MC < 1.12
    return this.getTooltip(Minecraft.getMinecraft().thePlayer, advanced)
    //#elseif MC < 1.16
    //$$ return this.getTooltip(Minecraft.getMinecraft().player) { advanced }
    //#elseif MC < 1.21
    //$$ return this.getTooltipLines(Minecraft.getInstance().player) { advanced }.map { it.getFormattedTextCompat() }.toMutableList()
    //#else
    //$$ val tooltipType = if (advanced) TooltipType.ADVANCED else TooltipType.BASIC
    //$$ return this.getTooltip(Item.TooltipContext.DEFAULT, MinecraftClient.getInstance().player, tooltipType).map { it.getFormattedTextCompat() }.toMutableList()
    //#endif
}
