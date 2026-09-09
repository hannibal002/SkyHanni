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
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable

@SkyHanniModule
object FairySoulQuestOverlay {
    val config get() = SkyHanniMod.feature.misc.fairySouls

    private var inFairySoulQuestMenu = false

    private var display = emptyList<Renderable>()

    private var islandSoulInfo = mapOf<Int, FairySoulApi.IslandSoulInfo>()

    @HandleEvent(onlyOnSkyblock = true)
    private fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        inFairySoulQuestMenu = event.inventoryName == "Fairy Souls Guide"

        if (!inFairySoulQuestMenu) return

        islandSoulInfo = FairySoulApi.getIslandSoulInfo(event.inventoryItems)

        display = buildList {
            for (islandSlot in islandSoulInfo.keys) {
                islandSoulInfo[islandSlot]?.soulsRemaining?.let {
                    if (it > 0) {
                        addString(
                            "§2${islandSoulInfo[islandSlot]?.islandName}§7: " +
                                "§e${islandSoulInfo[islandSlot]?.soulsFound}§7/§d${islandSoulInfo[islandSlot]?.soulsTotal}"
                        )
                    }
                }
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (!(inFairySoulQuestMenu && config.stackSize)) return

        val slotIndex = event.slot.index
        if (slotIndex !in islandSoulInfo) return

        event.stackTip = getStackTip(slotIndex)
    }

    @HandleEvent(GuiContainerEvent.BackgroundDrawnEvent::class, onlyOnSkyblock = true)
    private fun onBackgroundDrawn() {
        if (!inFairySoulQuestMenu) return
        if (!config.questHighlight) return

        for (slot in islandSoulInfo.keys) {
            val soulsRemaining = islandSoulInfo[slot]?.soulsRemaining ?: continue

            if (soulsRemaining > 0) {
                InventoryUtils.getSlotAtIndex(slot)?.highlight(LorenzColor.RED)
            }
        }
    }

    private fun getStackTip(slotIndex: Int): String {
        val soulsRemaining = islandSoulInfo[slotIndex]?.soulsRemaining ?: return ""
        return if (soulsRemaining > 0) "§e$soulsRemaining" else ""
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onInventoryClose() {
        if (inFairySoulQuestMenu) {
            inFairySoulQuestMenu = false
            display = emptyList()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onChestGuiRender() {
        if (!(inFairySoulQuestMenu && config.overlay)) return

        config.pos.renderRenderables(
            display,
            extraSpace = 1,
            posLabel = "Fairy Soul Quest Overlay",
        )
    }
}
