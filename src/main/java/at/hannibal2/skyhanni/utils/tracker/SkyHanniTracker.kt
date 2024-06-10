package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.TrackerConfig.PriceFromEntry
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.TrackerManager
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi.getBazaarData
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValue
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getNpcPriceOrNull
import at.hannibal2.skyhanni.utils.NEUItems.getPriceOrNull
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiInventory
import kotlin.time.Duration.Companion.seconds

open class SkyHanniTracker<Data : TrackerData>(
    val name: String,
    private val createNewSession: () -> Data,
    private val getStorage: (ProfileSpecificStorage) -> Data,
    private val drawDisplay: (Data) -> List<List<Any>>,
) {

    private var inventoryOpen = false
    private var displayMode: DisplayMode? = null
    private val currentSessions = mutableMapOf<ProfileSpecificStorage, Data>()
    private var display = emptyList<List<Any>>()
    private var sessionResetTime = SimpleTimeMark.farPast()
    private var dirty = false

    companion object {

        val config get() = SkyHanniMod.feature.misc.tracker
        private val storedTrackers get() = SkyHanniMod.feature.storage.trackerDisplayModes

        fun getPricePer(name: NEUInternalName) = when (config.priceFrom) {
            PriceFromEntry.INSTANT_SELL -> name.getBazaarData()?.instantBuyPrice ?: name.getPriceOrNull() ?: 0.0
            PriceFromEntry.SELL_OFFER -> name.getBazaarData()?.sellOfferPrice ?: name.getPriceOrNull() ?: 0.0

            else -> name.getNpcPriceOrNull() ?: 0.0
        }
    }

    fun isInventoryOpen() = inventoryOpen

    fun resetCommand() = ChatUtils.clickableChat(
        "Are you sure you want to reset your total $name? Click here to confirm.",
        onClick = {
            reset(DisplayMode.TOTAL, "Reset total $name!")
        },
        oneTimeClick = true
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

        val currentlyOpen = Minecraft.getMinecraft().currentScreen is GuiInventory
        if (!currentlyOpen && config.hideItemTrackersOutsideInventory && this is SkyHanniItemTracker) {
            return
        }
        if (inventoryOpen != currentlyOpen) {
            inventoryOpen = currentlyOpen
            update()
        }

        if (dirty || TrackerManager.dirty) {
            display = getSharedTracker()?.let {
                buildFinalDisplay(drawDisplay(it.get(getDisplayMode())))
            } ?: emptyList()
            dirty = false
        }

        position.renderStringsAndItems(display, posLabel = name)
    }

    fun update() {
        dirty = true
    }

    private fun buildFinalDisplay(rawList: List<List<Any>>) = rawList.toMutableList().also {
        if (it.isEmpty()) return@also
        if (inventoryOpen) {
            it.add(1, buildDisplayModeView())
        }
        if (inventoryOpen && getDisplayMode() == DisplayMode.SESSION) {
            it.addAsSingletonList(buildSessionResetButton())
        }
    }

    private fun buildSessionResetButton() = Renderable.clickAndHover(
        "§cReset session!",
        listOf(
            "§cThis will reset your",
            "§ccurrent session of",
            "§c$name"
        ),
        onClick = {
            if (sessionResetTime.passedSince() > 3.seconds) {
                reset(DisplayMode.SESSION, "Reset this session of $name!")
                sessionResetTime = SimpleTimeMark.now()
            }
        })

    private fun buildDisplayModeView() = LorenzUtils.buildSelector<DisplayMode>(
        "§7Display Mode: ",
        getName = { type -> type.displayName },
        isCurrent = { it == getDisplayMode() },
        onChange = {
            displayMode = it
            storedTrackers[name] = it
            update()
        }
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
