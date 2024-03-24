package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.LocationUtils.playerLocation
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.client.Minecraft
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

class PestParticleWaypoint {

    private var lastPestTrackerUse = SimpleTimeMark.farPast()

    private var firstParticlePoint: LorenzVec? = null
    private var secondParticlePoint: LorenzVec? = null
    private var lastParticlePoint: LorenzVec? = null
    private var guessPoint: LorenzVec? = null
    private var locs = mutableListOf<LorenzVec>()
    private var particles = 0
    private var lastParticles = 0

    @SubscribeEvent
    fun onItemClick(event: ItemClickEvent) {
        if (!isEnabled()) return
        if (PestAPI.hasVacuumInHand()) {
            if (event.clickType == ClickType.LEFT_CLICK && !Minecraft.getMinecraft().thePlayer.isSneaking) {
                lastPestTrackerUse = SimpleTimeMark.now()
                locs.clear()
                guessPoint = null
                lastParticlePoint = null
                firstParticlePoint = null
                secondParticlePoint = null
                particles = 0
            }
        }
    }

    @SubscribeEvent
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        // TODO time in config
        if (lastPestTrackerUse.passedSince() > 5.seconds) return

        if (event.type != EnumParticleTypes.REDSTONE) return
        val location = event.location

        if (particles > 5) return
        if (firstParticlePoint == null) {
            if (playerLocation().distance(location) > 5) return
            firstParticlePoint = location
            ++particles
        } else if (secondParticlePoint == null) {
            secondParticlePoint = location
            lastParticlePoint = location
            locs.add(location)
            ++particles
        } else {
            val firstDistance = secondParticlePoint?.let { firstParticlePoint?.distance(it) } ?: return
            val distance = lastParticlePoint?.distance(location) ?: return
            if ((distance-firstDistance).absoluteValue > 0.1) return
            lastParticlePoint = location
            locs.add(location)
            ++particles
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        // TODO time in config
        if (lastPestTrackerUse.passedSince() > 20.seconds) {
            locs.clear()
            guessPoint = null
            return
        }

        if (locs.isEmpty()) return
        var waypoint = guessPoint ?: getWaypoint(locs)
        if (lastParticles != particles) waypoint = getWaypoint(locs)
        guessPoint = waypoint
        event.drawWaypointFilled(waypoint, Color(255, 0, 255,100), beacon = true)
        event.drawDynamicText(waypoint, "§cAccurate Guess", 1.3)
    }

    private fun getWaypoint(list: MutableList<LorenzVec>): LorenzVec {
        var x = 0.0
        var y = 0.0
        var z = 0.0
        var i = 1

        val firstParticle = firstParticlePoint
        if (firstParticle?.x == null) return LorenzVec(0, 0, 0)

        for (particle in list) {
            x += (particle.x - firstParticle.x) / i
            y += (particle.y - firstParticle.y) / i
            z += (particle.z - firstParticle.z) / i
            ++i
        }
        x = (x / list.size) * 120
        y = (y / list.size) * 120
        z = (z / list.size) * 120

        return LorenzVec(firstParticle.x+x, firstParticle.y+y, firstParticle.z+z)
    }

    // TODO toggle
    fun isEnabled() = GardenAPI.inGarden()

}
