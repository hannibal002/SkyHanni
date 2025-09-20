package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.config.features.misc.tracker.GenericIndividualTrackerConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.TrackerGenericConfig
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.TimeUtils.dayToLocalDate
import at.hannibal2.skyhanni.utils.TimeUtils.monthFormatter
import at.hannibal2.skyhanni.utils.TimeUtils.monthToLocalDate
import at.hannibal2.skyhanni.utils.TimeUtils.weekFormatter
import at.hannibal2.skyhanni.utils.TimeUtils.weekTextFormatter
import at.hannibal2.skyhanni.utils.TimeUtils.weekToLocalDate
import at.hannibal2.skyhanni.utils.TimeUtils.yearFormatter
import at.hannibal2.skyhanni.utils.TimeUtils.yearToLocalDate
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.buildSearchBox
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import at.hannibal2.skyhanni.utils.renderables.toRenderable
import at.hannibal2.skyhanni.utils.tracker.TimedTrackerData.TrackerManager.trackerSet
import java.time.LocalDate

@Suppress("SpreadOperator")
class SkyhanniTimedTracker<Data : TrackerData<*>, Type : GenericIndividualTrackerConfig<*>>(
    name: String,
    createNewSession: () -> Data,
    private var storage: (ProfileSpecificStorage) -> TimedTrackerData<Data, *>,
    drawDisplay: (Data) -> List<Searchable>,
    extraDisplayModes: Map<DisplayMode, (ProfileSpecificStorage) -> Data> = emptyMap(),
    trackerConfig: () -> Type
) : SkyHanniTracker<Data, Type>(
    name,
    createNewSession,
    { throw UnsupportedOperationException("getStorage not used") },
    extraDisplayModes,
    drawDisplay = drawDisplay,
    trackerConfig = trackerConfig
) {
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

    var date = LocalDate.now()
    var week = date.format(weekFormatter).weekToLocalDate()
    var month = date.format(monthFormatter).monthToLocalDate()
    var year = date.format(yearFormatter).yearToLocalDate()

    private fun getNextDisplay(): DisplayMode {
        return availableTrackers[(availableTrackers.indexOf(displayMode) + 1) % availableTrackers.size]
    }

    private fun getPreviousDisplay(): DisplayMode {
        return availableTrackers[(availableTrackers.indexOf(displayMode) - 1 + availableTrackers.size) % availableTrackers.size]
    }

    private fun ProfileSpecificStorage.getData() = storage(this)
    private fun ProfileSpecificStorage.getDisplay(displayMode: DisplayMode, date: LocalDate = LocalDate.now()) =
        this.getData().getOrPutEntry(displayMode, date)

    override fun getSharedTracker() = ProfileStorageData.profileSpecific?.let { ps ->
        SharedTracker(
            availableTrackers.associateWith { ps.getDisplay(it) }
        )
    }

    private fun buildLore(): List<String> {
        val currentTracker = "§7Current Mode: §a${displayMode?.displayName ?: "none"}"
        val nextTracker = "§7Next Mode: §a${getNextDisplay().displayName} §e(Click)"
        val lastTracker = "§7Previous Mode: §a${getPreviousDisplay().displayName} §e(Ctrl + Click)"
        return listOf(currentTracker, nextTracker, lastTracker)
    }

    override fun getDisplay() = ProfileStorageData.profileSpecific?.let { ps ->
        val data = when (getDisplayMode()) {
            DisplayMode.WEEK -> ps.getDisplay(getDisplayMode(), week)
            DisplayMode.MONTH -> ps.getDisplay(getDisplayMode(), month)
            DisplayMode.YEAR -> ps.getDisplay(getDisplayMode(), year)
            else -> ps.getDisplay(getDisplayMode(), date)
        }
        val searchables = drawDisplay(data)
        if (config.trackerSearchEnabled.get()) buildFinalDisplay(searchables.buildSearchBox(textInput))
        else buildFinalDisplay(Renderable.vertical(searchables.toRenderable()))
    }.orEmpty()

    fun changeDate(oldDate: LocalDate, newDate: LocalDate) {
        if (date == oldDate) {
            date = newDate
            update()
        }
        if (week == oldDate.format(weekFormatter).weekToLocalDate()) {
            week = newDate.format(weekFormatter).weekToLocalDate()
            update()
        }
        if (month == oldDate.format(monthFormatter).monthToLocalDate()) {
            month = newDate.format(monthFormatter).monthToLocalDate()
            update()
        }
        if (year == oldDate.format(yearFormatter).yearToLocalDate()) {
            year = newDate.format(yearFormatter).yearToLocalDate()
            update()
        }
    }

    fun updateTracker(display: DisplayMode, day: LocalDate, week: LocalDate, month: LocalDate, year: LocalDate) {
        displayMode = display
        date = day
        this.week = week
        this.month = month
        this.year = year
    }


    fun dateString(): String = (
        when (displayMode) {
            DisplayMode.DAY -> {
                if (date == LocalDate.now()) "Today" else date.toString()
            }
            DisplayMode.WEEK -> {
                if (week.format(weekFormatter) == LocalDate.now().format(weekFormatter))
                    "This Week" else week.format(weekTextFormatter)
            }

            DisplayMode.MONTH -> {
                if (month.format(monthFormatter) == LocalDate.now().format(monthFormatter))
                    "This Month" else month.format(monthFormatter)
            }

            DisplayMode.YEAR -> {
                if (year.year == LocalDate.now().year)
                    "This Year" else year.format(yearFormatter)
            }

            else -> {
                displayMode?.displayName ?: "Session: "
            }
        }
        )

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
            }
        } else {
            add(
                Renderable.placeholder(10)
            )
        }

        add(searchBox)
        if (isEmpty()) return@buildList
        if (inventoryOpen) {
            buildDisplayModeView()
            if (getDisplayMode() == DisplayMode.SESSION) {
                add(buildSessionResetButton())
            }
        }
    }

    private fun buildDateSwitcherView(): List<Renderable>? {
        val statsStorage = ProfileStorageData.profileSpecific?.getData() ?: return null
        val entries = statsStorage.getEntries(getDisplayMode())?.keys ?: return null

        var previous: LocalDate? = null
        var next: LocalDate? = null

        when (getDisplayMode()) {
            DisplayMode.DAY -> {
                previous = entries.filter { it.dayToLocalDate() < date }.maxOrNull()?.dayToLocalDate()
                next = entries.filter { it.dayToLocalDate() > date }.minOrNull()?.dayToLocalDate()
            }
            DisplayMode.WEEK -> {
                previous = entries.filter { it.weekToLocalDate() < week }.maxOrNull()?.weekToLocalDate()
                next = entries.filter { it.weekToLocalDate() > week }.minOrNull()?.weekToLocalDate()
            }
            DisplayMode.MONTH -> {
                previous = entries.filter { it.monthToLocalDate() < month }.maxOrNull()?.monthToLocalDate()
                next = entries.filter { it.monthToLocalDate() > month }.minOrNull()?.monthToLocalDate()
            }
            DisplayMode.YEAR -> {
                previous = entries.filter { it.yearToLocalDate() < year }.maxOrNull()?.yearToLocalDate()
                next = entries.filter { it.yearToLocalDate() > year }.minOrNull()?.yearToLocalDate()
            }
            else -> {}
        }
        if (previous == null && next == null) return null
        val display = buildDateSwitcherButtons(previous, next)
        return display
    }

    private fun buildDateSwitcherButtons(
        previous: LocalDate?,
        next: LocalDate?,
    ): List<Renderable> {
        return listOfNotNull(
            previous?.let {
                Renderable.optionalLink(
                    "§a[ §r§f§l<- §a]",
                    onLeftClick = {
                        when (getDisplayMode()) {
                            DisplayMode.WEEK -> week = it
                            DisplayMode.MONTH -> month = it
                            DisplayMode.YEAR -> year = it
                            else -> date = it
                        }
                        update()
                    },
                )
            },
            next?.let {
                Renderable.optionalLink(
                    "§a[ §r§f§l-> §r§a]",
                    onLeftClick = {
                        when (getDisplayMode()) {
                            DisplayMode.WEEK -> week = it
                            DisplayMode.MONTH -> month = it
                            DisplayMode.YEAR -> year = it
                            else -> date = it
                        }
                        update()
                    },
                )
            },
            if (next != null &&
                when (getDisplayMode()) {
                    DisplayMode.WEEK -> next < LocalDate.now().format(weekFormatter).weekToLocalDate()
                    DisplayMode.MONTH -> next < LocalDate.now().format(monthFormatter).monthToLocalDate()
                    DisplayMode.YEAR -> next < LocalDate.now().format(yearFormatter).yearToLocalDate()
                    else -> next < LocalDate.now()
                }
            ) {
                Renderable.optionalLink(
                    "§a[ §r§f§l->> §r§a]",
                    onLeftClick = {
                        when (getDisplayMode()) {
                            DisplayMode.WEEK -> week = LocalDate.now().format(weekFormatter).weekToLocalDate()
                            DisplayMode.MONTH -> month = LocalDate.now().format(monthFormatter).monthToLocalDate()
                            DisplayMode.YEAR -> year = LocalDate.now().format(yearFormatter).yearToLocalDate()
                            else -> date = LocalDate.now()
                        }
                        update()
                    },
                )
            } else null
        )
    }
}
