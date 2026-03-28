package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.misc.tracker.TopLevelTrackerConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.generic.TrackerSettings
import at.hannibal2.skyhanni.config.features.misc.tracker.individual.PerTrackerConfig
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.TrackerManager
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.ReflectionUtils.findGenericSuperclassTypeArgument
import at.hannibal2.skyhanni.utils.RenderDisplayConfig
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
import at.hannibal2.skyhanni.utils.tracker.data.TrackerData
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Suppress("TooManyFunctions")
abstract class SkyHanniTracker<Data : TrackerData<*>>(private val staticName: String) {

    // This is needed because of Slayer Profit Tracker
    open val name get() = staticName

    internal abstract val storageAccessor: (ProfileSpecificStorage) -> Data
    internal abstract val config: TopLevelTrackerConfig

    internal open val perTrackerConfig: PerTrackerConfig<*> get() = config.perTrackerConfig
    internal open val trackerConfig: TrackerSettings
        get() = perTrackerConfig.let {
            if (it.useUniversalConfig) universalTracker
            else it.trackerConfig
        }

    internal open val trackUptime: Boolean = true
    internal open val customUptimeControl: Boolean = false

    /**
     * Controls when this tracker's display is rendered.
     *
     * Override to customize any combination of inventory presence, island filtering,
     * or an enable condition without needing to override six separate vals.
     */
    open val renderConfig: RenderDisplayConfig = RenderDisplayConfig()

    /**
     * Called at the start of each render pass. Return false to suppress rendering entirely.
     *
     * Override in subclasses that have additional suppression conditions beyond the standard
     * [RenderDisplayConfig] gates (e.g. hiding when an overlay is visible, or when outside
     * an inventory). The base implementation always returns true.
     */
    protected open fun shouldRender(): Boolean = true

    internal open val extraDisplayModes: Map<DisplayMode, (ProfileSpecificStorage) -> Data> = emptyMap()

    private var displayMode: DisplayMode? = null
    private val currentSessions = mutableMapOf<ProfileSpecificStorage, Data>()
    private var display = emptyList<Renderable>()
    private var sessionResetTime = SimpleTimeMark.farPast()
    private var wasSearchEnabled = trackerConfig.trackerSearchEnabled.get()
    private var dirty = false
    protected val textInput = SearchTextInput()
    private var lastUpdate: SimpleTimeMark = SimpleTimeMark.farPast()

    // Separate detector used only to drive isInventoryOpen(); RenderDisplayHelper manages
    // its own inventory detection internally for the render condition.
    private val inventoryDetector = InventoryDetector(
        { update() },
        { update() },
    ) { true }

    init {
        RenderDisplayHelper(renderConfig) {
            renderDisplay(config.position)
        }
    }

    @SkyHanniModule
    companion object {
        internal val universalTracker get() = SkyHanniMod.feature.misc.tracker
        internal val storedTrackers get() = SkyHanniMod.feature.storage.trackerDisplayModes
        private val unpausedTrackers: MutableSet<SkyHanniTracker<*>> = mutableSetOf()

        @HandleEvent
        fun onTick(event: SkyHanniTickEvent) {
            if (!event.isMod(10)) return
            unpausedTrackers.toList().forEach { tracker ->
                if (tracker.trackUptime) tracker.checkAfk()
            }
        }
    }

    /**
     * A snapshot of all [DisplayMode] data instances for the current profile.
     *
     * Callers use [modify] and [get] rather than accessing tracker storage directly,
     * so that uptime tracking and dirty-flagging are applied consistently.
     */
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

    internal abstract fun drawDisplayF(data: Data): List<Searchable>
    internal open fun extraOnRender() = Unit

    private val dataCtor by lazy {
        findGenericSuperclassTypeArgument<SkyHanniTracker<*>, Data>().getConstructor()
    }

    internal fun createNewSession() = dataCtor.newInstance()
    internal val storage: Data? get() = ProfileStorageData.profileSpecific?.let(storageAccessor)

    fun getPricePer(name: NeuInternalName) = name.getPrice(trackerConfig.priceSource)
    fun getPricePerOrNull(name: NeuInternalName) = name.getPriceOrNull(trackerConfig.priceSource)
    fun isInventoryOpen() = inventoryDetector.isInside()

