package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.config.Storage
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.utils.LorenzUtils.addSelector
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.tracker.TrackerUtils.addSessionResetButton
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiInventory

class SkyHanniTracker<Data : TrackerData>(
    private val name: String,
    private val currentSessionData: Data,
    private val getStorage: (Storage.ProfileSpecific) -> Data,
    private val update: () -> Unit,
) {
    private var inventoryOpen = false
    private var displayMode = DisplayMode.TOTAL

    fun isInventoryOpen() = inventoryOpen

    private fun getSharedTracker(): SharedTracker<Data>? {
        val profileSpecific = ProfileStorageData.profileSpecific ?: return null
        return SharedTracker(getStorage(profileSpecific), currentSessionData)
    }

    fun addSessionResetButton(list: MutableList<List<Any>>) {
        if (inventoryOpen && displayMode == DisplayMode.SESSION) {
            list.addSessionResetButton(name, getSharedTracker()) {
                update()
            }
        }
    }

    fun addDisplayModeToggle(list: MutableList<List<Any>>) {
        if (!inventoryOpen) return

        list.addSelector<DisplayMode>(
            "§7Display Mode: ",
            getName = { type -> type.displayName },
            isCurrent = { it == displayMode },
            onChange = {
                displayMode = it
                update()
            }
        )
    }

    fun currentDisplay() = getSharedTracker()?.get(displayMode)

    fun resetCommand(args: Array<String>, command: String) {
        TrackerUtils.resetCommand(name, command, args, getSharedTracker()) {
            update()
        }
    }

    fun modify(modifyFunction: (Data) -> Unit) {
        getSharedTracker()?.modify(modifyFunction)
    }

    fun renderDisplay(position: Position, display: List<List<Any>>) {
        val currentlyOpen = Minecraft.getMinecraft().currentScreen is GuiInventory
        if (inventoryOpen != currentlyOpen) {
            inventoryOpen = currentlyOpen
            update()
        }

        position.renderStringsAndItems(display, posLabel = name)
    }
}
