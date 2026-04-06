package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.misc.tracker.generic.TimedTrackerConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.individual.TimedPerTrackerConfig
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.weekTextFormatter
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.buildSearchBox
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import at.hannibal2.skyhanni.utils.renderables.toRenderable
import at.hannibal2.skyhanni.utils.tracker.data.TimedTrackerData
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds

@Suppress("TooManyFunctions")
abstract class SkyhanniTimedTracker<Data : TimedTrackerData<*>>(name: String) : SkyHanniTracker<Data>(name) {

    abstract override val perTrackerConfig: TimedPerTrackerConfig<*>
    abstract override val storageAccessor: (ProfileSpecificStorage) -> Data

    private val timedConfig: TimedTrackerConfig
        get() = if (perTrackerConfig.useUniversalConfig) universalTracker.timedTracker else perTrackerConfig.timedTracker

    override val availableTrackers = super.availableTrackers + listOf(
        DisplayMode.DAY,
        DisplayMode.WEEK,
        DisplayMode.MONTH,
        DisplayMode.YEAR,
    )

    // TODO figure out why stopwatches don't stop running when swapping data, and fix it properly
    private val activeStopwatches = mutableSetOf<TimedTrackerData<*>>()

    @SkyHanniModule
    companion object {
        private val trackerSet: MutableSet<SkyhanniTimedTracker<*>> = mutableSetOf()

        @HandleEvent
        fun onConfigLoad() {
            trackerSet.forEach { it.cleanEntries() }
        }
    }

    init {
        if (timedConfig.resetSession) createNewSession()
        cleanEntries()
        trackerSet.add(this)
        update()
    }

    private fun cleanEntries() = getData()?.cleanEntries(timedConfig)

    @Suppress("UNCHECKED_CAST")
    override fun getSharedTracker() = ProfileStorageData.profileSpecific?.let { ps ->
        SharedTracker(
            availableTrackers.associateWith { ps.getData().getOrPutNewestData(it) as Data },
        )
    }

    override fun startSessionUptime() {
        super.startSessionUptime()
        getData()?.sessionContainer?.getAllCurrentData()?.let { activeStopwatches.addAll(it) }
    }

    override fun pauseSessionUptime() {
        super.pauseSessionUptime()
        activeStopwatches.forEach { it.getActiveStopwatch()?.pause(true) }
        activeStopwatches.clear()
    }

    fun resetCommand(displayMode: DisplayMode?, string: String?) = ChatUtils.clickableChat(
        "Are you sure you want to reset your $name? Click here to confirm.",
        onClick = { reset(displayMode, string) },
        "§eClick to confirm.",
        oneTimeClick = true,
    )

    private fun reset(displayMode: DisplayMode? = null, string: String? = null) {
        val data = getData() ?: return
        if (displayMode != null) data.reset(displayMode, string) else data.reset()
        ChatUtils.chat("Reset $name!")
        update()
    }

    private fun getData(): Data? = ProfileStorageData.profileSpecific?.getData()
    private fun ProfileSpecificStorage.getData(): Data = storageAccessor(this)

    @Suppress("UNCHECKED_CAST")
    private fun getCurrentData(): Data? = getData()?.getOrPutCurrentData(getDisplayMode()) as? Data

    override fun getDisplay(): List<Renderable> {
        val displayLines = getCurrentData()?.let { drawDisplayF(it) } ?: return emptyList()
        return if (trackerConfig.trackerSearchEnabled.get()) {
            buildFinalDisplay(displayLines.buildSearchBox(textInput))
        } else {
            buildFinalDisplay(Renderable.vertical(displayLines.toRenderable()))
        }
    }

    override fun buildFinalDisplay(searchBox: Renderable) = buildList {
        if (isInventoryOpen()) {
            buildSwitcherView()?.let { switcher ->
                add(Renderable.horizontal(switcher, spacing = 5, horizontalAlign = RenderUtils.HorizontalAlignment.CENTER))
            } ?: add(Renderable.placeholder(12))
            add(Renderable.vertical { getDisplayText() })
        } else {
            add(Renderable.placeholder(0, 22))
        }
        add(searchBox)
        if (showSessionUptime()) add(buildSessionUptime(getCurrentData()))
        if (isEmpty()) return@buildList
        if (isInventoryOpen()) {
            buildDisplayModeView()
            if (getDisplayMode() == DisplayMode.SESSION) {
                val data = getData() ?: return@buildList
                if (data.sessionContainer.isCurrent(DisplayMode.SESSION)) {
                    add(buildSessionCreateButton())
                } else {
                    add(Renderable.horizontal(buildSessionRestoreButton(), buildSessionDeleteButton(), spacing = 5))
                }
            }
        }
    }

