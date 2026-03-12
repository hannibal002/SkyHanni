package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.tracker.GenericIndividualTrackerConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.ItemTrackerGenericConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.TrackerGenericConfig
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.TrackerManager
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValue
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.Stopwatch
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addAll
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableNullableButton
import at.hannibal2.skyhanni.utils.renderables.SearchTextInput
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.buildSearchBox
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.empty
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.renderables.toRenderable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Suppress("TooManyFunctions")
open class SkyHanniTracker<Data : TrackerData<*>, Config : GenericIndividualTrackerConfig<*>>(
    val name: String,
    private val createNewSession: () -> Data,
    private val getStorage: (ProfileSpecificStorage) -> Data,
    private val extraDisplayModes: Map<DisplayMode, (ProfileSpecificStorage) -> Data> = emptyMap(),
    private val trackUptime: Boolean = true,
    private val trackerConfig: () -> Config,
    private val customUptimeControl: Boolean = false,
    private val drawDisplay: (Data) -> List<Searchable>,
    private val position: () -> Position,
    private val inventory: InventoryDetector = RenderDisplayHelper.NO_INVENTORY,
    private val onlyOnIsland: IslandType? = null,
    private val condition: () -> Boolean,
) {
    val trackerSpecificConfig: Config get() = trackerConfig()
    private val config: TrackerGenericConfig get() =
        if (trackerSpecificConfig.useUniversalConfig) universalTracker else trackerSpecificConfig.trackerConfig
    private var displayMode: DisplayMode? = null
    private val currentSessions = mutableMapOf<ProfileSpecificStorage, Data>()
    private var display = emptyList<Renderable>()
    private var sessionResetTime = SimpleTimeMark.farPast()
    private var wasSearchEnabled = config.trackerSearchEnabled.get()
    private var dirty = false
    private var lastUpdate: SimpleTimeMark = SimpleTimeMark.farPast()
    val textInput = SearchTextInput()

    init {
        RenderDisplayHelper(
            inventory = inventory,
            outsideInventory = true,
            inOwnInventory = true,
            condition = condition,
            onlyOnIsland = onlyOnIsland,
            onRender = { renderDisplay(position()) },
        )
    }

    @SkyHanniModule
    companion object {
        private val universalTracker get() = SkyHanniMod.feature.misc.tracker
        private val storedTrackers get() = SkyHanniMod.feature.storage.trackerDisplayModes
        private val unpausedTrackers: MutableSet<SkyHanniTracker<*, *>> = mutableSetOf()

        @HandleEvent
        fun onTick(event: SkyHanniTickEvent) {
            if (!event.isMod(10)) return
            unpausedTrackers.toList().forEach { tracker ->
                if (tracker.trackUptime) tracker.checkAfk()
            }
        }
    }

    fun getPricePer(name: NeuInternalName) = name.getPrice(config.priceSource)
    fun getPricePerOrNull(name: NeuInternalName) = name.getPriceOrNull(config.priceSource)
    fun isInventoryOpen() = inventoryDetector.isInside()
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
        getSharedTracker()?.modify(mode, modifyFunction)
        update()
    }

    fun modifyEachMode(modifyFunction: (Data) -> Unit) {
        val sharedTracker = getSharedTracker() ?: return
        DisplayMode.entries.forEach { mode ->
            sharedTracker.tryModify(mode, modifyFunction)
        }
        update()
    }

    private val hideInEstimatedValue get() = (config as? ItemTrackerGenericConfig)?.itemTracker?.hideInEstimatedItemValue ?: false
    private val hideOutsideInventory get() = (config as? ItemTrackerGenericConfig)?.itemTracker?.hideOutsideInventory ?: false
    private val inventoryDetector = InventoryDetector(
        { update() },
        { update() },
    ) { true }

    private fun renderDisplay(position: Position) {
        if (hideInEstimatedValue && EstimatedItemValue.isCurrentlyShowing()) return
        if (!InventoryUtils.inAnyInventory() && hideOutsideInventory && this is SkyHanniItemTracker) return

        val searchEnabled = config.trackerSearchEnabled.get()
        if (dirty || TrackerManager.dirty || searchEnabled != wasSearchEnabled) {
            display = getSharedTracker()?.let {
                val data = it.get(getDisplayMode())
                val searchables = drawDisplay(data)
                val content = if (searchEnabled) searchables.buildSearchBox(textInput)
                else Renderable.vertical(searchables.toRenderable())
                buildFinalDisplay(content)
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
        if (isInventoryOpen()) {
            buildDisplayModeView()
            if (getDisplayMode() == DisplayMode.SESSION) add(buildSessionResetButton())
        }
    }

    private fun showSessionUptime() =
        config.showUptime.get() && (!config.onlyShowSession.get() || displayMode != DisplayMode.TOTAL)

    private fun checkAfk() {
        if (getCurrentStopwatch()?.isPaused() == true) return
        val sharedTracker = getSharedTracker() ?: return
        val afkTime = sharedTracker.get(DisplayMode.TOTAL).getActiveStopwatch()?.getLapTime()
        if (afkTime == null || afkTime > config.afkTimeout.seconds) {
            pauseSessionUptime()
            return
        }
        update()
    }

    private fun getDisplayModeTracker() = displayMode?.let { getSharedTracker()?.get(it) }
    fun getTotalUptime(): Duration? = getDisplayModeTracker()?.getTotalUptime()
    fun getCurrentStopwatch(): Stopwatch? = getDisplayModeTracker()?.getActiveStopwatch()

    fun startSessionUptime() {
        if (!trackUptime) return
        val sharedTracker = getSharedTracker() ?: return
        sharedTracker.modify { it.getActiveStopwatch()?.start(true) }
        if (!customUptimeControl) unpausedTrackers.add(this)
        update()
    }

    fun pauseSessionUptime() {
        if (!trackUptime) return
        val sharedTracker = getSharedTracker() ?: return
        sharedTracker.modify { it.getActiveStopwatch()?.pause(true) }
        if (!customUptimeControl) unpausedTrackers.remove(this)
        update()
    }

    fun swapActiveSession(session: SessionUptime, swapExtraTime: Boolean = true) {
        if (!customUptimeControl) return
        val sharedTracker = getSharedTracker() ?: return
        sharedTracker.modify { it.setActiveStopwatch(session, swapExtraTime) }
        update()
    }

    private fun buildSessionUptime(): Renderable {
        val sessionUptime = getTotalUptime() ?: return Renderable.empty()
        val isTotalDisplay = displayMode == DisplayMode.TOTAL
        val pausedText = if (getCurrentStopwatch()?.isPaused() == true) " §c(Paused!)" else ""
        val sessionList = buildList {
            getDisplayModeTracker()?.getSessionMap()?.entries?.forEach {
                if (it.value.getDuration() > 0.seconds) add("${it.key} Uptime: ${it.value.getDuration().format()}")
            }
        }
        return Renderable.hoverTips(
            Renderable.text("§eTotal Uptime: §b${sessionUptime.format()}$pausedText"),
            tips = buildList {
                addAll(sessionList)
                if (!isTotalDisplay) return@buildList
                addAll(
                    "§eⓘ §7Uptime tracked only from",
                    "§7SkyHanni version 6.0.0 onwards",
                )
            },
        )
    }

    private fun buildSessionResetButton() = Renderable.clickable(
        "§cReset session!",
        tips = listOf("§cThis will reset your", "§ccurrent session of", "§c$name"),
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

    protected fun getDisplayMode() = displayMode ?: run {
        config.defaultDisplayMode.get().mode ?: storedTrackers[name] ?: DisplayMode.TOTAL
    }.also { displayMode = it }

    protected fun getSharedTracker() = ProfileStorageData.profileSpecific?.let { ps ->
        SharedTracker(
            mapOf(
                DisplayMode.TOTAL to getStorage(ps),
                DisplayMode.SESSION to currentSessions.getOrPut(ps) { createNewSession() },
            ) + extraDisplayModes.mapValues { it.value(ps) },
        )
    }

    private fun reset(displayMode: DisplayMode, message: String) = getSharedTracker()?.let {
        it.get(displayMode).reset()
        ChatUtils.chat(message)
        update()
    }

    fun firstUpdate() = if (display.isEmpty()) update() else Unit

    inner class SharedTracker<Data : TrackerData<*>>(
        private val entries: Map<DisplayMode, Data>,
    ) {
        fun modify(mode: DisplayMode, modifyFunction: (Data) -> Unit) = get(mode).let(modifyFunction)
        fun tryModify(mode: DisplayMode, modifyFunction: (Data) -> Unit) = entries[mode]?.let(modifyFunction)
        fun modify(modifyFunction: (Data) -> Unit) = entries.values.forEach(modifyFunction)

        fun get(displayMode: DisplayMode) = entries[displayMode] ?: ErrorManager.skyHanniError(
            "Unregistered display mode accessed on tracker",
            "tracker" to name,
            "displayMode" to displayMode,
            "availableModes" to entries.keys,
        )
    }

    enum class DisplayMode(private val displayName: String, val shortenedName: String = displayName) {
        TOTAL("Total"),
        SESSION("This Session", "Session"),
        MAYOR("This Mayor", "Mayor"),
        ;

        override fun toString() = displayName
    }

    enum class DefaultDisplayMode(val display: String, val mode: DisplayMode?) {
        TOTAL("Total", DisplayMode.TOTAL),
        SESSION("This Session", DisplayMode.SESSION),
        REMEMBER_LAST("Remember Last", null),
        ;

        override fun toString() = display
    }
}
