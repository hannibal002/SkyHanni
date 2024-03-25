package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LocationUtils.playerLocation
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.exactPlayerEyeLocation
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.client.Minecraft
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

class PestParticleWaypoint {

    private val config get() = SkyHanniMod.feature.garden.pests.pestWaypoint

    private var lastPestTrackerUse = SimpleTimeMark.farPast()

    private var firstParticlePoint: LorenzVec? = null
    private var secondParticlePoint: LorenzVec? = null
    private var lastParticlePoint: LorenzVec? = null
    private var guessPoint: LorenzVec? = null
    private var locations = mutableListOf<LorenzVec>()
    private var particles = 0
    private var lastParticles = 0

    @SubscribeEvent
    fun onItemClick(event: ItemClickEvent) {
        if (!isEnabled()) return
        if (PestAPI.hasVacuumInHand()) {
            if (event.clickType == ClickType.LEFT_CLICK && !Minecraft.getMinecraft().thePlayer.isSneaking) {
                lastPestTrackerUse = SimpleTimeMark.now()
                reset()
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        lastPestTrackerUse = SimpleTimeMark.farPast()
        reset()
    }

    private fun reset() {
        locations.clear()
        guessPoint = null
        lastParticlePoint = null
        firstParticlePoint = null
        secondParticlePoint = null
        particles = 0
        lastParticles = 0
    }

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        if (event.type != EnumParticleTypes.REDSTONE) return
        val location = event.location
        if (config.hideParticles) event.cancel()
        if (lastPestTrackerUse.passedSince() > 3.seconds) return

        if (particles > 5) return
        if (firstParticlePoint == null) {
            if (playerLocation().distance(location) > 5) return
            firstParticlePoint = location
        } else if (secondParticlePoint == null) {
            secondParticlePoint = location
            lastParticlePoint = location
            locations.add(location)
        } else {
            val firstDistance = secondParticlePoint?.let { firstParticlePoint?.distance(it) } ?: return
            val distance = lastParticlePoint?.distance(location) ?: return
            if ((distance-firstDistance).absoluteValue > 0.1) return
            lastParticlePoint = location
            locations.add(location)
        }
        ++particles
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        if (locations.isEmpty()) return
        if (lastPestTrackerUse.passedSince() > config.showWaypointForSeconds.seconds) {
            reset()
            return
        }
        val waypoint = if (lastParticles != particles || guessPoint == null) {
            getWaypoint(locations).also {
                guessPoint = it
                lastParticles = particles
            }
        } else {
            guessPoint ?: return
        }
        event.drawWaypointFilled(waypoint, Color(255, 0, 255,100), beacon = true)
        event.drawDynamicText(waypoint, "§cPest Guess", 1.3)
        if (config.drawLine) event.draw3DLine(event.exactPlayerEyeLocation(), waypoint, LorenzColor.AQUA.toColor(), 3, false)
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (event.repeatSeconds(1)) {
            if ((guessPoint?.distanceToPlayer() ?: return) < 8.0 && lastPestTrackerUse.passedSince() > 1.seconds) {
                lastPestTrackerUse = SimpleTimeMark.farPast()
                reset()
            }
        }
    }

    private fun getWaypoint(list: MutableList<LorenzVec>): LorenzVec {
        var pos = LorenzVec(0.0,0.0,0.0)

        val firstParticle = firstParticlePoint
        if (firstParticle?.x == null) return pos

        for ((i, particle) in list.withIndex()) {
            pos = pos.add((particle.subtract(firstParticle)).divide(i.toDouble()+1.0))
        }
        pos = firstParticle.add(pos.multiply(120.0/list.size))

        return pos
    }

    fun isEnabled() = GardenAPI.inGarden() && config.enabled

}