    open fun resetCommand() = ChatUtils.clickableChat(
        "Are you sure you want to reset your total $name? Click here to confirm.",
        onClick = { reset(DisplayMode.TOTAL, "Reset total $name!") },
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

    fun firstUpdate() = if (display.isEmpty()) update() else Unit

    private fun renderDisplay(position: at.hannibal2.skyhanni.config.core.config.Position) {
        if (!shouldRender()) return

        val searchEnabled = trackerConfig.trackerSearchEnabled.get()
        if (dirty || TrackerManager.dirty || searchEnabled != wasSearchEnabled) {
            display = getDisplay()
            dirty = false
        }
        wasSearchEnabled = searchEnabled
        position.renderRenderables(display, posLabel = name)
    }

    fun update() {
        dirty = true
    }

    protected open fun getDisplay() = getSharedTracker()?.let {
        val data = it.get(getDisplayMode())
        val displayLines = drawDisplayF(data)
        if (trackerConfig.trackerSearchEnabled.get()) buildFinalDisplay(displayLines.buildSearchBox(textInput))
        else buildFinalDisplay(Renderable.vertical(displayLines.toRenderable()))
    }.orEmpty()

    protected open fun buildFinalDisplay(searchBox: Renderable) = buildList {
        add(searchBox)
        if (isEmpty()) return@buildList
        if (showSessionUptime()) add(buildSessionUptime())
        if (isInventoryOpen()) {
            buildDisplayModeView()
            if (getDisplayMode() == DisplayMode.SESSION) add(buildSessionResetButton())
        }
    }

    internal fun showSessionUptime() =
        trackerConfig.showUptime.get() && (!trackerConfig.onlyShowSession.get() || displayMode != DisplayMode.TOTAL)

    private fun checkAfk() {
        if (getCurrentStopwatch()?.isPaused() == true) return
        val sharedTracker = getSharedTracker() ?: return
        val afkTime = sharedTracker.get(DisplayMode.TOTAL).getActiveStopwatch()?.getLapTime()
        if (afkTime == null || afkTime > trackerConfig.afkTimeout.seconds) {
            pauseSessionUptime()
            return
        }
        update()
    }

    private fun DisplayMode?.getDisplayModeTracker() = this?.let { getSharedTracker()?.get(it) }
    fun getTotalUptime(): Duration? = displayMode?.getDisplayModeTracker()?.getTotalUptime()
    fun getCurrentStopwatch(): Stopwatch? = displayMode?.getDisplayModeTracker()?.getActiveStopwatch()

    open fun startSessionUptime() {
        if (!this.trackUptime) return
        val sharedTracker = getSharedTracker() ?: return
        sharedTracker.modify { it.getActiveStopwatch()?.start(true) }
        if (!customUptimeControl) unpausedTrackers.add(this)
        update()
    }

    open fun pauseSessionUptime() {
        if (!this.trackUptime) return
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

    fun isPaused(): Boolean = getCurrentStopwatch()?.isPaused() == true

    fun buildSessionUptime(tracker: Data? = displayMode?.getDisplayModeTracker()): Renderable {
        val sessionUptime = tracker?.getTotalUptime() ?: return Renderable.empty()
        val isTotalDisplay = displayMode == DisplayMode.TOTAL
        val pausedText = if (getCurrentStopwatch()?.isPaused() == true) " §c(Paused!)" else ""
        val sessionList = buildList {
            displayMode?.getDisplayModeTracker()?.getSessionMap()?.entries?.forEach {
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

    protected open val availableTrackers = listOf(DisplayMode.TOTAL, DisplayMode.SESSION) + this.extraDisplayModes.keys

    protected open fun MutableList<Renderable>.buildDisplayModeView() {
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
        trackerConfig.defaultDisplayMode.get().mode ?: storedTrackers[name] ?: DisplayMode.TOTAL
    }.also { displayMode = it }

    protected open fun getSharedTracker() = ProfileStorageData.profileSpecific?.let { ps ->
        SharedTracker(
            mapOf(
                DisplayMode.TOTAL to storageAccessor(ps),
                DisplayMode.SESSION to currentSessions.getOrPut(ps) { dataCtor.newInstance() },
            ) + extraDisplayModes.mapValues { it.value(ps) },
        )
    }

    protected fun buildSessionResetButton() = Renderable.clickable(
        "§cReset session!",
        tips = listOf("§cThis will reset your", "§ccurrent session of", "§c$name"),
        onLeftClick = {
            if (sessionResetTime.passedSince() > 3.seconds) {
                reset(DisplayMode.SESSION, "Reset this session of $name!")
                sessionResetTime = SimpleTimeMark.now()
            }
        },
    )

    private fun reset(displayMode: DisplayMode, message: String) = getSharedTracker()?.let {
        it.get(displayMode).reset()
        ChatUtils.chat(message)
        update()
    }
}
