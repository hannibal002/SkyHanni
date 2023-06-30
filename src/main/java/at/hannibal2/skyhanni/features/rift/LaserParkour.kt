package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class LaserParkour {
    private val config get() = SkyHanniMod.feature.rift
    private val puzzleRoom = AxisAlignedBB(-298.0, 0.0, -112.0, -309.0, 63.0, -101.0)

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!RiftAPI.inRift()) return
        if (!config.laserParkour) return
        if (!puzzleRoom.isVecInside(Minecraft.getMinecraft().thePlayer.positionVector)) return
        for (location in locations) {
            event.drawWaypointFilled(location, LorenzColor.GREEN.toColor())
        }
    }

    private var locations = mutableListOf(
        LorenzVec(-304.0, 2.0, -107.0),
        LorenzVec(-306.0, 4.0, -103.0),
        LorenzVec(-308.0, 6.0, -105.0),
        LorenzVec(-304.0, 8.0, -109.0),
        LorenzVec(-300.0, 10.0, -111.0),
        LorenzVec(-304.0, 12.0, -107.0),
        LorenzVec(-308.0, 14.0, -103.0),
        LorenzVec(-306.0, 16.0, -107.0),
        LorenzVec(-302.0, 18.0, -111.0),
        LorenzVec(-300.0, 20.0, -107.0),
        LorenzVec(-304.0, 22.0, -111.0),
        LorenzVec(-306.0, 24.0, -109.0),
        LorenzVec(-302.0, 26.0, -111.0),
        LorenzVec(-300.0, 28.0, -107.0),
        LorenzVec(-304.0, 30.0, -103.0),
        LorenzVec(-306.0, 32.0, -105.0),
        LorenzVec(-302.0, 34.0, -107.0),
        LorenzVec(-300.0, 36.0, -109.0),
        LorenzVec(-302.0, 38.0, -105.0),
        LorenzVec(-304.0, 40.0, -107.0),
        LorenzVec(-306.0, 42.0, -111.0),
        LorenzVec(-302.0, 44.0, -107.0),
        LorenzVec(-300.0, 46.0, -103.0),
        LorenzVec(-304.0, 48.0, -107.0),
        LorenzVec(-308.0, 50.0, -105.0),
        LorenzVec(-304.0, 52.0, -109.0),
        LorenzVec(-306.0, 54.0, -111.0)
    )
}