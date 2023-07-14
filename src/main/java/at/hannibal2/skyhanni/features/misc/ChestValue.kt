package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.OtherInventoryData
import at.hannibal2.skyhanni.events.DrawScreenAfterEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValue
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addOrPut
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NumberUtil
import io.github.moulberry.notenoughupdates.mixins.AccessorGuiContainer
import io.github.moulberry.notenoughupdates.util.GuiTextures.accessory_bag_overlay
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumChatFormatting
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11
import scala.Console.println
import java.awt.Color
import java.util.*


class ChestValue {

    private val config get() = SkyHanniMod.feature.misc

    var data = mutableMapOf<Int, MutableList<ItemStack>>()

    private var itemValueTotal = 0.0
    private var itemEntryList: MutableList<Map.Entry<ItemStack, Double>> = mutableListOf()

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.estimatedItemValueChest) return

        var value = 0.0
        var valueMap: MutableMap<ItemStack, Double> = mutableMapOf()

        for ((slot, item) in event.inventoryItems) {
            value += EstimatedItemValue.getEstimatedItemPrice(item, mutableListOf()).first * item.stackSize
            valueMap.addOrPut(item, EstimatedItemValue.getEstimatedItemPrice(item, mutableListOf()).first * item.stackSize)
        }
        itemValueTotal = value
        var i = 0

        itemEntryList.clear()
        while (i<=3){
            var highestValueEntry: Map.Entry<ItemStack, Double>? = null
            var highestValue: Double? = null


            for (entry in valueMap.entries) {
                if (highestValue == null || entry.value > highestValue) {
                    highestValueEntry = entry
                    highestValue = entry.value
                }
            }
            if (highestValueEntry != null) {
                itemEntryList.add(highestValueEntry)
                valueMap.remove(highestValueEntry.key)
            }
            i += 1
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onInventoryRender(event: DrawScreenAfterEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.estimatedItemValueChest) return
        if(InventoryUtils.openInventoryName() != "Chest" && InventoryUtils.openInventoryName() != "Large Chest") return
        if(itemValueTotal == 0.0) return
        val mc = Minecraft.getMinecraft()
        val fontRenderer = mc.fontRendererObj
        val gui = mc.currentScreen
        val accessorGui = (gui as? AccessorGuiContainer) ?: return

        val guiLeft = accessorGui.guiLeft
        val guiTop = accessorGui.guiTop
        val xSize = accessorGui.xSize

        Minecraft.getMinecraft().textureManager.bindTexture(accessory_bag_overlay)
        Utils.drawTexturedRect(
            ((guiLeft + xSize) + 3).toFloat(),
            guiTop.toFloat(),
            80f,
            149f,
            0f,
            80 / 256f,
            0f,
            149 / 256f,
            GL11.GL_NEAREST
        )
        fontRenderer.drawString("Chest Value:", guiLeft + xSize + 10, guiTop + 10, Color.BLACK.rgb)
        var string = ""
        string = if(config.formatItemValueChest) EnumChatFormatting.BOLD.toString() + NumberUtil.format(itemValueTotal.round(1))
        else EnumChatFormatting.BOLD.toString() + itemValueTotal.round(1)
        fontRenderer.drawString(string, guiLeft + xSize + 10, guiTop + 10 + fontRenderer.FONT_HEIGHT, 16766208)
        var i = 0
        while(i<=itemEntryList.size - 1) {
            val entry = itemEntryList[i]
            GuiRenderUtils.renderItemStack(
                entry.key,
                guiLeft + xSize + 10,
                guiTop + 20 + (fontRenderer.FONT_HEIGHT * (i + 1)) * 2
            )
            if(entry.key.stackSize != 1) {fontRenderer.drawString(entry.key.stackSize.toString(), guiLeft + xSize + 20, guiTop + 30 + (fontRenderer.FONT_HEIGHT * (i + 1)) * 2, Color.WHITE.rgb)}
            if(config.formatItemValueChest) fontRenderer.drawString(EnumChatFormatting.BOLD.toString() + NumberUtil.format(entry.value.round(1)), guiLeft + xSize + 10 + (fontRenderer.FONT_HEIGHT * 2), guiTop + 20 + (fontRenderer.FONT_HEIGHT * (i + 1)) * 2, 16766208)
            else fontRenderer.drawString(EnumChatFormatting.BOLD.toString() + entry.value.round(1), guiLeft + xSize + 10 + (fontRenderer.FONT_HEIGHT * 2), guiTop + 20 + (fontRenderer.FONT_HEIGHT * (i + 1)) * 2, 16766208)
            i+=1
        }
    }
}