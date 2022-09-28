package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.between
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class RngMeterInventory {

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        val screen = Minecraft.getMinecraft().currentScreen
        if (screen !is GuiChest) return
        val chest = screen.inventorySlots as ContainerChest
        val chestName = chest.lowerChestInventory.displayName.unformattedText.trim()


        val stack = event.stack
        if (SkyHanniMod.feature.inventory.rngMeterFloorName) {
            if (chestName == "Catacombs RNG Meter") {
                val name = stack.name ?: return
                if (name.removeColor() == "RNG Meter") {
                    event.stackTip = stack.getLore()[0].between("(", ")")
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!LorenzUtils.inSkyblock) return

        if (event.gui !is GuiChest) return
        val guiChest = event.gui
        val chest = guiChest.inventorySlots as ContainerChest
        val chestName = chest.lowerChestInventory.displayName.unformattedText.trim()

        if (SkyHanniMod.feature.inventory.rngMeterNoDrop) {
            if (chestName == "Catacombs RNG Meter") {
                for (slot in InventoryUtils.getItemsInOpenChest()) {
                    val stack = slot.stack
                    if (stack.getLore().any { it.contains("You don't have an RNG drop") }) {
                        slot highlight LorenzColor.RED
                    }
                }
            }
        }

        if (SkyHanniMod.feature.inventory.rngMeterSelectedDrop) {
            if (chestName.endsWith(" RNG Meter")) {
                for (slot in InventoryUtils.getItemsInOpenChest()) {
                    val stack = slot.stack
                    if (stack.getLore().any { it.contains("§aSELECTED") }) {
                        slot highlight LorenzColor.YELLOW
                    }
                }
            }
        }
    }
}