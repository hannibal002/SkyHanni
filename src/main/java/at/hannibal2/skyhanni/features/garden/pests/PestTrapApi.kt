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
import at.hannibal2.skyhanni.utils.TimeLimitedCache
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.enumMapOf
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
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

    private var delayEvent = false
    private var lastTitleHash: Int = 0
    private var lastNoBaitHash: Int = 0
    private var lastFullHash: Int = 0
    private var trapsPlaced: Int = 0
    private var anyFull: Boolean = false
    private var anyNoBait: Boolean = false
    private var timeEnteredGarden: SimpleTimeMark? = null
    var MAX_TRAPS = 3
        private set

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.PEST_TRAPS)) return
        val timeEnteredGarden = timeEnteredGarden ?: return
        if (timeEnteredGarden.passedSince() < 5.seconds) {
            delayEvent = true
            DelayedRun.runDelayed(5.seconds) {
                if (delayEvent) onWidgetUpdate(event)
            }
            return
        }

        delayEvent = false
        trapsPlaced = event.lines.map { it.getPlacedTraps() }.firstOrNull { it != trapsPlaced } ?: trapsPlaced
        anyFull = event.lines.map { it.anyFull() }.firstOrNull { it != anyFull } ?: anyFull
        anyNoBait = event.lines.map { it.anyNoBait() }.firstOrNull { it != anyNoBait } ?: anyNoBait

        PestTrapDataEvent(
            trapsPlaced = trapsPlaced,
            anyFull = anyFull,
            anyNoBait = anyNoBait,
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

    private fun String.getPlacedTraps(): Int = tabListPestTrapsPattern.matchMatcher(this) {
        widgetEnabledAndVisible[TabWidget.PEST_TRAPS] = true
        lastTitleHash = this.hashCode().takeIf { it != lastTitleHash } ?: return@matchMatcher trapsPlaced
        MAX_TRAPS = groupOrNull("max")?.toIntOrNull() ?: MAX_TRAPS
        return groupOrNull("count")?.toIntOrNull() ?: trapsPlaced
    } ?: trapsPlaced

    private fun String.anyFull(): Boolean = tabListFullTrapsPattern.matchMatcher(this) {
        widgetEnabledAndVisible[TabWidget.FULL_TRAPS] = true
        lastFullHash = this.hashCode().takeIf { it != lastFullHash } ?: return@matchMatcher anyFull
        return this.getTrapIndexSet()?.any() ?: return@matchMatcher anyFull
    } ?: anyFull

    private fun String.anyNoBait(): Boolean = tabListNoBaitPattern.matchMatcher(this) {
        widgetEnabledAndVisible[TabWidget.NO_BAIT] = true
        lastNoBaitHash = this.hashCode().takeIf { it != lastNoBaitHash } ?: return@matchMatcher anyNoBait
        return this.getTrapIndexSet()?.any() ?: return@matchMatcher anyNoBait
    } ?: anyNoBait

    @Suppress("UnstableApiUsage")
    private fun baseWidgetStatus() = TimeLimitedCache<TabWidget, Boolean>(
        expireAfterWrite = 30.seconds,
        removalListener = { key, _, removalCause ->
            if (removalCause != EXPIRED) return@TimeLimitedCache
            widgetErrors.addOrPut(key ?: return@TimeLimitedCache, 1)
        },
    )
}
