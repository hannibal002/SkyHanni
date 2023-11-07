package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.config.Storage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.utils.tracker.TrackerUtils.addDisplayModeToggle
import at.hannibal2.skyhanni.utils.tracker.TrackerUtils.addSessionResetButton

class SkyHanniTracker<Data : TrackerData>(
    val name: String,
    private val currentSessionData: Data,
    val getStorage: (Storage.ProfileSpecific) -> Data,
    val update: () -> Unit,
) {

    private fun getSharedTracker(): SharedTracker<Data>? {
        val profileSpecific = ProfileStorageData.profileSpecific ?: return null
        return SharedTracker(getStorage(profileSpecific), currentSessionData)
    }

    fun addSessionResetButton(list: MutableList<List<Any>>, inventoryOpen: Boolean) {
        if (inventoryOpen && TrackerUtils.currentDisplayMode == DisplayMode.CURRENT) {
            list.addSessionResetButton(name, getSharedTracker()) {
                update()
            }
        }
    }

    fun addDisplayModeToggle(list: MutableList<List<Any>>, inventoryOpen: Boolean) {
        if (inventoryOpen) {
            list.addDisplayModeToggle {
                update()
            }
        }
    }

    fun currentDisplay() = getSharedTracker()?.getCurrent()

    fun resetCommand(args: Array<String>, command: String) {
        TrackerUtils.resetCommand(name, command, args, getSharedTracker()) {
            update()
        }
    }

    fun modify(modifyFunction: (Data) -> Unit) {
        getSharedTracker()?.modify(modifyFunction)
    }
}
