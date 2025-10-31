/**
 * TODO LIST
 *  - countdown events like fishing festival + fiesta when its not on tablist
 *  - choose the amount of decimal places in shorten nums
 *  - heavily optimize elements and events by only updating them when absolutely needed
 */

package at.hannibal2.hanni.features.gui.customscoreboard

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.enums.OutsideSBFeature
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.ScoreboardData
import at.hannibal2.hanni.events.ConfigLoadEvent
import at.hannibal2.hanni.events.DebugDataCollectEvent
import at.hannibal2.hanni.events.GuiPositionMovedEvent
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.IslandChangeEvent
import at.hannibal2.hanni.events.ScoreboardUpdateEvent
import at.hannibal2.hanni.events.hypixel.HypixelJoinEvent
import at.hannibal2.hanni.features.gui.customscoreboard.ScoreboardLine.Companion.align
import at.hannibal2.hanni.features.gui.customscoreboard.elements.ScoreboardElement
import at.hannibal2.hanni.features.gui.customscoreboard.elements.ScoreboardElementTitle
import at.hannibal2.hanni.features.gui.customscoreboard.events.ScoreboardEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.ConditionalUtils
import at.hannibal2.hanni.utils.DelayedRun.runDelayed
import at.hannibal2.hanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.hanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.hanni.utils.RenderUtils.renderRenderable
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.hanni.utils.TabListData
import at.hannibal2.hanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.hanni.utils.renderables.Renderable
import at.hannibal2.hanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.hanni.utils.renderables.primitives.text
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@HanniModule
object CustomScoreboard {

    private var display: Renderable? = null
    private var cache: Renderable? = null

    private var currentIslandEntries = listOf<ScoreboardElement>()
    var currentIslandEvents = listOf<ScoreboardEvent>()
        private set

    var activePatterns = listOf<Pattern>()
        private set

    private const val GUI_NAME = "Custom Scoreboard"

    private var nextScoreboardUpdate = SimpleTimeMark.farFuture()

    private var dirty = false

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        display ?: return

        val render =
            if (SkyBlockUtils.inSkyBlock && !TabListData.fullyLoaded && displayConfig.cacheScoreboardOnIslandSwitch && cache != null) cache
            else display

        render ?: return

        // We want to update the background every time, so we can have a smooth transition when using chroma as the color
        val finalRenderable = RenderBackground.addBackground(render)

        RenderBackground.updatePosition(finalRenderable)

