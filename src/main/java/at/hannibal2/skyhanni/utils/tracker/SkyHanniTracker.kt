package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.RenderData
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.TrackerManager
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValue
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemPriceSource
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.Stopwatch
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addButton
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableNullableButton
import at.hannibal2.skyhanni.utils.renderables.SearchTextInput
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.buildSearchBox
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.empty
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.renderables.toRenderable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Suppress("TooManyFunctions")
open class SkyHanniTracker<Data : TrackerData>(
    val name: String,
    private val createNewSession: () -> Data,
    private val getStorage: (ProfileSpecificStorage) -> Data,
    private val extraDisplayModes: Map<DisplayMode, (ProfileSpecificStorage) -> Data> = emptyMap(),
    private val trackUptime: Boolean = true,
    private val customUptimeControl: Boolean = false,
    private val drawDisplay: (Data) -> List<Searchable>,
) {
    private var inventoryOpen = false
    private var displayMode: DisplayMode? = null
    private val currentSessions = mutableMapOf<ProfileSpecificStorage, Data>()
    private var display = emptyList<Renderable>()
    private var sessionResetTime = SimpleTimeMark.farPast()
    private var wasSearchEnabled = config.trackerSearchEnabled.get()
    private var dirty = false
    private var lastUpdate: SimpleTimeMark = SimpleTimeMark.farPast()
    val textInput = SearchTextInput()

    @SkyHanniModule
    companion object {

        private val config get() = SkyHanniMod.feature.misc.tracker
        private val storedTrackers get() = SkyHanniMod.feature.storage.trackerDisplayModes
        private val unpausedTrackers: MutableSet<SkyHanniTracker<*>> = mutableSetOf()

        @HandleEvent
        fun onTick(event: SkyHanniTickEvent) {
            if (!event.isMod(10)) return

            unpausedTrackers.toList().forEach { tracker ->
                if (tracker.trackUptime) {
                    tracker.checkAfk()
                }
            }
        }

        fun getPricePer(name: NeuInternalName) = name.getPrice(config.priceSource)
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
        val sharedTracker = getSharedTracker() ?: return
        sharedTracker.modify(modifyFunction)
        startSessionUptime()
        lastUpdate = SimpleTimeMark.now()
        update()
    }

    fun modify(mode: DisplayMode, modifyFunction: (Data) -> Unit) {
        val sharedTracker = getSharedTracker() ?: return
        sharedTracker.modify(mode, modifyFunction)
        update()
    }

    fun modifyEachMode(modifyFunction: (Data) -> Unit) {
        val sharedTracker = getSharedTracker() ?: return
        DisplayMode.entries.forEach { mode ->
            sharedTracker.tryModify(mode, modifyFunction)
        }
        update()
    }

    fun renderDisplay(position: Position) {
        if (config.hideInEstimatedItemValue && EstimatedItemValue.isCurrentlyShowing()) return

        var currentlyOpen = Minecraft.getInstance().screen?.let { it is InventoryScreen || it is ContainerScreen } ?: false
        if (!currentlyOpen && config.hideOutsideInventory && this is SkyHanniItemTracker) {
            return
        }
        if (RenderData.outsideInventory) {
            currentlyOpen = false
        }
        if (inventoryOpen != currentlyOpen) {
            inventoryOpen = currentlyOpen
            update()
        }

        val searchEnabled = config.trackerSearchEnabled.get()
        if (dirty || TrackerManager.dirty || (searchEnabled != wasSearchEnabled)) {
            display = getSharedTracker()?.let {
                val data = it.get(getDisplayMode())
                val searchables = drawDisplay(data)
                if (config.trackerSearchEnabled.get()) buildFinalDisplay(searchables.buildSearchBox(textInput))
                else buildFinalDisplay(Renderable.vertical(searchables.toRenderable()))
            }.orEmpty()
            dirty = false
        }
        wasSearchEnabled = searchEnabled

        position.renderRenderables(display, posLabel = name)
    }

    fun update() {
        dirty = true
    }

    private fun buildFinalDisplay(searchBox: Renderable) = buildList {
        add(searchBox)
        if (isEmpty()) return@buildList
        if (showSessionUptime()) add(buildSessionUptime())
        if (inventoryOpen) {
            buildDisplayModeView()
            if (getDisplayMode() == DisplayMode.SESSION) {
                add(buildSessionResetButton())
            }
        }
    }

    private fun showSessionUptime(): Boolean =
        config.showUptime.get() && (!config.onlyShowSession.get() || displayMode != DisplayMode.TOTAL)

    private fun checkAfk() {
        if (getCurrentStopwatch()?.isPaused() == true) {
            return
        }
        val sharedTracker = getSharedTracker() ?: return
        // Afk time should be the same for all valid displays
        val afkTime = sharedTracker.get(DisplayMode.TOTAL).getActiveStopwatch()?.getLapTime()
        if (afkTime == null || afkTime > config.afkTimeout.seconds) {
            pauseSessionUptime()
            return
        }
        update()
    }

    fun getTotalUptime(): Duration? = displayMode?.let { getSharedTracker()?.get(it)?.getTotalUptime() }

    open fun getCurrentStopwatch(): Stopwatch? = displayMode?.let { getSharedTracker()?.get(it)?.getActiveStopwatch() }

    private fun startSessionUptime() {
        if (!this.trackUptime) return
        val sharedTracker = getSharedTracker() ?: return
        sharedTracker.modify { it.getActiveStopwatch()?.start(true) }
        if (!customUptimeControl) unpausedTrackers.add(this)
        update()
    }

    private fun pauseSessionUptime() {
        if (!this.trackUptime) return
        val sharedTracker = getSharedTracker() ?: return
        sharedTracker.modify { it.getActiveStopwatch()?.pause(true) }
        if (!customUptimeControl) unpausedTrackers.remove(this)
        update()
    }

    private fun swapActiveSession(session: SessionUptime) {
        if (!this.customUptimeControl) return
        val sharedTracker = getSharedTracker() ?: return
        sharedTracker.modify { it.setActiveStopwatch(session) }
        update()
    }

    private fun buildSessionUptime(): Renderable {
        val sessionUptime = getTotalUptime() ?: return Renderable.empty()
        val isTotalDisplay = displayMode == DisplayMode.TOTAL
        val pausedText = if (getCurrentStopwatch()?.isPaused() == true) " §c(Paused!)" else ""
        // Uptime added after trackers already had data
        return if (isTotalDisplay) {
            Renderable.hoverTips(
                Renderable.text("§eTotal Uptime: §b${sessionUptime.format()}$pausedText"),
                tips = listOf(
                    "§eⓘ §7Uptime tracked only from",
                    "§7SkyHanni version 6.0.0 onwards",
                )
            )
        } else {
            Renderable.text("§eSession Uptime: §b${sessionUptime.format()}$pausedText")
        }
    }

    private fun buildSessionResetButton() = Renderable.clickable(
        "§cReset session!",
        tips = listOf(
            "§cThis will reset your",
            "§ccurrent session of",
            "§c$name",
        ),
        onLeftClick = {
            if (sessionResetTime.passedSince() > 3.seconds) {
                reset(DisplayMode.SESSION, "Reset this session of $name!")
                sessionResetTime = SimpleTimeMark.now()
            }
        },
    )

    private val availableTrackers = listOf(DisplayMode.TOTAL, DisplayMode.SESSION) + extraDisplayModes.keys

    private fun MutableList<Renderable>.buildDisplayModeView() {
        addRenderableNullableButton<DisplayMode>(
            label = "Display Mode",
            current = getDisplayMode(),
            onChange = { new ->
                if (new == null) return@addRenderableNullableButton
                displayMode = new
                storedTrackers[name] = new
                update()
            },
            universe = availableTrackers,
        )
    }

    protected fun getSharedTracker() = ProfileStorageData.profileSpecific?.let { ps ->
        SharedTracker(
            mapOf(
                DisplayMode.TOTAL to ps.getTotal(),
                DisplayMode.SESSION to ps.getCurrentSession(),
            ) + extraDisplayModes.mapValues { it.value(ps) },
        )
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

    protected fun getDisplayMode() = displayMode ?: run {
        val newValue = config.defaultDisplayMode.get().mode ?: storedTrackers[name] ?: DisplayMode.TOTAL
        displayMode = newValue
        newValue
    }

    fun firstUpdate() {
        if (display.isEmpty()) {
            update()
        }
    }

    fun initRenderer(
        position: () -> Position,
        inventory: InventoryDetector = RenderDisplayHelper.NO_INVENTORY,
        onlyOnIsland: IslandType? = null,
        condition: () -> Boolean,
    ) {
        RenderDisplayHelper(
            inventory,
            outsideInventory = true,
            inOwnInventory = true,
            condition = condition,
            onlyOnIsland = onlyOnIsland,
            onRender = {
                renderDisplay(position())
            },
        )
    }

    inner class SharedTracker<Data : TrackerData>(
        private val entries: Map<DisplayMode, Data>,
    ) {

        fun modify(mode: DisplayMode, modifyFunction: (Data) -> Unit) {
            get(mode).let(modifyFunction)
        }

        fun tryModify(mode: DisplayMode, modifyFunction: (Data) -> Unit) {
            entries[mode]?.let(modifyFunction)
        }

        fun modify(modifyFunction: (Data) -> Unit) {
            entries.values.forEach(modifyFunction)
        }

        fun get(displayMode: DisplayMode) = entries[displayMode] ?: ErrorManager.skyHanniError(
            "Unregistered display mode accessed on tracker",
            "tracker" to name,
            "displayMode" to displayMode,
            "availableModes" to entries.keys,
        )
    }

    fun handlePossibleRareDrop(internalName: NeuInternalName, amount: Int, message: Boolean = true) {
        val (itemName, price) = SlayerApi.getItemNameAndPrice(internalName, amount)
        if (config.warnings.chat && price >= config.warnings.minimumChat && message) {
            ChatUtils.chat("§a+Tracker Drop§7: §r$itemName")
        }
        if (config.warnings.title && price >= config.warnings.minimumTitle) {
            TitleManager.sendTitle("§a+ $itemName", weight = price)
        }
    }

    fun addPriceFromButton(lists: MutableList<Searchable>) {
        if (isInventoryOpen()) {
            lists.addButton(
                label = "Price Source",
                current = config.priceSource,
                getName = { it.sellName },
                onChange = {
                    config.priceSource = it
                    update()
                },
                universe = ItemPriceSource.entries,
            )
        }
    }

    enum class DisplayMode(private val displayName: String, val shortenedName: String = displayName) {
        TOTAL("Total"),
        SESSION("This Session", "Session"),
        MAYOR("This Mayor", "Mayor"),
        ;

        override fun toString(): String = displayName
    }

    enum class DefaultDisplayMode(val display: String, val mode: DisplayMode?) {
        TOTAL("Total", DisplayMode.TOTAL),
        SESSION("This Session", DisplayMode.SESSION),
        REMEMBER_LAST("Remember Last", null),
        ;

        override fun toString() = display
    }
}
