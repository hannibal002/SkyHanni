package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable

@SkyHanniModule
object FairySoulQuestOverlay {
    val config get() = SkyHanniMod.feature.misc.fairySouls

    private var inFairySoulQuestMenu = false

    private val islandList = mutableListOf<String>()
    private var displayList = emptyList<Renderable>()

    private var remainingMap = mapOf<Int, FairySoulApi.remainingMapData>()

    @HandleEvent(onlyOnSkyblock = true)
    private fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        inFairySoulQuestMenu = event.inventoryName == "Fairy Souls Guide"

        if (!inFairySoulQuestMenu) return

        remainingMap = FairySoulApi.getRemainingMap(event)

        for (islandSlot in remainingMap.keys) {
            remainingMap[islandSlot]?.soulsRemaining?.let {
                if (it > 0) {
                    islandList.add(
                        "§2${remainingMap[islandSlot]?.islandName}§7: " +
                            "§e${remainingMap[islandSlot]?.soulsFound}§7/§d${remainingMap[islandSlot]?.soulsTotal}"
                    )
                }
            }
        }
    }

    @HandleEvent
    private fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (!(inFairySoulQuestMenu && config.fairySoulStackSize)) return

        val slotIndex = event.slot.index
        if (slotIndex !in remainingMap) return

        event.stackTip = getStackTip(slotIndex)
    }

    @HandleEvent(GuiContainerEvent.BackgroundDrawnEvent::class, onlyOnSkyblock = true)
    private fun onBackgroundDrawn() {
        if (!inFairySoulQuestMenu) return
        if (!config.fairySoulQuestHighlight) return

        for (slot in remainingMap.keys) {
            val soulsRemaining = remainingMap[slot]?.soulsRemaining ?: continue

            if (soulsRemaining > 0) {
                InventoryUtils.getSlotAtIndex(slot)?.highlight(LorenzColor.RED)
            }
        }
    }

    private fun getStackTip(slotIndex: Int): String {
        val soulsRemaining = remainingMap[slotIndex]?.soulsRemaining ?: return ""
        return if (soulsRemaining > 0) "§e$soulsRemaining" else ""
    }

    @HandleEvent
    private fun onInventoryClose() {
        inFairySoulQuestMenu = false
        islandList.clear()
    }

    @HandleEvent
    private fun onChestGuiRender() {
        if (!SkyBlockUtils.onHypixel) return
        if (!(inFairySoulQuestMenu && config.fairySoulOverlay)) return

        displayList = buildList {
            for (island in islandList) {
                addString(island)
            }
        }

        config.pos.renderRenderables(
            displayList,
            extraSpace = 1,
            posLabel = "Fairy Soul Quest Overlay",
        )
    }
}
