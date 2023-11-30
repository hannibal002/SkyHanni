package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.events.LorenzKeyPressEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard
import java.awt.Color
import kotlin.math.floor
import kotlin.time.Duration.Companion.milliseconds

object GardenPlotBorders {

    private val config get() = GardenAPI.config.plotBorders
    private var timeLastSaved = SimpleTimeMark.farPast()
    private var showBorders = false

    private fun LorenzVec.addX(x: Int) = add(x, 0, 0)
    private fun LorenzVec.addZ(z: Int) = add(0, 0, z)
    private fun LorenzVec.addXZ(x: Int, z: Int) = add(x, 0, z)

    @SubscribeEvent
    fun onKeyClick(event: LorenzKeyPressEvent) {
        if (!isEnabled()) return
        if (timeLastSaved.passedSince() < 250.milliseconds) return

        if (event.keyCode == Keyboard.KEY_G && Keyboard.isKeyDown(Keyboard.KEY_F3)) {
            timeLastSaved = SimpleTimeMark.now()
            showBorders = !showBorders
        }
        if (event.keyCode == Keyboard.KEY_F3 && Keyboard.isKeyDown(Keyboard.KEY_G)) {
            timeLastSaved = SimpleTimeMark.now()
            showBorders = !showBorders
        }
    }

    @SubscribeEvent
    fun render(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        if (!showBorders) return
        val plot = GardenPlotAPI.getCurrentPlot() ?: return
        event.render(plot, LorenzColor.YELLOW.toColor(), LorenzColor.DARK_BLUE.toColor())
    }

    fun LorenzRenderWorldEvent.render(plot: GardenPlotAPI.Plot, lineColor: Color, cornerColor: Color) {

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
            tryDraw3DLine(start.addZ(plotSize), end.addZ(plotSize), lineColor, 1, true)
        }

        // Render vertical on Z-Axis
        for (z in 4..<plotSize step 4) {
            val start = LorenzVec(chunkMinX, minHeight, chunkMinZ + z)
            val end = LorenzVec(chunkMinX, maxHeight, chunkMinZ + z)
            // Left lines
            tryDraw3DLine(start, end, lineColor, 1, true)
            // Right lines
            tryDraw3DLine(start.addX(plotSize), end.addX(plotSize), lineColor, 1, true)
        }

        // Render horizontal
        for (y in minHeight..maxHeight step 4) {
            val start = LorenzVec(chunkMinX, y, chunkMinZ)
            // (minX, minZ) -> (minX, minZ + 96)
            tryDraw3DLine(start, start.addZ(plotSize), lineColor, 1, true)
            // (minX, minZ + 96) -> (minX + 96, minZ + 96)
            tryDraw3DLine(start.addZ(plotSize), start.addXZ(plotSize, plotSize), lineColor, 1, true)
            // (minX + 96, minZ + 96) -> (minX + 96, minZ)
            tryDraw3DLine(start.addXZ(plotSize, plotSize), start.addX(plotSize), lineColor, 1, true)
            // (minX + 96, minZ) -> (minX, minZ)
            tryDraw3DLine(start.addX(plotSize), start, lineColor, 1, true)
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

    fun isEnabled() = GardenAPI.inGarden() && config
}
