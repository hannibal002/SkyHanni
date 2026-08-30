package at.hannibal2.skyhanni.features.garden.mutations

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.plot.GardenPlotApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.OSUtils
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.Items

@SkyHanniModule
object MutationsWebsite {

    private val config get() = SkyHanniMod.feature.garden.greenhouse

    private const val ICON_SLOT = 8
    private const val MIDDLE_SLOT = 22
    private const val LINK = "https://skymutations.eu/"

    private val icon by lazy {
        ItemUtils.createItemStack(
            Items.MAP,
            "§aSky Mutations",
            "§8(Link from SkyHanni)",
            "",
            "§7Click to open the §aSky Mutations",
            "§7webiste, with guides and other",
            "§7useful informations for mutations!",
        )
    }

    private var inInventory = false

    @HandleEvent
    private fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        inInventory = event.isInInventory()
    }

    private fun InventoryUpdatedEvent.isInInventory(): Boolean {
        return inGardenCarpenter() || inCropAnalyzer()
    }

    private fun InventoryUpdatedEvent.inGardenCarpenter(): Boolean {
        if (!GardenApi.inGarden()) return false
        if (!GardenPlotApi.inGreenhouse()) return false
        if (inventoryName == "Carpenter") {
            return inventoryItems[MIDDLE_SLOT]?.cleanName == "All Mutations"
        }
        if (inventoryName == "Crop Guide") return true
        if (inventoryName.endsWith(" All Mutations")) return true

        return false
    }

    private fun InventoryUpdatedEvent.inCropAnalyzer(): Boolean {
        if (!IslandType.THE_FARMING_ISLANDS.isInIsland()) return false
        if (inventoryName == "Crop Analyzer") return true
        if (inventoryName.endsWith(" All Mutations")) return true

        return false
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onReplaceItem(event: ReplaceItemEvent) {
        if (!isEnabled()) return
        if (!inInventory) return
        if (event.inventory !is SimpleContainer) return
        if (event.slot != ICON_SLOT) return

        // only replace if there is either no item or a colored glass pane (no item name)
        if (event.hasItem) {
            val originalItem = event.originalItem
            if (originalItem.cleanName != "") {
                ErrorManager.logErrorStateWithData(
                    "can no show item for mutations website", "at slot $ICON_SLOT is already a different item.",
                    "originalItem" to originalItem,
                    "cleanName" to originalItem.cleanName,
                    "internalName" to originalItem.getInternalNameOrNull(),
                    "getCleanLore" to originalItem.getCleanLore(),
                )
            }
        }

        event.replace(icon)
    }

    @HandleEvent
    private fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled()) return
        if (!inInventory) return
        if (event.slotId == ICON_SLOT) {
            event.cancel()
            OSUtils.openBrowser(LINK)
        }
    }

    private fun isEnabled() = config.mutationsWebsite
}
