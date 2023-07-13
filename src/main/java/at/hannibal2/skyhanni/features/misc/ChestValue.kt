package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.DrawScreenAfterEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValue
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addOrPut
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import io.github.moulberry.notenoughupdates.mixins.AccessorGuiContainer
import io.github.moulberry.notenoughupdates.util.GuiTextures.accessory_bag_overlay
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumChatFormatting
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.*


class ChestValue {

    private val config get() = SkyHanniMod.feature.misc

    var data = mutableMapOf<Int, MutableList<ItemStack>>()

    private var itemValueTotal = 0.0
    private var itemEntryList: MutableList<Map.Entry<ItemStack, Double>> = mutableListOf()

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.estimatedChestValue) return

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
        if (!config.estimatedChestValue) return
        val mc = Minecraft.getMinecraft()
        val fontRenderer = mc.fontRendererObj
        if (Minecraft.getMinecraft().currentScreen !is GuiChest) return
        val gui = mc.currentScreen
        val gui2: GuiChest = gui as GuiChest
        val cc: ContainerChest = gui2.inventorySlots as ContainerChest
        val guiName: String = cc.lowerChestInventory.displayName.unformattedText
        if (guiName != "Large Chest" && guiName != "Chest") return
        val accessorGui: AccessorGuiContainer? = gui as? AccessorGuiContainer
        Minecraft.getMinecraft().textureManager.bindTexture(accessory_bag_overlay)
        if (accessorGui != null) {
            Utils.drawTexturedRect(
                ((accessorGui.guiLeft + accessorGui.xSize) + 3).toFloat(),
                accessorGui.guiTop.toFloat(),
                80f,
                149f,
                0f,
                80 / 256f,
                0f,
                149 / 256f,
                GL11.GL_NEAREST
            )

            fontRenderer.drawString("Chest Value:", accessorGui.guiLeft + accessorGui.xSize + 10, accessorGui.guiTop + 10, Color.BLACK.rgb)
            val string: String = EnumChatFormatting.BOLD.toString() + java.text.NumberFormat.getIntegerInstance().format(itemValueTotal.round(1)).toString()
            fontRenderer.drawString(string, accessorGui.guiLeft + accessorGui.xSize + 10, accessorGui.guiTop + 10 + fontRenderer.FONT_HEIGHT, 16766208)
            var i = 0
            while(i<=itemEntryList.size - 1) {
                val entry = itemEntryList[i]
                GuiRenderUtils.renderItemStack(
                    entry.key,
                    accessorGui.guiLeft + accessorGui.xSize + 10,
                    accessorGui.guiTop + 20 + (fontRenderer.FONT_HEIGHT * (i + 1)) * 2
                )
                if(entry.key.stackSize != 1) {fontRenderer.drawString(entry.key.stackSize.toString(), accessorGui.guiLeft + accessorGui.xSize + 20, accessorGui.guiTop + 30 + (fontRenderer.FONT_HEIGHT * (i + 1)) * 2, Color.WHITE.rgb)}
                fontRenderer.drawString(EnumChatFormatting.BOLD.toString() + entry.value.round(1).toString(), accessorGui.guiLeft + accessorGui.xSize + 10 + (fontRenderer.FONT_HEIGHT * 2), accessorGui.guiTop + 20 + (fontRenderer.FONT_HEIGHT * (i + 1)) * 2, 16766208)
                i+=1
            }
        }
    }
}