package at.hannibal2.skyhanni.features.anvil

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.utils.InventoryUtils.getInventoryName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object AnvilCombineHelper {

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return

        val chest = event.gui.inventorySlots as? ContainerChest ?: return
        val chestName = chest.getInventoryName()

        if (chestName != "Anvil") return

        val matchLore = chest.inventorySlots
            .filterNotNull()
            .filter { it.slotNumber == it.slotIndex && it.stack != null }
            .firstOrNull { it.slotNumber == 29 }?.stack?.getLore() ?: return

        if (matchLore.isEmpty()) return

        chest.inventorySlots
            .filterNotNull()
            .filter { it.slotNumber != it.slotIndex && it.stack != null && matchLore == it.stack.getLore() }
            .forEach { it highlight LorenzColor.GREEN }
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && SkyHanniMod.feature.inventory.anvilCombineHelper
}
