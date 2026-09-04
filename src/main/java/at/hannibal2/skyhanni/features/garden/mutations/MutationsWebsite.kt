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
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.Items
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object MutationsWebsite {

    private val config get() = SkyHanniMod.feature.garden.greenhouse

    private const val ICON_SLOT = 8
    private const val MIDDLE_SLOT = 22
    private const val LINK = "https://skymutations.eu/"
    private var lastClicked = SimpleTimeMark.farPast()

    private val icon by lazy {
        ItemUtils.createItemStack(
            Items.MAP,
            "§aSky Mutations",
            "§8(From SkyHanni)",
            "",
            "§7Click here to open",
            "§7guides and other useful",
            "§7information about mutations",
            "§7on §askymutations.eu",
        )
    }

    private var inInventory = false

    @HandleEvent
    private fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        inInventory = event.isInInventory()
    }

    @HandleEvent
    private fun onInventoryClose() {
        inInventory = false
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

        // we expect an empty slot or an unnamed filler pane, warn if Hypixel ever puts a real item here
        if (event.hasItem) {
            val originalItem = event.originalItem
            if (originalItem.cleanName.isNotEmpty()) {
                ErrorManager.logErrorStateWithData(
                    "can not show item for mutations website", "at slot $ICON_SLOT is already a different item.",
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
        if (event.slot?.container !is SimpleContainer) return
        if (event.slotId != ICON_SLOT) return
        event.cancel()
        if (lastClicked.passedSince() < 2.seconds) return
        OSUtils.openBrowser(LINK)
        lastClicked = SimpleTimeMark.now()
    }

    private fun isEnabled() = config.mutationsWebsite
}
