package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.misc.tracker.TimedTrackerConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.TopLevelTrackerConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.generic.TrackerGenericConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.individual.TimedGenericIndividualTrackerConfig
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.weekTextFormatter
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.buildSearchBox
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import at.hannibal2.skyhanni.utils.renderables.toRenderable
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds

@Suppress("SpreadOperator", "TooManyFunctions")
abstract class SkyhanniTimedTracker<Data : TimedTrackerData<*>>(name: String) : SkyHanniTracker<Data>(name) {
    abstract override val perTrackerConfig: TimedGenericIndividualTrackerConfig<*>
    abstract override val storageAccessor: (ProfileSpecificStorage) -> Data

    private val timedConfig: TimedTrackerConfig get() =
        if (perTrackerConfig.useUniversalConfig) universalTracker.timedTracker
        else perTrackerConfig.timedTracker

    override val availableTrackers = super.availableTrackers + listOf(
        DisplayMode.DAY,
        DisplayMode.WEEK,
        DisplayMode.MONTH,
        DisplayMode.YEAR,
    )

    private val activeStopwatches = mutableSetOf<Data>()

    @SkyHanniModule
    companion object {
        private val trackerSet: MutableSet<SkyhanniTimedTracker<*>> = mutableSetOf()

        @HandleEvent
        fun onConfigLoad() {
            trackerSet.forEach { it.cleanEntries() }
        }
    }

    init {
        if (timedConfig.resetSession) {
            createNewSession()
        }
        cleanEntries()
        add()
        update()
    }

    private fun add() = trackerSet.add(this)
    private fun cleanEntries() = getData()?.cleanEntries(timedConfig)

    // only modify latest data, regardless of what's being displayed
    override fun getSharedTracker() = ProfileStorageData.profileSpecific?.let { ps ->
        SharedTracker<Data>(
            availableTrackers.associateWith { ps.getOrPutNewestData(it) }
        )
    }

    // TODO figure out why this isn't working
    // make sure stopwatches don't infinitely run when swapping data
    override fun startSessionUptime() {
        super.startSessionUptime()
        getData()?.getAllCurrentData()?.let { activeStopwatches.addAll(it) }
    }

    override fun pauseSessionUptime() {
        super.pauseSessionUptime()
        activeStopwatches.forEach { it.getActiveStopwatch()?.pause(true) }
        activeStopwatches.clear()
    }

    fun resetCommand(displayMode: DisplayMode?, string: String?) = ChatUtils.clickableChat(
        "Are you sure you want to reset your $name? Click here to confirm.",
        onClick = {
            reset(displayMode, string)
        },
        "§eClick to confirm.",
        oneTimeClick = true,
    )

    private fun reset(displayMode: DisplayMode? = null, string: String? = null) {
        when {
            displayMode != null && string != null -> getData()?.reset(displayMode, string)
            displayMode != null -> getData()?.reset(displayMode)
            else -> getData()?.reset()
        }
        ChatUtils.chat("Reset $name!")
        update()
    }

    private fun getData(): Data? = ProfileStorageData.profileSpecific?.getData()
    private fun getOrPutCurrentData(displayMode: DisplayMode = getDisplayMode()): Data? = getData()?.getOrPutCurrentData(displayMode)
    private fun getOrPutCurrentName(displayMode: DisplayMode = getDisplayMode()): String? = getData()?.getOrPutCurrentName(displayMode)
    private fun getPrevNext(displayMode: DisplayMode, string: String): Pair<String?, String?> =
        ProfileStorageData.profileSpecific?.getData()?.getPrevNext(displayMode, string) ?: (null to null)

    private fun ProfileSpecificStorage.getData(): Data = storageAccessor(this)
    private fun ProfileSpecificStorage.getOrPutNewestData(displayMode: DisplayMode = getDisplayMode()) =
        this.getData().getOrPutNewestData(displayMode)

    override fun getDisplay(): List<Renderable> {
        val searchables = getOrPutCurrentData()?.let { drawDisplayF(it) } ?: return emptyList()
        return if (trackerConfig.trackerSearchEnabled.get()) {
            buildFinalDisplay(searchables.buildSearchBox(textInput))
        } else {
            buildFinalDisplay(Renderable.vertical(searchables.toRenderable()))
        }
    }

    override fun buildFinalDisplay(searchBox: Renderable) = buildList {
        if (isInventoryOpen()) {
            buildSwitcherView()?.let { dateSwitcherView ->
                add(
                    Renderable.horizontal(
                        dateSwitcherView,
                        spacing = 5,
                        horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                    ),
                )
            } ?: add(Renderable.placeholder(12))
            add(buildModeRenderable())
        } else {
            add(
                Renderable.placeholder(0, 22)
            )
        }
        add(searchBox)
        if (showSessionUptime()) add(buildSessionUptime(getOrPutCurrentData()))
        if (isEmpty()) return@buildList
        if (isInventoryOpen()) {
            buildDisplayModeView()
            if (getDisplayMode() == DisplayMode.SESSION) {
                if (getData()?.isCurrent(DisplayMode.SESSION) == true) {
                    add(buildSessionCreateButton())
                } else {
                    add(Renderable.horizontal(buildSessionRestoreButton(), buildSessionDeleteButton(), spacing = 5))
                }
            }
        }
    }

