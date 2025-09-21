package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.misc.tracker.TimedTrackerConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.TrackerGenericConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.timed.TimedGenericIndividualConfig
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.DateChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.TimeUtils.dayToLocalDate
import at.hannibal2.skyhanni.utils.TimeUtils.monthFormatter
import at.hannibal2.skyhanni.utils.TimeUtils.monthToLocalDate
import at.hannibal2.skyhanni.utils.TimeUtils.weekFormatter
import at.hannibal2.skyhanni.utils.TimeUtils.weekTextFormatter
import at.hannibal2.skyhanni.utils.TimeUtils.weekToLocalDate
import at.hannibal2.skyhanni.utils.TimeUtils.yearFormatter
import at.hannibal2.skyhanni.utils.TimeUtils.yearToLocalDate
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.buildSearchBox
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import at.hannibal2.skyhanni.utils.renderables.toRenderable
import java.time.LocalDate

@Suppress("SpreadOperator")
class SkyhanniTimedTracker<Data : TrackerData<*>, Type : TimedGenericIndividualConfig<*>>(
    name: String,
    createNewSession: () -> Data,
    private var storage: (ProfileSpecificStorage) -> TimedTrackerData<Data, *>,
    drawDisplay: (Data) -> List<Searchable>,
    extraDisplayModes: Map<DisplayMode, (ProfileSpecificStorage) -> Data> = emptyMap(),
    customUptimeControl: Boolean = false,
    trackerConfig: () -> Type
) : SkyHanniTracker<Data, Type>(
    name,
    createNewSession,
    { throw UnsupportedOperationException("getStorage not used") },
    extraDisplayModes,
    drawDisplay = drawDisplay,
    trackerConfig = trackerConfig,
    customUptimeControl = customUptimeControl
) {
    private val timedConfig: TimedTrackerConfig get() =
        if (trackerSpecificConfig.useUniversalConfig) universalTracker.timedTracker else trackerSpecificConfig.timedTracker
    override val availableTrackers = listOf(
        DisplayMode.TOTAL,
        DisplayMode.SESSION,
        DisplayMode.DAY,
        DisplayMode.WEEK,
        DisplayMode.MONTH,
        DisplayMode.YEAR,
    ) + extraDisplayModes.keys
    private val config: TrackerGenericConfig
        get() = if (trackerSpecificConfig.useUniversalConfig) universalTracker else trackerSpecificConfig.trackerConfig

    var date: LocalDate = LocalDate.now()
    var week: LocalDate = date.format(weekFormatter).weekToLocalDate()
    var month: LocalDate = date.format(monthFormatter).monthToLocalDate()
    var year: LocalDate = date.format(yearFormatter).yearToLocalDate()

    @SkyHanniModule
    companion object {
        private val trackerSet: MutableSet<SkyhanniTimedTracker<*, *>> = mutableSetOf()

        @HandleEvent
        fun onDateChange(event: DateChangeEvent) {
            trackerSet.forEach { it.changeDate(event.oldDate, event.newDate) }
        }

        @HandleEvent
        fun onConfigLoad(event: ConfigLoadEvent) {
            trackerSet.forEach { it.cleanEntries() }
        }
    }

    init {
        if (timedConfig.resetSession) {
            ProfileStorageData.profileSpecific?.getData()?.getEntry(DisplayMode.SESSION)?.reset()
        }
        trackerSet.add(this)
    }
    private fun cleanEntries() = ProfileStorageData.profileSpecific?.getData()?.cleanEntries(timedConfig)

    override fun getSharedTracker() = ProfileStorageData.profileSpecific?.let { ps ->
        SharedTracker(
            availableTrackers.associateWith { ps.getDisplay(it) }
        )
    }

    private fun ProfileSpecificStorage.getData() = storage(this)
    private fun ProfileSpecificStorage.getDisplay(displayMode: DisplayMode, date: LocalDate = LocalDate.now()) =
        this.getData().getOrPutEntry(displayMode, date)

    private fun getData(displayMode: DisplayMode = getDisplayMode()): Data? = ProfileStorageData.profileSpecific?.let { ps ->
        when (displayMode) {
            DisplayMode.WEEK -> ps.getDisplay(displayMode, week)
            DisplayMode.MONTH -> ps.getDisplay(displayMode, month)
            DisplayMode.YEAR -> ps.getDisplay(displayMode, year)
            else -> ps.getDisplay(displayMode, date)
        }
    }

    override fun getDisplay() = getData()?.let { data ->
        val searchables = drawDisplay(data)
        if (config.trackerSearchEnabled.get()) buildFinalDisplay(searchables.buildSearchBox(textInput))
        else buildFinalDisplay(Renderable.vertical(searchables.toRenderable()))
    }.orEmpty()

    fun changeDate(oldDate: LocalDate, newDate: LocalDate) {
        var changed = false

        fun updateIfMatch(current: LocalDate, newVal: LocalDate): LocalDate {
            return if (current == oldDate) {
                changed = true
                newVal
            } else current
        }

        date = updateIfMatch(date, newDate)
        week = updateIfMatch(week, newDate.format(weekFormatter).weekToLocalDate())
        month = updateIfMatch(month, newDate.format(monthFormatter).monthToLocalDate())
        year = updateIfMatch(year, newDate.format(yearFormatter).yearToLocalDate())

        if (changed) update()
    }

    fun dateString(): String {
        val today = LocalDate.now()
        return when (displayMode) {
            DisplayMode.DAY -> if (date == today) "Today" else date.toString()
            DisplayMode.WEEK -> if (week.format(weekFormatter) == today.format(weekFormatter)) "This Week"
            else week.format(weekTextFormatter)
            DisplayMode.MONTH -> if (month.format(monthFormatter) == today.format(monthFormatter)) "This Month"
            else month.format(monthFormatter)
            DisplayMode.YEAR -> if (year.year == today.year) "This Year" else year.format(yearFormatter)
            else -> displayMode?.displayName ?: "Session: "
        }
    }

    override fun buildFinalDisplay(searchBox: Renderable) = buildList {
        if (inventoryOpen) {
            buildDateSwitcherView()?.let { dateSwitcherView ->
                add(
                    Renderable.horizontal(
                        dateSwitcherView,
                        spacing = 5,
                        horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                    ),
                )
            } ?: add(Renderable.placeholder(12))
            add(buildDateRenderable())
        } else {
            add(
                Renderable.placeholder(0, 22)
            )
        }
        add(searchBox)
        add(buildSessionUptime(getData()))
        if (isEmpty()) return@buildList
        if (inventoryOpen) {
            buildDisplayModeView()
            if (getDisplayMode() == DisplayMode.SESSION) {
                add(buildSessionResetButton())
            }
        }
    }

    private fun buildDateRenderable(onlyShowDates: Boolean = true) = Renderable.vertical(
        buildList {
            val displayText: String = if (getDisplayMode().isDate) {
                "§7${displayMode?.alternateName}: §a${dateString()}"
            } else {
                if (onlyShowDates) {
                    ""
                } else {
                    "§7Mode: §a${getDisplayMode().displayName}"
                }
            }

            addString(displayText)
        }
    )

    private fun buildDateSwitcherView(): List<Renderable>? {
        val (previous, next) = when (getDisplayMode()) {
            DisplayMode.DAY -> getPrevNext(date) { this.dayToLocalDate() }
            DisplayMode.WEEK -> getPrevNext(week) { this.weekToLocalDate() }
            DisplayMode.MONTH -> getPrevNext(month) { this.monthToLocalDate() }
            DisplayMode.YEAR -> getPrevNext(year) { this.yearToLocalDate() }
            else -> Pair(null, null)
        }
        if (previous == null && next == null) return null
        val display = buildDateSwitcherButtons(previous, next)
        return display
    }

    private fun getPrevNext(date: LocalDate, func: String.() -> LocalDate): Pair<LocalDate?, LocalDate?> {
        val statsStorage = ProfileStorageData.profileSpecific?.getData()
        val entries = statsStorage?.getEntries(getDisplayMode())?.keys ?: return Pair(null, null)

        val previous = entries.filter { it.func() < date }.maxOrNull()?.func()
        val next = entries.filter { it.func() > date }.minOrNull()?.func()
        return Pair(previous, next)
    }

    private fun buildDateSwitcherButtons(
        previous: LocalDate?,
        next: LocalDate?,
    ): List<Renderable> {
        return listOfNotNull(
            previous?.let { Renderable.optionalLink("§a[ §r§f§l<- §a]", onLeftClick = { updateDate(it) }) },
            next?.let { Renderable.optionalLink("§a[ §r§f§l-> §r§a]", onLeftClick = { updateDate(it) }) },
            if (next?.isInPast(getDisplayMode()) == true) {
                Renderable.optionalLink("§a[ §r§f§l->> §r§a]", onLeftClick = { updateDatesToNow(getDisplayMode()) })
            } else null
        )
    }

    private fun LocalDate.isInPast(displayMode: DisplayMode): Boolean = when (displayMode) {
        DisplayMode.WEEK -> this < LocalDate.now().format(weekFormatter).weekToLocalDate()
        DisplayMode.MONTH -> this < LocalDate.now().format(monthFormatter).monthToLocalDate()
        DisplayMode.YEAR -> this < LocalDate.now().format(yearFormatter).yearToLocalDate()
        else -> this < LocalDate.now()
    }

    private fun updateDatesToNow(displayMode: DisplayMode) {
        when (displayMode) {
            DisplayMode.WEEK -> week = LocalDate.now().format(weekFormatter).weekToLocalDate()
            DisplayMode.MONTH -> month = LocalDate.now().format(monthFormatter).monthToLocalDate()
            DisplayMode.YEAR -> year = LocalDate.now().format(yearFormatter).yearToLocalDate()
            else -> date = LocalDate.now()
        }
        update()
    }

    private fun updateDate(newDate: LocalDate) {
        when (getDisplayMode()) {
            DisplayMode.WEEK -> week = newDate
            DisplayMode.MONTH -> month = newDate
            DisplayMode.YEAR -> year = newDate
            else -> date = newDate
        }
        update()
    }
}
