package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.TrackerManager
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValue
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.buildSearchBox
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiInventory
import kotlin.time.Duration.Companion.seconds

open class SkyHanniTracker<Data : TrackerData>(
    val name: String,
    private val createNewSession: () -> Data,
    private val getStorage: (ProfileSpecificStorage) -> Data,
    private val drawDisplay: (Data) -> List<Searchable>,
) {

    private var inventoryOpen = false
    private var displayMode: DisplayMode? = null
    private val currentSessions = mutableMapOf<ProfileSpecificStorage, Data>()
    private var display = emptyList<Renderable>()
    private var sessionResetTime = SimpleTimeMark.farPast()
    private var dirty = false

    companion object {

        val config get() = SkyHanniMod.feature.misc.tracker
        private val storedTrackers get() = SkyHanniMod.feature.storage.trackerDisplayModes

        fun getPricePer(name: NEUInternalName) = name.getPrice(config.priceSource)
    }

    fun isInventoryOpen() = inventoryOpen

    fun resetCommand() = ChatUtils.clickableChat(
        "Are you sure you want to reset your total $name? Click here to confirm.",
        onClick = {
            reset(DisplayMode.TOTAL, "Reset total $name!")
        },
        "§eClick to confirm.",
        oneTimeClick = true,
    )

    fun modify(modifyFunction: (Data) -> Unit) {
        getSharedTracker()?.let {
            it.modify(modifyFunction)
            update()
        }
    }

    fun modify(mode: DisplayMode, modifyFunction: (Data) -> Unit) {
        val storage = ProfileStorageData.profileSpecific ?: return
        val data: Data = when (mode) {
            DisplayMode.TOTAL -> storage.getTotal()
            DisplayMode.SESSION -> storage.getCurrentSession()
        }
        modifyFunction(data)
        update()
    }

    fun renderDisplay(position: Position) {
        if (config.hideInEstimatedItemValue && EstimatedItemValue.isCurrentlyShowing()) return

        val currentlyOpen = Minecraft.getMinecraft().currentScreen?.let { it is GuiInventory || it is GuiChest } ?: false
        if (!currentlyOpen && config.hideItemTrackersOutsideInventory && this is SkyHanniItemTracker) {
            return
        }
        if (inventoryOpen != currentlyOpen) {
            inventoryOpen = currentlyOpen
            update()
        }

        if (dirty || TrackerManager.dirty) {
            display = getSharedTracker()?.let {
                val data = it.get(getDisplayMode())
                val searchables = drawDisplay(data)
                buildFinalDisplay(searchables.buildSearchBox())
            } ?: emptyList()
            dirty = false
        }

        position.renderRenderables(display, posLabel = name)
    }

    fun update() {
        dirty = true
    }

    private fun buildFinalDisplay(searchBox: Renderable) = buildList {
        add(searchBox)
        if (isEmpty()) return@buildList
        if (inventoryOpen) {
            add(buildDisplayModeView())
        }
        if (inventoryOpen && getDisplayMode() == DisplayMode.SESSION) {
            add(buildSessionResetButton())
        }
    }

    private fun buildSessionResetButton() = Renderable.clickAndHover(
        "§cReset session!",
        listOf(
            "§cThis will reset your",
            "§ccurrent session of",
            "§c$name",
        ),
        onClick = {
            if (sessionResetTime.passedSince() > 3.seconds) {
                reset(DisplayMode.SESSION, "Reset this session of $name!")
                sessionResetTime = SimpleTimeMark.now()
            }
        },
    )

    private fun buildDisplayModeView() = Renderable.horizontalContainer(
        CollectionUtils.buildSelector<DisplayMode>(
            "§7Display Mode: ",
            getName = { type -> type.displayName },
            isCurrent = { it == getDisplayMode() },
            onChange = {
                displayMode = it
                storedTrackers[name] = it
                update()
            },
        ),
    )

    protected fun getSharedTracker() = ProfileStorageData.profileSpecific?.let {
        SharedTracker(it.getTotal(), it.getCurrentSession())
    }

    private fun ProfileSpecificStorage.getCurrentSession() = currentSessions.getOrPut(this) { createNewSession() }

    private fun ProfileSpecificStorage.getTotal(): Data = getStorage(this)

    private fun reset(displayMode: DisplayMode, message: String) {
        getSharedTracker()?.let {
            it.get(displayMode).reset()
            ChatUtils.chat(message)
            update()
        }
    }

    private fun getDisplayMode() = displayMode ?: run {
        val newValue = config.defaultDisplayMode.get().mode ?: storedTrackers[name] ?: DisplayMode.TOTAL
        displayMode = newValue
        newValue
    }

    fun firstUpdate() {
        if (display.isEmpty()) {
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

    enum class DefaultDisplayMode(val display: String, val mode: DisplayMode?) {
        TOTAL("Total", DisplayMode.TOTAL),
        SESSION("This Session", DisplayMode.SESSION),
        REMEMBER_LAST("Remember Last", null),
        ;

        override fun toString() = display
    }
}
