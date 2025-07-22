package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.garden.pests.PestTrapDataEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.enumMapOf
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import com.google.common.cache.RemovalCause.EXPIRED
import java.util.regex.Matcher
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PestTrapApi {
    // Todo: Use these to yell at the user to enable the widget if it's disabled
    private val widgetEnabledAndVisible: TimeLimitedCache<TabWidget, Boolean> = baseWidgetStatus()
    private val widgetErrors: MutableMap<TabWidget, Long> = enumMapOf()

    private val tabListPestTrapsPattern = TabWidget.PEST_TRAPS.pattern
    private val tabListFullTrapsPattern = TabWidget.FULL_TRAPS.pattern
    private val tabListNoBaitPattern = TabWidget.NO_BAIT.pattern

    private val delayEvent: MutableMap<TabWidget, Boolean> = enumMapOf()
    private val lastHashes: TimeLimitedCache<TabWidget, Int> = TimeLimitedCache(10.seconds)
    var trapsPlaced: Int? = null
        private set
    var fullTraps: Set<Int>? = null
        private set
    var noBaitTraps: Set<Int>? = null
        private set
    private var timeEnteredGarden: SimpleTimeMark? = null
    var MAX_TRAPS = 3
        private set

    @HandleEvent
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.PEST_TRAPS, TabWidget.FULL_TRAPS, TabWidget.NO_BAIT)) return
        if (event.lines.isEmpty()) return
        val timeEnteredGarden = timeEnteredGarden ?: run {
            timeEnteredGarden = SimpleTimeMark.now()
            return
        }
        if (timeEnteredGarden.passedSince() < 5.seconds) {
            delayEvent[event.widget] = true
            DelayedRun.runDelayed(5.seconds) {
                if (delayEvent[event.widget] == true) onWidgetUpdate(event)
            }
            return
        }

        delayEvent[event.widget] = false

        when (event.widget) {
            TabWidget.PEST_TRAPS -> {
                widgetEnabledAndVisible[TabWidget.PEST_TRAPS] = true
                trapsPlaced = event.lines.firstNotNullOfOrNull { it.getTrapsPlacedOrNull() }
            }
            TabWidget.FULL_TRAPS -> {
                widgetEnabledAndVisible[TabWidget.FULL_TRAPS] = true
                fullTraps = event.lines.firstNotNullOfOrNull { it.getFullTrapsOrNull() }
            }
            TabWidget.NO_BAIT -> {
                widgetEnabledAndVisible[TabWidget.NO_BAIT] = true
                noBaitTraps = event.lines.firstNotNullOfOrNull { it.getNoBaitTrapsOrNull() }
            }
            else -> return
        }

        PestTrapDataEvent(
            trapsPlaced = trapsPlaced ?: MAX_TRAPS,
            fullTraps = fullTraps.orEmpty(),
            noBaitTraps = noBaitTraps.orEmpty(),
        ).post()
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.newIsland != IslandType.GARDEN) return
        timeEnteredGarden = SimpleTimeMark.now()
    }

    private fun Matcher.getTrapIndexSet(): Set<Int>? =
        groupOrNull("traps")?.removeColor()?.replace("#", "")?.split(", ")?.mapNotNull {
            it.toIntOrNull()
        }?.takeIfNotEmpty()?.toSet()

    private fun TabWidget.getNewHashOrNull(line: String): Int? = line.hashCode().takeIf {
        it != lastHashes[this]
    }

    private fun String.getTrapsPlacedOrNull(): Int? = tabListPestTrapsPattern.matchMatcher(this) {
        widgetEnabledAndVisible[TabWidget.PEST_TRAPS] = true
        MAX_TRAPS = groupOrNull("max")?.toIntOrNull() ?: MAX_TRAPS
        lastHashes[TabWidget.PEST_TRAPS] = TabWidget.PEST_TRAPS.getNewHashOrNull(this@getTrapsPlacedOrNull)
            ?: return@matchMatcher trapsPlaced
        return groupOrNull("count")?.toIntOrNull()
    }

    private fun String.getFullTrapsOrNull(): Set<Int>? = tabListFullTrapsPattern.matchMatcher(this) {
        widgetEnabledAndVisible[TabWidget.FULL_TRAPS] = true
        lastHashes[TabWidget.FULL_TRAPS] = TabWidget.FULL_TRAPS.getNewHashOrNull(this@getFullTrapsOrNull)
            ?: return@matchMatcher fullTraps
        return this.getTrapIndexSet()
    }

    private fun String.getNoBaitTrapsOrNull(): Set<Int>? = tabListNoBaitPattern.matchMatcher(this) {
        widgetEnabledAndVisible[TabWidget.NO_BAIT] = true
        lastHashes[TabWidget.NO_BAIT] = TabWidget.NO_BAIT.getNewHashOrNull(this@getNoBaitTrapsOrNull)
            ?: return@matchMatcher noBaitTraps
        return this.getTrapIndexSet()
    }

    @Suppress("UnstableApiUsage")
    private fun baseWidgetStatus() = TimeLimitedCache<TabWidget, Boolean>(
        expireAfterWrite = 30.seconds,
        removalListener = { key, _, removalCause ->
            if (removalCause != EXPIRED) return@TimeLimitedCache
            widgetErrors.addOrPut(key ?: return@TimeLimitedCache, 1)
        },
    )
}
