package at.hannibal2.skyhanni.features.garden.plot

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.entity.EntityMoveEvent
import at.hannibal2.skyhanni.events.garden.GardenPlotSprayEvent
import at.hannibal2.skyhanni.events.garden.GardenPlotSprayDataTablistReadEvent
import at.hannibal2.skyhanni.events.garden.PlotChangeEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.garden.pests.sprayonator.SprayType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LocationUtils.playerLocation
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.getTablistEndTime
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.annotations.Expose
import net.minecraft.client.player.LocalPlayer
import java.awt.Color
import kotlin.math.floor
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GardenPlotApi {

    private const val PLOT_SIZE = 96.0
    private const val PLOT_GRID_SIZE = 5
    private const val PLOT_GRID_MIN = -240.0
    private const val PLOT_GRID_MAX = 240.0

    private val plotMap = listOf(
        listOf(21, 13, 9, 14, 22),
        listOf(15, 5, 1, 6, 16),
        listOf(10, 2, 0, 3, 11),
        listOf(17, 7, 4, 8, 18),
        listOf(23, 19, 12, 20, 24),
    )

    private val patternGroup = RepoPattern.group("garden.plot")

    /**
     * REGEX-TEST: §aPlot §7- §b4
     */
    private val plotNamePattern by patternGroup.pattern(
        "name",
        "§.Plot §7- §b(?<name>.*)",
    )

    /**
     * REGEX-TEST: §aThe Barn
     */
    private val barnNamePattern by patternGroup.pattern(
        "barnname",
        "§.(?<name>The Barn)",
    )

    /**
     * REGEX-TEST: §7Greenhouse Plot
     */
    private val greenhousePlotPattern by patternGroup.pattern(
        "greenhouse",
        "§7Greenhouse Plot",
    )

    /**
     * REGEX-TEST: §7Cleanup: §b0% Completed
     */
    private val uncleanedPlotPattern by patternGroup.pattern(
        "uncleaned",
        "§7Cleanup: .* (?:§.)*Completed",
    )

    /**
     * REGEX-TEST: §aUnlocked Garden §r§aPlot §r§7- §r§b10§r§a!
     */
    private val unlockPlotChatPattern by patternGroup.pattern(
        "chat.unlock",
        "§aUnlocked Garden §r§aPlot §r§7- §r§b(?<plot>.*)§r§a!",
    )

    /**
     * REGEX-TEST: §aPlot §r§7- §r§b10 §r§ais now clean!
     */
    private val cleanPlotChatPattern by patternGroup.pattern(
        "chat.clean",
        "§aPlot §r§7- §r§b(?<plot>.*) §r§ais now clean!",
    )

    /**
     * REGEX-TEST: SPRAYONATOR! You sprayed Plot - 6 with Compost!
     * REGEX-TEST: SPRAYONATOR! You sprayed Plot - 7 with 3 Plant Matter!
     * REGEX-TEST: SPRAYONATOR! You sprayed Plot - 8 with 5 Jelly!
     */
    val plotSprayedPattern by patternGroup.pattern(
        "spray.target.colorless",
        "SPRAYONATOR! You sprayed Plot - (?<plot>.+) with (?:(?<amount>\\d+) )?(?<spray>.+)!",
    )

    /**
     * REGEX-TEST: SPLASH! Your Garden was cleared of all active Sprayonator effects!
     */
    private val portableWasherPattern by patternGroup.pattern(
        "spray.cleared.portablewasher-nocolor",
        "SPLASH! Your Garden was cleared of all active Sprayonator effects!",
    )

    /**
     * REGEX-TEST: SPRAYONATOR! The smell of Compost on Plot - 5 ran out!
     * REGEX-TEST: SPRAYONATOR! The smell of Compost on Plot - test plot name ran out!
     */
    private val plotSprayExpiredPattern by patternGroup.pattern(
        "spray.expired",
        "SPRAYONATOR! The smell of (?<spray>[\\w+ ]+) on Plot - (?<plot>.+) ran out!",
    )

    /**
     * REGEX-TEST: Spray: None
     * REGEX-TEST: Spray: Compost (12m)
     * REGEX-TEST: Spray: Compost (1m 3s)
     * REGEX-TEST: Spray: Compost (53s)
     * REGEX-TEST: Spray: Honey Jar (53s)
     */
    private val plotSprayedTablistPattern by patternGroup.pattern(
        "tablist.spraytime-nocolor",
        "Spray: (?<spray>[\\w\\s]+)(?:\\((?<time>.*)\\))?",
    )
    var plots = listOf<GardenPlot>()

    fun fetchCurrentPlot(): GardenPlot? = getPlot(playerLocation())

    fun inGreenhouse(): Boolean {
        return currentPlot?.greenhouse ?: false
    }

    var currentPlot: GardenPlot? = null
        private set

    /**
     * Checks whether the player has moved to a different plot and fires [PlotChangeEvent] if so.
     *
     * [currentPlot] holds the last known plot and is updated only here.
     * [fetchCurrentPlot] computes the plot from the current player position without caching.
     */
    fun checkCurrentPlot() {
        val plot = fetchCurrentPlot()
        if (plot != currentPlot) {
            currentPlot = plot
            updateCurrentPlot()
        }
    }

    private fun updateCurrentPlot() {
        PlotChangeEvent(currentPlot).post()
    }

    class PlotData(
        @Expose
        val id: Int,

        @Expose
        var name: String,

        @Expose
        var pests: Int,

        @Expose
        var sprayExpiryTime: SimpleTimeMark?,

        @Expose
        var sprayType: SprayType?,

        @Expose
        var sprayHasNotified: Boolean,

        @Expose
        var isBeingPasted: Boolean,

        @Expose
        var isPestCountInaccurate: Boolean,

        @Expose
        var locked: Boolean,

        @Expose
        var uncleared: Boolean,

        @Expose
        var greenhouse: Boolean,
    )

    data class SprayData(
        val expiry: SimpleTimeMark,
        val type: SprayType,
    )


    fun getPlot(location: LorenzVec): GardenPlot? {
        if (location.y !in 0.0..<256.0) return null
        val plotX = location.x.toPlotIndex() ?: return null
        val plotZ = location.z.toPlotIndex() ?: return null
        return getPlotByID(plotMap[plotZ][plotX])
    }

    private fun Double.toPlotIndex(): Int? {
        if (this !in PLOT_GRID_MIN..PLOT_GRID_MAX) return null
        if (this >= PLOT_GRID_MAX) return PLOT_GRID_SIZE - 1
        return floor((this - PLOT_GRID_MIN) / PLOT_SIZE).toInt().coerceIn(0, PLOT_GRID_SIZE - 1)
    }

    init {
        val list = mutableListOf<GardenPlot>()
        var slot = 2
        for ((y, rows) in plotMap.withIndex()) {
            for ((x, id) in rows.withIndex()) {
                val minX = ((x - 2) * 96 - 48).toDouble()
                val minY = ((y - 2) * 96 - 48).toDouble()
                val maxX = ((x - 2) * 96 + 48).toDouble()
                val maxY = ((y - 2) * 96 + 48).toDouble()
                val a = LorenzVec(minX, 0.0, minY)
                val b = LorenzVec(maxX, 256.0, maxY)
                val middle = a.middle(b).copy(y = 10.0)
                val box = a.axisAlignedTo(b).inflate(0.0001, 0.0, 0.0001)
                list.add(GardenPlot(id, slot, box, middle))
                slot++
            }
            slot += 4
        }
        plots = list
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        plotSprayedPattern.matchMatcher(event.cleanMessage) {
            val plotName = group("plot")
            val sprayName = group("spray")
            val amount = groupOrNull("amount")?.toIntOrNull() ?: 1

            val plot = getPlotByName(plotName) ?: return
            val spray = SprayType.getByNameOrNull(sprayName) ?: return

            plot.setSpray(spray, 30.minutes)
            GardenPlotSprayEvent.SprayAddedEvent(plot, spray, amount).post()
        }
        plotSprayExpiredPattern.matchMatcher(event.cleanMessage) {
            val sprayName = group("spray")
            val plotName = group("plot")

            val plot = getPlotByName(plotName) ?: return
            val spray = SprayType.getByNameOrNull(sprayName) ?: return
            GardenPlotSprayEvent.SprayExpiredEvent(plot, spray).post()
        }
        cleanPlotChatPattern.matchMatcher(event.message) {
            val plotId = group("plot").toInt()
            val plot = getPlotByID(plotId)
            plot?.uncleared = false
        }
        unlockPlotChatPattern.matchMatcher(event.message) {
            val plotId = group("plot").toInt()
            val plot = getPlotByID(plotId)
            plot?.locked = false
        }

        portableWasherPattern.matchMatcher(event.cleanMessage) {
            for (plot in plots) {
                if (plot.currentSpray != null) {
                    plot.removeSpray()
                }
            }
        }
    }

    private fun getPlotByID(plotId: Int) = plots.firstOrNull { it.id == plotId }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "Configure Plots") return

        for (plot in plots) {
            val itemStack = event.inventoryItems[plot.inventorySlot] ?: continue
            val lore = itemStack.getLore()
            plotNamePattern.matchMatcher(itemStack.hoverName.formattedTextCompatLeadingWhiteLessResets()) {
                val plotName = group("name")
                plot.name = plotName
            }
            barnNamePattern.matchMatcher(itemStack.hoverName.formattedTextCompatLeadingWhiteLessResets()) {
                plot.name = group("name")
            }
            plot.locked = false
            plot.isBeingPasted = false
            plot.greenhouse = false
            for (line in lore) {
                if (line.contains("§7Cost:")) plot.locked = true
                if (line.contains("§7Pasting in progress:")) plot.isBeingPasted = true
                plot.uncleared = false
                uncleanedPlotPattern.matchMatcher(line) {
                    plot.uncleared = true
                }
                greenhousePlotPattern.matchMatcher(line) {
                    plot.greenhouse = true
                }
            }
        }
    }

    @HandleEvent
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.PESTS)) return
        val plot = fetchCurrentPlot() ?: return
        if (plot.isBarn()) return

        plotSprayedTablistPattern.firstMatcher(event.lines.map { it.string.trim() }) {
            val sprayName = group("spray").trim()
            val time = groupOrNull("time")?.let { getTablistEndTime(it, plot.currentSpray?.expiry) }
            if (time == null) {
                plot.removeSpray()
                return
            }

            val newSpray: SprayType? = SprayType.getByNameOrNull(sprayName)

            val spray = plot.currentSpray
            if (spray != null) {
                if (newSpray == null) {
                    plot.removeSpray()
                    return
                } else {
                    GardenPlotSprayDataTablistReadEvent(plot.name, spray, newSpray, time).post()
                    plot.setSpray(newSpray, time.timeUntil())
                }
            } else {
                if (newSpray == null) return
                GardenPlotSprayDataTablistReadEvent(plot.name, spray, newSpray, time).post()
                plot.setSpray(newSpray, time.timeUntil())
            }
        }
    }

    @HandleEvent
    fun onPlotChange(event: PlotChangeEvent) {
        DelayedRun.runDelayed(3.seconds) {
            TabWidget.forceUpdateWidget(TabWidget.PESTS)
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onPlayerMove(event: EntityMoveEvent<LocalPlayer>) {
        DelayedRun.runDelayed(.5.seconds) {
            checkCurrentPlot()
        }
    }

    fun getPlotByName(plotName: String) = plots.firstOrNull { it.name == plotName }

    fun SkyHanniRenderWorldEvent.renderPlot(
        plot: GardenPlot,
        lineColor: Color,
        cornerColor: Color,
        showBuildLimit: Boolean = false,
    ) {

        // These don't refer to Minecraft chunks but rather garden plots, but I use
        // the word chunk as the logic closely represents how chunk borders are rendered in latter mc versions
        val plotSize = 96
        val chunkX = floor((plot.middle.x + 48) / plotSize).toInt()
        val chunkZ = floor((plot.middle.z + 48) / plotSize).toInt()
        val chunkMinX = (chunkX * plotSize) - 48
        val chunkMinZ = (chunkZ * plotSize) - 48

        // Lowest point in the garden
        val minHeight = 66
        val maxHeight = 66 + 36

        // Render 4 vertical corners
        for (i in 0..plotSize step plotSize) {
            for (j in 0..plotSize step plotSize) {
                val start = LorenzVec(chunkMinX + i, minHeight, chunkMinZ + j)
                val end = LorenzVec(chunkMinX + i, maxHeight, chunkMinZ + j)
                tryDraw3DLine(start, end, cornerColor, 3, true)
            }
        }

        // Render vertical on X-Axis
        for (x in 4..<plotSize step 4) {
            val start = LorenzVec(chunkMinX + x, minHeight, chunkMinZ)
            val end = LorenzVec(chunkMinX + x, maxHeight, chunkMinZ)
            // Front lines
            tryDraw3DLine(start, end, lineColor, 2, true)
            // Back lines
            tryDraw3DLine(start.add(z = plotSize), end.add(z = plotSize), lineColor, 2, true)
        }

        // Render vertical on Z-Axis
        for (z in 4..<plotSize step 4) {
            val start = LorenzVec(chunkMinX, minHeight, chunkMinZ + z)
            val end = LorenzVec(chunkMinX, maxHeight, chunkMinZ + z)
            // Left lines
            tryDraw3DLine(start, end, lineColor, 2, true)
            // Right lines
            tryDraw3DLine(start.add(x = plotSize), end.add(x = plotSize), lineColor, 2, true)
        }

        // Render horizontal
        val buildLimit = minHeight + 11
        val iterable = if (showBuildLimit) {
            (minHeight..maxHeight step 4) + buildLimit
        } else {
            minHeight..maxHeight step 4
        }
        for (y in iterable) {
            val start = LorenzVec(chunkMinX, y, chunkMinZ)
            val isRedLine = y == buildLimit
            val color = if (isRedLine) Color.red else lineColor
            val depth = if (isRedLine) 3 else 2
            // (minX, minZ) -> (minX, minZ + 96)
            tryDraw3DLine(start, start.add(z = plotSize), color, depth, true)
            // (minX, minZ + 96) -> (minX + 96, minZ + 96)
            tryDraw3DLine(start.add(z = plotSize), start.add(x = plotSize, z = plotSize), color, depth, true)
            // (minX + 96, minZ + 96) -> (minX + 96, minZ)
            tryDraw3DLine(start.add(x = plotSize, z = plotSize), start.add(x = plotSize), color, depth, true)
            // (minX + 96, minZ) -> (minX, minZ)
            tryDraw3DLine(start.add(x = plotSize), start, color, depth, true)
        }
    }

    private fun SkyHanniRenderWorldEvent.tryDraw3DLine(
        p1: LorenzVec,
        p2: LorenzVec,
        color: Color,
        lineWidth: Int,
        depth: Boolean,
    ) {
        if (isOutOfBorders(p1)) return
        if (isOutOfBorders(p2)) return
        draw3DLine(p1, p2, color, lineWidth, depth)
    }

    private fun isOutOfBorders(location: LorenzVec) = when {
        location.x > 240 -> true
        location.x < -240 -> true
        location.z > 240 -> true
        location.z < -240 -> true

        else -> false
    }
}
