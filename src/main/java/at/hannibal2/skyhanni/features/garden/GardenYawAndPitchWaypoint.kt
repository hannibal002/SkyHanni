package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.exactLocation2
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object GardenYawAndPitchWaypoint {

    var enabled = false

    var targetYaw = 0.0
    var targetPitch = 0.0
    var targetDistance = 0.0

    fun command(yaw: Double, pitch: Double, distance: Double) {
        targetYaw = yaw
        targetPitch = pitch
        targetDistance = distance
        enabled = !enabled
        val status = if (enabled) "enabled" else "disabled"
        ChatUtils.chat("yaw and pitch waypoint is now $status")
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        if (!enabled) return
        val playerLocation = LocationUtils.playerEyeLocation()

        val direction = LorenzVec.getFromYawPitch(targetYaw, targetPitch).normalize().multiply(targetDistance)
        val lookingDirection = lookingAt().normalize().multiply(targetDistance)
        val target = event.exactLocation2(playerLocation.add(direction))
        val lookingAt = event.exactLocation2(playerLocation.add(lookingDirection))

        val distance = playerLocation.distance(target).round(1)
        val distanceWrong = lookingAt.distance(target).round(1)

        event.drawWaypointFilled(target, LorenzColor.GREEN.toColor(), seeThroughBlocks = true)
        event.drawDynamicText(target, "§aLook at Me!", 1.5, yOff = -5f)
        event.drawWaypointFilled(lookingAt, LorenzColor.WHITE.toColor(), seeThroughBlocks = true)
        event.drawDynamicText(lookingAt, "§ayou look at!", 1.5, yOff = -15f)

        event.drawDynamicText(target, "§b($distance/$distanceWrong)", 1.5, yOff = 20f)
    }

    private fun lookingAt() =
        LorenzVec.getFromYawPitch(GardenYawAndPitch.lastYaw.toDouble(), GardenYawAndPitch.lastPitch.toDouble())

    fun isEnabled() = LorenzUtils.inSkyBlock
}
