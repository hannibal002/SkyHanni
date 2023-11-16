package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzKeyPressEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard
import java.awt.Color
import kotlin.math.floor
import kotlin.time.Duration.Companion.milliseconds

class GardenPlotBorders {

    private val config get() = SkyHanniMod.feature.garden.plotBorders
    private var timeLastSaved = SimpleTimeMark.farPast()
    private var showBorders = false
    private val LINE_COLOR = LorenzColor.YELLOW.toColor()

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

        val entity = Minecraft.getMinecraft().renderViewEntity

        // Lowest point in garden
        val minHeight = 66
        val maxHeight = 256

        // These don't refer to Minecraft chunks but rather garden plots, but I use
        // the word chunk as the logic closely represents how chunk borders are rendered in latter mc versions
        val chunkX = floor((entity.posX + 48) / 96).toInt()
        val chunkZ = floor((entity.posZ + 48) / 96).toInt()
        val chunkMinX = (chunkX * 96) - 48
        val chunkMinZ = (chunkZ * 96) - 48

        // Render 4 vertical corners
        for (i in 0..96 step 96) {
            for (j in 0..96 step 96) {
                val start = LorenzVec(chunkMinX + i, minHeight, chunkMinZ + j)
                val end = LorenzVec(chunkMinX + i, maxHeight, chunkMinZ + j)
                event.tryDraw3DLine(start, end, LorenzColor.DARK_BLUE.toColor(), 2, true)
            }
        }

        // Render vertical on X-Axis
        for (x in 4..<96 step 4) {
            val start = LorenzVec(chunkMinX + x, minHeight, chunkMinZ)
            val end = LorenzVec(chunkMinX + x, maxHeight, chunkMinZ)
            // Front lines
            event.tryDraw3DLine(start, end, LINE_COLOR, 1, true)
            // Back lines
            event.tryDraw3DLine(start.addZ(96), end.addZ(96), LINE_COLOR, 1, true)
        }

        // Render vertical on Z-Axis
        for (z in 4..<96 step 4) {
            val start = LorenzVec(chunkMinX, minHeight, chunkMinZ + z)
            val end = LorenzVec(chunkMinX, maxHeight, chunkMinZ + z)
            // Left lines
            event.tryDraw3DLine(start, end, LINE_COLOR, 1, true)
            // Right lines
            event.tryDraw3DLine(start.addX(96), end.addX(96), LINE_COLOR, 1, true)
        }

        // Render horizontal
        for (y in minHeight..maxHeight step 4) {
            val start = LorenzVec(chunkMinX, y, chunkMinZ)
            // (minX, minZ) -> (minX, minZ + 96)
            event.tryDraw3DLine(start, start.addZ(96), LINE_COLOR, 1, true)
            // (minX, minZ + 96) -> (minX + 96, minZ + 96)
            event.tryDraw3DLine(start.addZ(96), start.addXZ(96, 96), LINE_COLOR, 1, true)
            // (minX + 96, minZ + 96) -> (minX + 96, minZ)
            event.tryDraw3DLine(start.addXZ(96, 96), start.addX(96), LINE_COLOR, 1, true)
            // (minX + 96, minZ) -> (minX, minZ)
            event.tryDraw3DLine(start.addX(96), start, LINE_COLOR, 1, true)
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
