package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.config.Storage
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.addSelector
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiInventory

class SkyHanniTracker<Data : TrackerData>(
    private val name: String,
    private val createNewSession: () -> Data,
    private val getStorage: (Storage.ProfileSpecific) -> Data,
    private val update: () -> Unit,
) {
    private var inventoryOpen = false
    private var displayMode = DisplayMode.TOTAL
    private val currentSessions = mutableMapOf<Storage.ProfileSpecific, Data>()

    fun isInventoryOpen() = inventoryOpen

    fun addSessionResetButton(list: MutableList<List<Any>>) {
        if (!inventoryOpen || displayMode != DisplayMode.SESSION) return

        list.addAsSingletonList(
            Renderable.clickAndHover(
                "§cReset session!",
                listOf(
                    "§cThis will reset your",
                    "§ccurrent session of",
                    "§c$name"
                ),
            ) {
                reset(DisplayMode.SESSION, "§e[SkyHanni] Reset this session of $name!")
            })
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
        if (args.size == 1 && args[0].lowercase() == "confirm") {
            reset(DisplayMode.TOTAL, "§e[SkyHanni] Reset total $name!")
            return
        }

        LorenzUtils.clickableChat(
            "§e[SkyHanni] Are you sure you want to reset your total $name? Click here to confirm.",
            "$command confirm"
        )
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

    private fun getSharedTracker(): SharedTracker<Data>? {
        val profileSpecific = ProfileStorageData.profileSpecific ?: return null
        return SharedTracker(getStorage(profileSpecific), getCurrentSession(profileSpecific))
    }

    private fun getCurrentSession(profileSpecific: Storage.ProfileSpecific) =
        currentSessions.getOrPut(profileSpecific) { createNewSession() }

    private fun reset(displayMode: DisplayMode, message: String) {
        getSharedTracker()?.get(displayMode)?.let {
            it.reset()
            LorenzUtils.chat(message)
            update()
        }
    }

    class SharedTracker<Data : TrackerData>(private val total: Data, private val currentSession: Data) {
        fun modify(modifyFunction: (Data) -> Unit) {
            modifyFunction(total)
            modifyFunction(currentSession)
        }

        fun get(displayMode: DisplayMode) = when (displayMode) {
            DisplayMode.TOTAL -> total
            DisplayMode.SESSION -> currentSession
        }
    }

    enum class DisplayMode(val displayName: String) {
        TOTAL("Total"),
        SESSION("This Session"),
        ;
    }

}
