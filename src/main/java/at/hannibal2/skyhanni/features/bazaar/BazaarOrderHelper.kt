package at.hannibal2.skyhanni.features.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11

class BazaarOrderHelper {

    companion object {
        fun isBazaarOrderInventory(inventoryName: String): Boolean = when (inventoryName) {
            "Your Bazaar Orders" -> true
            "Co-op Bazaar Orders" -> true
            else -> false
        }
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!SkyHanniMod.feature.bazaar.orderHelper) return
        if (event.gui !is GuiChest) return
        val guiChest = event.gui
        val chest = guiChest.inventorySlots as ContainerChest
        val inventoryName = chest.lowerChestInventory.displayName.unformattedText.trim()

        if (!isBazaarOrderInventory(inventoryName)) return
        val lightingState = GL11.glIsEnabled(GL11.GL_LIGHTING)
        GlStateManager.disableLighting()
        GlStateManager.color(1f, 1f, 1f, 1f)

        out@ for (slot in chest.inventorySlots) {
            if (slot == null) continue
            if (slot.slotNumber != slot.slotIndex) continue
            if (slot.stack == null) continue

            val stack = slot.stack
            val displayName = stack.displayName
            val isSelling = displayName.startsWith("§6§lSELL§7: ")
            val isBuying = displayName.startsWith("§a§lBUY§7: ")
            if (!isSelling && !isBuying) continue

            val text = displayName.split("§7: ")[1]
            val name = BazaarApi.getCleanBazaarName(text)
            val data = BazaarApi.getBazaarDataForName(name)
            val buyPrice = data.buyPrice
            val sellPrice = data.sellPrice

            val itemLore = stack.getLore()
            for (line in itemLore) {
                if (line.startsWith("§7Filled:")) {
                    if (line.endsWith(" §a§l100%!")) {
                        slot highlight LorenzColor.GREEN
                        continue@out
                    }
                }
            }
            for (line in itemLore) {
                if (line.startsWith("§7Price per unit:")) {
                    var text = line.split(": §6")[1]
                    text = text.substring(0, text.length - 6)
                    text = text.replace(",", "")
                    val price = text.toDouble()
                    if (isSelling) {
                        if (buyPrice < price) {
                            slot highlight LorenzColor.GOLD
                            continue@out
                        }
                    } else {
                        if (sellPrice > price) {
                            slot highlight LorenzColor.GOLD
                            continue@out
                        }
                    }

                }
            }
        }

        if (lightingState) GlStateManager.enableLighting()
    }
}
