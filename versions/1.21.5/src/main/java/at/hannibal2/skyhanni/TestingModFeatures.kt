package at.hannibal2.skyhanni

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawColor
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawSphereWireframeInWorld
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.renderBeaconBeam

@SkyHanniModule
object TestingModFeatures {

    init {
        println("TestingModFeatures loaded")
    }

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
//         val location = LorenzVec(0, -58, 0)
//
//         event.drawString(
//             location,
//             "Test String",
//             seeThroughBlocks = true,
//             LorenzColor.GREEN.toColor(),
//         )
//
//         event.drawString(
//             location + LorenzVec(0, 0, 1),
//             "Test String 2",
//             seeThroughBlocks = false,
//             LorenzColor.DARK_GREEN.toColor(),
//         )
//
//         event.drawColor(
//             location + LorenzVec(0, 0, 2),
//             LorenzColor.AQUA,
//             beacon = false,
//             seeThroughBlocks = false,
//         )
//
//         event.drawColor(
//             location + LorenzVec(0, 0, 3),
//             LorenzColor.YELLOW,
//             beacon = true,
//             seeThroughBlocks = true,
//         )
//
//         event.drawColor(
//             location + LorenzVec(0, 0, 4),
//             LorenzColor.DARK_PURPLE,
//             beacon = true,
//             seeThroughBlocks = false,
//         )
//
//         event.drawColor(
//             location + LorenzVec(0, 0, 5),
//             LorenzColor.WHITE,
//             beacon = false,
//             seeThroughBlocks = true,
//         )
//
//         event.renderBeaconBeam(
//             location + LorenzVec(0, 0, 6),
//             LorenzColor.RED.toColor().rgb,
//         )
//
//         event.drawWaypointFilled(
//             location + LorenzVec(0, 0, 7),
//             LorenzColor.BLUE.toColor(),
//             seeThroughBlocks = true,
//             beacon = true
//         )
//
//         event.drawWaypointFilled(
//             location + LorenzVec(0, 0, 8),
//             LorenzColor.LIGHT_PURPLE.toColor(),
//             seeThroughBlocks = false,
//             beacon = false,
//         )
//
//         event.drawWaypointFilled(
//             location + LorenzVec(0, 0, 9),
//             LorenzColor.GRAY.toColor(),
//             seeThroughBlocks = true,
//             beacon = false,
//         )
//
//         event.drawWaypointFilled(
//             location + LorenzVec(0, 0, 10),
//             LorenzColor.BLACK.toColor(),
//             seeThroughBlocks = false,
//             beacon = true,
//         )
//
//         val playerLocation = LocationUtils.playerLocation()
//
//         event.drawSphereWireframeInWorld(LorenzColor.GREEN.toColor(), playerLocation, 16f)
    }
}
