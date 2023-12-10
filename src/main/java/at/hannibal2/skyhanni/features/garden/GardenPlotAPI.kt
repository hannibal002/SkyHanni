package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.features.misc.LockMouseLook
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LocationUtils.isPlayerInside
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.annotations.Expose
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.math.floor

object GardenPlotAPI {

    private val plotNamePattern by RepoPattern.pattern("garden.plot.name", "§.Plot §7- §b(?<name>.*)")

    var plots = listOf<Plot>()

    fun getCurrentPlot(): Plot? {
        return plots.firstOrNull { it.isPlayerInside() }
    }

    class Plot(val id: Int, var inventorySlot: Int, val box: AxisAlignedBB, val middle: LorenzVec)

    class PlotData(
        @Expose
        val id: Int,

        @Expose
        var name: String,

        @Expose
        var pests: Int
    )

    private fun Plot.getData() = GardenAPI.storage?.plotData?.getOrPut(id) { PlotData(id, "$id", 0) }

    var Plot.name: String
        get() = getData()?.name ?: "$id"
        set(value) {
            getData()?.name = value
        }

    var Plot.pests: Int
        get() = getData()?.pests ?: 0
        set(value) {
            getData()?.pests = value
        }

    fun Plot.isBarn() = id == -1

    fun Plot.isPlayerInside() = box.isPlayerInside()

    fun Plot.sendTeleportTo() {
        LorenzUtils.sendCommandToServer("tptoplot $name")
        LockMouseLook.autoDisable()
    }

    init {
        val plotMap = listOf(
            listOf(21, 13, 9, 14, 22),
            listOf(15, 5, 1, 6, 16),
            listOf(10, 2, -1, 3, 11),
            listOf(17, 7, 4, 8, 18),
            listOf(23, 19, 12, 20, 24),
        )
        val list = mutableListOf<Plot>()
        var slot = 2
        for ((y, rows) in plotMap.withIndex()) {
            for ((x, id) in rows.withIndex()) {
                val minX = ((x - 2) * 96 - 48).toDouble()
                val minY = ((y - 2) * 96 - 48).toDouble()
                val maxX = ((x - 2) * 96 + 48).toDouble()
                val maxY = ((y - 2) * 96 + 48).toDouble()
                val a = LorenzVec(minX, 0.0, minY)
                val b = LorenzVec(maxX, 256.0, maxY)
                val middle = a.interpolate(b, 0.5).copy(y = 10.0)
                val box = a.axisAlignedTo(b).expand(0.0001, 0.0, 0.0001)
                list.add(Plot(id, slot, box, middle))
                slot++
            }
            slot += 4
        }
        plots = list
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!GardenAPI.inGarden()) return
        if (event.inventoryName != "Configure Plots") return

        for (plot in plots) {
            val itemName = event.inventoryItems[plot.inventorySlot]?.name ?: continue
            plotNamePattern.matchMatcher(itemName) {
                plot.name = group("name")
            }
        }
    }

    fun getPlotByName(plotName: String) = plots.firstOrNull { it.name == plotName }

    fun LorenzRenderWorldEvent.renderPlot(plot: Plot, lineColor: Color, cornerColor: Color) {

        // These don't refer to Minecraft chunks but rather garden plots, but I use
        // the word chunk as the logic closely represents how chunk borders are rendered in latter mc versions
        val plotSize = 96
        val chunkX = floor((plot.middle.x + 48) / plotSize).toInt()
        val chunkZ = floor((plot.middle.z + 48) / plotSize).toInt()
        val chunkMinX = (chunkX * plotSize) - 48
        val chunkMinZ = (chunkZ * plotSize) - 48

        // Lowest point in the garden
        val minHeight = 66
        val maxHeight = 256

        // Render 4 vertical corners
        for (i in 0..plotSize step plotSize) {
            for (j in 0..plotSize step plotSize) {
                val start = LorenzVec(chunkMinX + i, minHeight, chunkMinZ + j)
                val end = LorenzVec(chunkMinX + i, maxHeight, chunkMinZ + j)
                tryDraw3DLine(start, end, cornerColor, 2, true)
            }
        }

        // Render vertical on X-Axis
        for (x in 4..<plotSize step 4) {
            val start = LorenzVec(chunkMinX + x, minHeight, chunkMinZ)
            val end = LorenzVec(chunkMinX + x, maxHeight, chunkMinZ)
            // Front lines
            tryDraw3DLine(start, end, lineColor, 1, true)
            // Back lines
            tryDraw3DLine(start.add(z = plotSize), end.add(z = plotSize), lineColor, 1, true)
        }

        // Render vertical on Z-Axis
        for (z in 4..<plotSize step 4) {
            val start = LorenzVec(chunkMinX, minHeight, chunkMinZ + z)
            val end = LorenzVec(chunkMinX, maxHeight, chunkMinZ + z)
            // Left lines
            tryDraw3DLine(start, end, lineColor, 1, true)
            // Right lines
            tryDraw3DLine(start.add(x = plotSize), end.add(x = plotSize), lineColor, 1, true)
        }

        // Render horizontal
        for (y in minHeight..maxHeight step 4) {
            val start = LorenzVec(chunkMinX, y, chunkMinZ)
            // (minX, minZ) -> (minX, minZ + 96)
            tryDraw3DLine(start, start.add(z = plotSize), lineColor, 1, true)
            // (minX, minZ + 96) -> (minX + 96, minZ + 96)
            tryDraw3DLine(start.add(z = plotSize), start.add(x = plotSize, z = plotSize), lineColor, 1, true)
            // (minX + 96, minZ + 96) -> (minX + 96, minZ)
            tryDraw3DLine(start.add(x = plotSize, z = plotSize), start.add(x = plotSize), lineColor, 1, true)
            // (minX + 96, minZ) -> (minX, minZ)
            tryDraw3DLine(start.add(x = plotSize), start, lineColor, 1, true)
        }
    }

    private fun LorenzRenderWorldEvent.tryDraw3DLine(
        p1: LorenzVec,
        p2: LorenzVec,
        color: Color,
        lineWidth: Int,
        depth: Boolean
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
