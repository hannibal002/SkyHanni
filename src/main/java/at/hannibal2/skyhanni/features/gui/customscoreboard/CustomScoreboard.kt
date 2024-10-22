/**
 * TODO LIST
 *  - Bank API (actually maybe not, I like the current design)
 *  - countdown events like fishing festival + fiesta when its not on tablist
 *  - improve hide coin difference to also work with bits, motes, etc
 *  - color options in the purse etc lines
 *  - choose the amount of decimal places in shorten nums
 *  - heavily optimize elements and events by only updating them when absolutely needed
 */

package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.enums.OutsideSbFeature
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.GuiPositionMovedEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.WorldChangeEvent
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardLine.Companion.align
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.ScoreboardElement
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.DelayedRun.runDelayed
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraftforge.client.GuiIngameForge
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CustomScoreboard {

    private var display: Renderable? = null
    private var cache: Renderable? = null

    private var currentIslandEntries = listOf<ScoreboardElement>()
    var currentIslandEvents = listOf<ScoreboardEvent>()
        private set

    private const val GUI_NAME = "Custom Scoreboard"

    private var nextScoreboardUpdate = SimpleTimeMark.farFuture()

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        display ?: return

        val render =
            if (LorenzUtils.inSkyBlock && !TabListData.fullyLoaded && displayConfig.cacheScoreboardOnIslandSwitch && cache != null) cache
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
            with(alignmentConfig) {
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
    fun onTick(event: SkyHanniTickEvent) {
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
        if (LorenzUtils.inSkyBlock && displayConfig.useCustomLines) UnknownLinesHandler.handleUnknownLines()
    }

    @HandleEvent
    fun onScoreboardChange(event: ScoreboardUpdateEvent) {
        dirty = true
    }

    // TODO move those into their respective classes and make them private
    internal val config get() = SkyHanniMod.feature.gui.customScoreboard
    internal val displayConfig get() = config.display
    internal val alignmentConfig get() = displayConfig.alignment
    internal val arrowConfig get() = displayConfig.arrow
    internal val chunkedConfig get() = displayConfig.chunkedStats
    internal val eventsConfig get() = displayConfig.events
    internal val mayorConfig get() = displayConfig.mayor
    internal val partyConfig get() = displayConfig.party
    internal val maxwellConfig get() = displayConfig.maxwell
    internal val informationFilteringConfig get() = config.informationFiltering
    internal val backgroundConfig get() = config.background

    private fun createLines() = when {
        !LorenzUtils.inSkyBlock -> addAllNonSkyBlockLines()
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

    private fun List<ScoreboardLine>.createRenderable() = Renderable.verticalContainer(
        map { Renderable.string(it.display, horizontalAlign = it.alignment) },
        displayConfig.lineSpacing - 10,
        horizontalAlign = HorizontalAlignment.CENTER,
        verticalAlign = VerticalAlignment.CENTER,
    )

    private fun List<ScoreboardLine>.removeEmptyLinesFromEdges(): List<ScoreboardLine> =
        takeIf { !informationFilteringConfig.hideEmptyLinesAtTopAndBottom }
            ?: dropWhile { it.display.isBlank() }.dropLastWhile { it.display.isBlank() }

    private var dirty = false

    // The ElementType for the Vanilla Scoreboard is called HELMET
    // Thanks to APEC for showing this
    @SubscribeEvent
    fun onRenderScoreboard(event: RenderGameOverlayEvent.Post) {
        if (event.type == RenderGameOverlayEvent.ElementType.HELMET) {
            if (isHideVanillaScoreboardEnabled()) {
                GuiIngameForge.renderObjective = false
            }
            if (dirty) {
                GuiIngameForge.renderObjective = true
                dirty = false
            }
        }
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(
            config.enabled,
            displayConfig.hideVanillaScoreboard,
            SkyHanniMod.feature.misc.showOutsideSB,
        ) {
            if (!isHideVanillaScoreboardEnabled()) dirty = true
        }
        ConditionalUtils.onToggle(
            config.scoreboardEntries,
            eventsConfig.eventEntries,
        ) {
            updateIslandEntries()
        }
    }

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        runDelayed(2.seconds) {
            if (!LorenzUtils.inSkyBlock || !(LorenzUtils.onHypixel && OutsideSbFeature.CUSTOM_SCOREBOARD.isSelected())) dirty = true
        }
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        updateIslandEntries()
    }

    private fun updateIslandEntries() {
        currentIslandEntries = config.scoreboardEntries.get().map { it.element }.filter { it.showIsland() }
        currentIslandEvents = eventsConfig.eventEntries.get().map { it.event }.filter { it.showIsland() }
    }

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Custom Scoreboard")
        event.addIrrelevant {
            if (!config.enabled.get()) {
                add("Custom Scoreboard disabled.")
            } else {
                add("Custom Scoreboard Lines:")
                ScoreboardConfigElement.entries.forEach { entry ->
                    add(
                        "   ${entry.name.firstLetterUppercase()} - " +
                            "island: ${entry.element.showIsland()} - " +
                            "show: ${entry.element.showWhen()} - " +
                            "${entry.element.getLines().map { it.display }}",
                    )
                }
                allUnknownLines.takeIfNotEmpty()?.let { set ->
                    add("Recent Unknown Lines:")
                    set.forEach { add("   ${it.line}") }
                }
            }
        }
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
        (LorenzUtils.inSkyBlock || (OutsideSbFeature.CUSTOM_SCOREBOARD.isSelected() && LorenzUtils.onHypixel)) && config.enabled.get()

    private fun isHideVanillaScoreboardEnabled() = isEnabled() && displayConfig.hideVanillaScoreboard.get()

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        val prefix = "gui.customScoreboard"
        val displayConfigPrefix = "$prefix.displayConfig"
        val displayPrefix = "$prefix.display"

        event.move(
            28,
            "$prefix.displayConfig.showAllActiveEvents",
            "$prefix.displayConfig.eventsConfig.showAllActiveEvents",
        )

        event.move(31, "$displayConfigPrefix.arrowAmountDisplay", "$displayPrefix.arrow.amountDisplay")
        event.move(31, "$displayConfigPrefix.colorArrowAmount", "$displayPrefix.arrow.colorArrowAmount")
        event.move(31, "$displayConfigPrefix.showMagicalPower", "$displayPrefix.maxwell.showMagicalPower")
        event.move(31, "$displayConfigPrefix.compactTuning", "$displayPrefix.maxwell.compactTuning")
        event.move(31, "$displayConfigPrefix.tuningAmount", "$displayPrefix.maxwell.tuningAmount")
        event.move(31, "$displayConfigPrefix.hideVanillaScoreboard", "$displayPrefix.hideVanillaScoreboard")
        event.move(31, "$displayConfigPrefix.displayNumbersFirst", "$displayPrefix.displayNumbersFirst")
        event.move(31, "$displayConfigPrefix.showUnclaimedBits", "$displayPrefix.showUnclaimedBits")
        event.move(31, "$displayConfigPrefix.showMaxIslandPlayers", "$displayPrefix.showMaxIslandPlayers")
        event.move(31, "$displayConfigPrefix.numberFormat", "$displayPrefix.numberFormat")
        event.move(31, "$displayConfigPrefix.lineSpacing", "$displayPrefix.lineSpacing")
        event.move(
            31,
            "$displayConfigPrefix.cacheScoreboardOnIslandSwitch",
            "$displayPrefix.cacheScoreboardOnIslandSwitch",
        )
        // Categories
        event.move(31, "$displayConfigPrefix.alignment", "$displayPrefix.alignment")
        event.move(31, "$displayConfigPrefix.titleAndFooter", "$displayPrefix.titleAndFooter")
        event.move(31, "$prefix.backgroundConfig", "$prefix.background")
        event.move(31, "$prefix.informationFilteringConfig", "$prefix.informationFiltering")
        event.move(31, "$displayConfigPrefix.eventsConfig", "$displayPrefix.events")
        event.move(31, "$prefix.mayorConfig", "$displayPrefix.mayor")
        event.move(31, "$prefix.partyConfig", "$displayPrefix.party")

        event.transform(37, "$displayPrefix.events.eventEntries") { element ->
            val array = element.asJsonArray
            array.add(JsonPrimitive(ScoreboardEvent.QUEUE.name))
            array
        }
        event.transform(40, "$displayPrefix.events.eventEntries") { element ->
            val jsonArray = element.asJsonArray
            val newArray = JsonArray()

            for (jsonElement in jsonArray) {
                val stringValue = jsonElement.asString
                if (stringValue !in listOf("HOT_DOG_CONTEST", "EFFIGIES")) {
                    newArray.add(jsonElement)
                }
            }

            if (jsonArray.any { it.asString in listOf("HOT_DOG_CONTEST", "EFFIGIES") }) {
                newArray.add(JsonPrimitive(ScoreboardEvent.RIFT.name))
            }

            newArray
        }

        event.move(43, "$displayPrefix.alignment.alignRight", "$displayPrefix.alignment.horizontalAlignment") {
            JsonPrimitive(
                if (it.asBoolean) {
                    HorizontalAlignment.RIGHT.name
                } else {
                    HorizontalAlignment.DONT_ALIGN.name
                },
            )
        }
        event.move(43, "$displayPrefix.alignment.alignCenterVertically", "$displayPrefix.alignment.verticalAlignment") {
            JsonPrimitive(
                if (it.asBoolean) {
                    VerticalAlignment.CENTER.name
                } else {
                    VerticalAlignment.DONT_ALIGN.name
                },
            )
        }
        event.transform(50, "$displayPrefix.events.eventEntries") { element ->
            val array = element.asJsonArray
            array.add(JsonPrimitive(ScoreboardEvent.ANNIVERSARY.name))
            array.add(JsonPrimitive(ScoreboardEvent.CARNIVAL.name))
            array
        }
        event.transform(51, "$displayPrefix.events.eventEntries") { element ->
            val array = element.asJsonArray
            array.add(JsonPrimitive(ScoreboardEvent.NEW_YEAR.name))
            array
        }
        event.move(
            57,
            "$displayPrefix.titleAndFooter.useHypixelTitleAnimation",
            "$displayPrefix.titleAndFooter.useCustomTitle",
        ) {
            JsonPrimitive(!it.asBoolean)
        }
    }
}
