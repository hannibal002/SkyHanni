package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CruxTalismanDisplay {

    private val config get() = SkyHanniMod.feature.rift.crux
    private val partialName = "CRUX_TALISMAN"
    private var display = listOf<List<Any>>()
    private val displayLine = mutableListOf<String>()

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return
        config.cruxTalismanPosition.renderStringsAndItems(
                display,
                posLabel = "Crux Talisman Display"
        )
    }

    private fun update() {
        display = drawDisplay()
    }

    private fun drawDisplay(): List<List<Any>> {
        return buildList { if (displayLine.isNotEmpty()) displayLine.forEach { addAsSingletonList(it) } }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!event.isMod(20)) return
        displayLine.clear()
        var found = false
        val inventoryStack = InventoryUtils.getItemsInOwnInventory()
        for (stack in inventoryStack) {
            val internalName = stack.getInternalName()
            if (internalName.contains(partialName)) {
                for (line in stack.getLore()) {
                    if (line.startsWith("§7Kill Milestones")) {
                        found = true
                        continue
                    }
                    if (found) {
                        if (line.isEmpty()) {
                            found = false
                            continue
                        }
                        displayLine.add(line)
                    }
                }
            }
        }
        update()
    }

    fun isEnabled() = RiftAPI.inRift() && config.cruxTalismanProgress && InventoryUtils.getItemsInOwnInventory().any { it.getInternalName().startsWith(partialName) }

}