    private fun getDisplayText(displayMode: DisplayMode = getDisplayMode(), string: String? = null): String {
        val container = getData()?.sessionContainer ?: return ""
        val resolved = string ?: container.resolveCurrentName(displayMode)
        val prefix = "§7${displayMode.alternateName}"
        val suffix = if (container.isCurrent(displayMode, resolved)) {
            "§a${displayMode.currentName}"
        } else {
            val weekString = (displayMode.toValue(resolved) as? LocalDate)?.format(weekTextFormatter) ?: resolved
            "§a${if (displayMode == DisplayMode.WEEK) weekString else resolved}"
        }
        return "$prefix: $suffix"
    }

    private fun buildSwitcherView(): List<Renderable>? {
        val container = getData()?.sessionContainer ?: return null
        val current = container.resolveCurrentName(getDisplayMode())
        val (previous, next) = container.getPrevNext(getDisplayMode(), current)
        val hasMoreAfterNext = next?.let { container.getPrevNext(getDisplayMode(), it).second } != null

        fun switcherButton(label: String, string: String) = Renderable.clickable(
            label,
            onLeftClick = { updateDisplay(string) },
            tips = listOf(getDisplayText(string = string)),
        )

        return listOfNotNull(
            previous?.let { switcherButton("§a[ §r§f§l<- §a]", it) },
            next?.let { switcherButton("§a[ §r§f§l-> §a]", it) },
            container.getMostRecentName(getDisplayMode())?.takeIf { hasMoreAfterNext }?.let {
                switcherButton("§a[ §r§f§l->> §r§a]", it)
            },
        )
    }

    private var sessionEditTime = SimpleTimeMark.farPast()

    private fun buildSessionCreateButton() = Renderable.clickable(
        "§cCreate New Session!",
        tips = listOf("§cThis will create a new", "§csession of", "§c$name"),
        onLeftClick = { if (sessionEditTime.passedSince() > 3.seconds) createSession() },
    )

    private fun buildSessionRestoreButton() = Renderable.clickable(
        "§c[Restore]",
        tips = listOf("§cThis will restore", "§cthis session of", "§c$name"),
        onLeftClick = {
            if (sessionEditTime.passedSince() > 3.seconds) {
                restoreSession()
                sessionEditTime = SimpleTimeMark.now()
            }
        },
    )

    private fun buildSessionDeleteButton() = Renderable.clickable(
        "§c[Delete]",
        tips = listOf("§cThis will delete", "§cthis session of", "§c$name"),
        onLeftClick = {
            if (sessionEditTime.passedSince() > .5.seconds) {
                deleteSession()
                sessionEditTime = SimpleTimeMark.now()
            }
        },
    )

    private fun createSession() {
        val data = getData() ?: return
        val string = ((data.sessionContainer.getMostRecentName(DisplayMode.SESSION)?.toIntOrNull() ?: 1) + 1).toString()
        data.getOrPutEntry(DisplayMode.SESSION, string)
        data.cleanEntry(timedConfig, DisplayMode.SESSION)
        sessionEditTime = SimpleTimeMark.now()
        update()
    }

    private fun deleteSession() {
        val data = getData() ?: return
        val container = data.sessionContainer
        val currentName = container.getCurrentName(DisplayMode.SESSION) ?: return
        if (currentName == container.getMostRecentName(DisplayMode.SESSION)) return
        container.deleteEntry(DisplayMode.SESSION, currentName)
        update()
    }

    private fun restoreSession() {
        val data = getData() ?: return
        val container = data.sessionContainer
        val string = ((container.getMostRecentName(DisplayMode.SESSION)?.toIntOrNull() ?: 1) + 1).toString()
        val sessionData = container.getCurrentName(DisplayMode.SESSION)
            ?.let { container.deleteEntry(DisplayMode.SESSION, it) } ?: return
        container.sessions.getOrPut(DisplayMode.SESSION) { mutableMapOf() }[string] = sessionData
        container.setCurrentName(DisplayMode.SESSION, null)
        update()
    }

    private fun updateDisplay(string: String, displayMode: DisplayMode = getDisplayMode()) {
        getData()?.sessionContainer?.setCurrentName(displayMode, string)
        update()
    }
}
