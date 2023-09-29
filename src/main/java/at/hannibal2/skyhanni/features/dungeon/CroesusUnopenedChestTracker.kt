package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CroesusUnopenedChestTracker {

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.dungeon.croesusUnopenedChestTracker) return

        val chestName = InventoryUtils.openInventoryName()

        if (chestName == "Croesus") {
            for (slot in InventoryUtils.getItemsInOpenChest()) {
                val stack = slot.stack
                val lore = stack.getLore()
                if ("§eClick to view chests!" in lore && "§aNo more Chests to open!" !in lore) {
                    val hasOpenedChests = lore.any { it.contains("Opened Chest") }
                    slot highlight if (hasOpenedChests) LorenzColor.DARK_AQUA else LorenzColor.DARK_PURPLE
                }
            }
        }
    }
}