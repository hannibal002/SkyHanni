package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CruxTalismanDisplay {

    private val config get() = SkyHanniMod.feature.rift.crux
    private val partialName = "CRUX_TALISMAN"
    private var display = listOf<List<Any>>()
    private val displayLine = mutableListOf<String>()
    private val bonusesLine = mutableListOf<String>()

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
        return buildList {
            if (displayLine.isNotEmpty()) {
                addAsSingletonList("§7Progress:")
                displayLine.forEach { addAsSingletonList(it) }
            }
            if (bonusesLine.isNotEmpty()) {
                addAsSingletonList("§7Bonuses:")
                bonusesLine.forEach { addAsSingletonList("  $it") }
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!event.isMod(20)) return
        displayLine.clear()
        bonusesLine.clear()

        val inventoryStack = InventoryUtils.getItemsInOwnInventory()
        inventoryStack.filter { it.getInternalName().contains(partialName) }.forEach { stack ->
            var found = false
            var bonusFound = false

            stack.getLore().forEach forEachLine@{ line ->
                if (line.startsWith("§7Kill Milestones")) {
                    found = true
                    return@forEachLine
                }

                if (found) {
                    if (line.isEmpty()) {
                        found = false
                        return@forEachLine
                    }
                    displayLine.add(line)
                }

                if (line.startsWith("§7Total Bonuses")) {
                    bonusFound = true
                    return@forEachLine
                }

                if (bonusFound) {
                    if (line.isEmpty()) {
                        bonusFound = false
                        return@forEachLine
                    }
                    bonusesLine.add(line)
                }
            }
        }
        update()
    }

    fun isEnabled() = RiftAPI.inRift() && config.cruxTalismanProgress && InventoryUtils.getItemsInOwnInventory().any { it.getInternalName().startsWith(partialName) }
}