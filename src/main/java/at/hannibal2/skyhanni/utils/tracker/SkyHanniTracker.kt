package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.config.Storage
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.tracker.TrackerUtils.addDisplayModeToggle
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

    fun isInventoryOpen() = inventoryOpen

    private fun getSharedTracker(): SharedTracker<Data>? {
        val profileSpecific = ProfileStorageData.profileSpecific ?: return null
        return SharedTracker(getStorage(profileSpecific), currentSessionData)
    }

    fun addSessionResetButton(list: MutableList<List<Any>>) {
        if (inventoryOpen && TrackerUtils.currentDisplayMode == DisplayMode.SESSION) {
            list.addSessionResetButton(name, getSharedTracker()) {
                update()
            }
        }
    }

    fun addDisplayModeToggle(list: MutableList<List<Any>>, closedText: String? = null) {
        if (inventoryOpen) {
            list.addDisplayModeToggle {
                update()
            }
        } else {
            closedText?.let {
                list.addAsSingletonList(it)
            }
        }
    }

    fun currentDisplay() = getSharedTracker()?.get(TrackerUtils.currentDisplayMode)

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