    private fun buildModeRenderable() = Renderable.vertical { getDisplayText() }

    private fun getDisplayText(displayMode: DisplayMode = getDisplayMode(), string: String? = getOrPutCurrentName(displayMode)): String {
        if (string == null) return ""
        val prefix = "§7${displayMode.alternateName}"
        val suffix = if (getData()?.isCurrent(displayMode, string) == true) {
            "§a${displayMode.currentName}"
        } else {
            val weekString = (displayMode.toValue(string) as? LocalDate)?.format(weekTextFormatter) ?: string
            "§a${if (displayMode == DisplayMode.WEEK) weekString else string}"
        }
        return "$prefix: $suffix"
    }

    private fun buildSwitcherView(): List<Renderable>? {
        val (previous, next) = getOrPutCurrentName()?.let {
            ProfileStorageData.profileSpecific?.getData()?.getPrevNext(getDisplayMode(), it)
        } ?: return null
        val display = buildSwitcherButtons(previous, next)
        return display
    }

    private var sessionEditTime = SimpleTimeMark.farPast()

    private fun buildSessionCreateButton() = Renderable.clickable(
        "§cCreate New Session!",
        tips = listOf(
            "§cThis will create a new",
            "§csession of",
            "§c$name",
        ),
        onLeftClick = {
            if (sessionEditTime.passedSince() > 3.seconds) {
                createSession()
            }
        },
    )

    private fun buildSessionRestoreButton() = Renderable.clickable(
        "§c[Restore]",
        tips = listOf(
            "§cThis will restore",
            "§cthis session of",
            "§c$name",
        ),
        onLeftClick = {
            if (sessionEditTime.passedSince() > 3.seconds) {
                restoreSession()
                sessionEditTime = SimpleTimeMark.now()
            }
        },
    )

    private fun buildSessionDeleteButton() = Renderable.clickable(
        "§c[Delete]",
        tips = listOf(
            "§cThis will delete",
            "§cthis session of",
            "§c$name",
        ),
        onLeftClick = {
            if (sessionEditTime.passedSince() > .5.seconds) {
                deleteSession()
                sessionEditTime = SimpleTimeMark.now()
            }
        },
    )

    private fun buildSwitcherButtons(
        previous: String?,
        next: String?,
    ): List<Renderable> {
        return listOfNotNull(
            previous?.let {
                Renderable.clickable(
                    "§a[ §r§f§l<- §a]",
                    onLeftClick = { updateDisplay(it) },
                    tips = listOf(getDisplayText(string = it))
                )
            },

            next?.let {
                Renderable.clickable(
                    "§a[ §r§f§l-> §a]",
                    onLeftClick = { updateDisplay(it) },
                    tips = listOf(getDisplayText(string = it))
                )
            },

            if (next?.let { getPrevNext(getDisplayMode(), it).second } != null) {
                val mostRecent = getData()?.getMostRecentName(getDisplayMode())
                mostRecent?.let {
                    Renderable.clickable(
                        "§a[ §r§f§l->> §r§a]",
                        onLeftClick = { updateDisplay(it) },
                        tips = listOf(getDisplayText(string = it))
                    )
                }
            } else null
        )
    }

    private fun createSession() {
        val currentInt = getData()?.getMostRecentName(DisplayMode.SESSION)?.toIntOrNull() ?: 1
        val string = (currentInt + 1).toString()
        getData()?.getOrPutEntry(DisplayMode.SESSION, string)
        getData()?.cleanEntry(timedConfig, DisplayMode.SESSION)
        sessionEditTime = SimpleTimeMark.now()
        update()
    }

    private fun deleteSession() {
        val currentName = getData()?.getCurrentName(DisplayMode.SESSION) ?: return
        if (currentName == getData()?.getMostRecentName(DisplayMode.SESSION)) return
        getData()?.deleteEntry(DisplayMode.SESSION, currentName)
        update()
    }

    private fun restoreSession() {
        val currentInt = getData()?.getMostRecentName(DisplayMode.SESSION)?.toIntOrNull() ?: 1
        val string = (currentInt + 1).toString()
        val data = getData()?.getCurrentName(DisplayMode.SESSION)?.let { getData()?.deleteEntry(DisplayMode.SESSION, it) } ?: return
        getData()?.createEntry(DisplayMode.SESSION, string, data)
        getData()?.setCurrentName(DisplayMode.SESSION, "current")
        update()
    }

    private fun updateDisplay(string: String, displayMode: DisplayMode = getDisplayMode()) {
        getData()?.setCurrentName(displayMode, string)
        update()
    }
}
