package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.config.Storage
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiInventory
import kotlin.time.Duration.Companion.seconds

class SkyHanniTracker<Data : TrackerData>(
    private val name: String,
    private val createNewSession: () -> Data,
    private val getStorage: (Storage.ProfileSpecific) -> Data,
    private val drawDisplay: (Data) -> List<List<Any>>,
) {
    private var inventoryOpen = false
    private var displayMode = DisplayMode.TOTAL
    private val currentSessions = mutableMapOf<Storage.ProfileSpecific, Data>()
    private var display = emptyList<List<Any>>()
    private var sessionResetTime = SimpleTimeMark.farPast()

    fun isInventoryOpen() = inventoryOpen

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
        update()
    }

    fun renderDisplay(position: Position) {
        val currentlyOpen = Minecraft.getMinecraft().currentScreen is GuiInventory
        if (inventoryOpen != currentlyOpen) {
            inventoryOpen = currentlyOpen
            update()
        }

        position.renderStringsAndItems(display, posLabel = name)
    }

    fun update() {
        display = buildFinalDisplay()
    }

    private fun buildFinalDisplay(): List<List<Any>> {
        val data = currentDisplay() ?: return emptyList()

        return drawDisplay(data).toMutableList().also {
            if (inventoryOpen) {
                it.add(1, buildDisplayModeView())
            }
            if (inventoryOpen && displayMode == DisplayMode.SESSION) {
                it.addAsSingletonList(buildSessionResetButton())
            }
        }
    }

    private fun buildSessionResetButton() = Renderable.clickAndHover(
        "§cReset session!",
        listOf(
            "§cThis will reset your",
            "§ccurrent session of",
            "§c$name"
        ),
    ) {
        if (sessionResetTime.passedSince() > 3.seconds) {
            reset(DisplayMode.SESSION, "§e[SkyHanni] Reset this session of $name!")
            sessionResetTime = SimpleTimeMark.now()
        }
    }

    private fun buildDisplayModeView() = LorenzUtils.buildSelector<DisplayMode>(
        "§7Display Mode: ",
        getName = { type -> type.displayName },
        isCurrent = { it == displayMode },
        onChange = {
            displayMode = it
            update()
        }
    )

    private fun currentDisplay() = getSharedTracker()?.get(displayMode)

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