        config.position.renderRenderable(finalRenderable, posLabel = GUI_NAME)
    }

    @HandleEvent
    fun onGuiPositionMoved(event: GuiPositionMovedEvent) {
        if (event.guiName == GUI_NAME) {
            with(displayConfig.alignment) {
                if (horizontalAlignment != HorizontalAlignment.DONT_ALIGN || verticalAlignment != VerticalAlignment.DONT_ALIGN) {
                    val tempHori = horizontalAlignment
                    val tempVert = verticalAlignment

                    horizontalAlignment = HorizontalAlignment.DONT_ALIGN
                    verticalAlignment = VerticalAlignment.DONT_ALIGN
                    ChatUtils.clickableChat(
                        "Disabled Custom Scoreboard auto-alignment. Click here to undo this action!",
                        oneTimeClick = true,
                        onClick = {
                            horizontalAlignment = tempHori
                            verticalAlignment = tempVert
                            ChatUtils.chat("Enabled Custom Scoreboard auto-alignment.")
                        },
                    )
                }
            }
        }
    }

    @HandleEvent
    fun onTick() {
        if (!isEnabled()) return

        if (dirty || nextScoreboardUpdate.isInPast()) {
            nextScoreboardUpdate = 250.milliseconds.fromNow()
            dirty = false
            display = createLines().removeEmptyLinesFromEdges().createRenderable()
            if (TabListData.fullyLoaded) {
                cache = display
            }
        }

        // Remove Known Lines, so we can get the unknown ones
        if (SkyBlockUtils.inSkyBlock && displayConfig.useCustomLines && SkyBlockUtils.lastWorldSwitch.passedSince() > 7.seconds)
            UnknownLinesHandler.handleUnknownLines()
    }

    @HandleEvent
    fun onScoreboardChange(event: ScoreboardUpdateEvent) {
        dirty = true
    }

    internal val config get() = HanniMod.feature.gui.customScoreboard
    internal val displayConfig get() = config.display
    internal val informationFilteringConfig get() = config.informationFiltering
    private val eventsConfig get() = displayConfig.events

    private fun createLines() = when {
        !SkyBlockUtils.inSkyBlock -> addAllNonSkyBlockLines()
        !displayConfig.useCustomLines -> addDefaultSkyBlockLines()
        else -> addCustomSkyBlockLines()
    }

    private fun addAllNonSkyBlockLines() = buildList {
        addAll(ScoreboardElementTitle.getLines())
        addAll(ScoreboardData.sidebarLinesFormatted.map { it.align() })
    }

    private fun addDefaultSkyBlockLines() = buildList {
        add(ScoreboardData.objectiveTitle align displayConfig.titleAndFooter.alignTitle)
        addAll(ScoreboardData.sidebarLinesFormatted.map { it.align() })
    }

    private fun addCustomSkyBlockLines() = buildList<ScoreboardLine> {
        for (element in currentIslandEntries) {
            val lines = element.getLines()
            if (lines.isEmpty()) continue

            if (
                informationFilteringConfig.hideConsecutiveEmptyLines &&
                lines.first().display.isEmpty() &&
                lastOrNull()?.display?.isEmpty() == true
            ) {
                continue
            }

            addAll(lines)
        }
    }

    private fun List<ScoreboardLine>.createRenderable() = Renderable.vertical(
        map { Renderable.text(it.display, horizontalAlign = it.alignment) },
        displayConfig.lineSpacing - 10,
        horizontalAlign = HorizontalAlignment.CENTER,
        verticalAlign = VerticalAlignment.CENTER,
    )

    private fun List<ScoreboardLine>.removeEmptyLinesFromEdges(): List<ScoreboardLine> =
        takeIf { !informationFilteringConfig.hideEmptyLinesAtTopAndBottom }
            ?: dropWhile { it.display.isBlank() }.dropLastWhile { it.display.isBlank() }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(
            config.scoreboardEntries,
            eventsConfig.eventEntries,
        ) {
            updateIslandEntries()
        }
    }

    @HandleEvent
    fun onHypixelJoin(event: HypixelJoinEvent) {
        updateAllIslandEntries()
    }

    @HandleEvent
    fun onWorldChange() {
        runDelayed(2.seconds) {
            if (!SkyBlockUtils.inSkyBlock || !(SkyBlockUtils.onHypixel && OutsideSBFeature.CUSTOM_SCOREBOARD.isSelected())) dirty = true
        }
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.newIsland == IslandType.NONE) updateAllIslandEntries()
        else updateIslandEntries()
    }

    private fun updateIslandEntries() {
        currentIslandEntries = config.scoreboardEntries.get().map { it.element }.filter { it.showIsland() }
        currentIslandEvents = eventsConfig.eventEntries.get().map { it.event }.filter { it.showIsland() }

        activePatterns = (ScoreboardConfigElement.getElements() + ScoreboardConfigEventElement.getEvents())
            .filter { it.showIsland() }
            .flatMap { it.elementPatterns }
            .distinct()
        activePatterns += ScoreboardPattern.brokenPatterns
    }

    private fun updateAllIslandEntries() {
        currentIslandEntries = config.scoreboardEntries.get().map { it.element }
        currentIslandEvents = eventsConfig.eventEntries.get().map { it.event }

        activePatterns = ScoreboardConfigElement.getElements()
            .flatMap { it.elementPatterns }
            .distinct()
        activePatterns += ScoreboardPattern.brokenPatterns
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Custom Scoreboard")
        event.addIrrelevant {
            if (!config.enabled.get()) {
                add("Custom Scoreboard disabled.")
            } else {
                add("Custom Scoreboard Lines:")
                addAll(formatEntriesDebug(config.scoreboardEntries.get().map { it.name to it.element }, currentIslandEntries))

                add("Custom Scoreboard Events:")
                addAll(formatEntriesDebug(eventsConfig.eventEntries.get().map { it.name to it.event }, currentIslandEvents))

                add("Active Patterns (${activePatterns.size}):")
                activePatterns.forEach { add("   $it") }

                allUnknownLines.takeIfNotEmpty()?.let { set ->
                    add("Recent Unknown Lines:")
                    set.forEach { add("   ${it.line}") }
                }
            }
        }
    }

    private fun formatEntriesDebug(entries: List<Pair<String, ScoreboardElement>>, currentIslandList: List<ScoreboardElement>) =
        entries.map { (name, element) ->
            val lines = element.getLines().takeIf { it.isNotEmpty() }?.joinToString(", ") { it.display } ?: "No lines to display"
            "   ${name.firstLetterUppercase()} - " +
                "island: ${element.showIsland()} - " +
                "in Island: ${element in currentIslandList} - " +
                "show: ${element.showWhen()} - " +
                lines
        }

    @JvmStatic
    fun resetAppearance() {
        with(config.scoreboardEntries) {
            get().clear()
            get().addAll(ScoreboardConfigElement.defaultOptions)
            notifyObservers()
        }
    }

    private fun isEnabled() =
        (SkyBlockUtils.inSkyBlock || (OutsideSBFeature.CUSTOM_SCOREBOARD.isSelected() && SkyBlockUtils.onHypixel)) && config.enabled.get()

    @JvmStatic
    fun isHideVanillaScoreboardEnabled() = isEnabled() && displayConfig.hideVanillaScoreboard.get()
}
