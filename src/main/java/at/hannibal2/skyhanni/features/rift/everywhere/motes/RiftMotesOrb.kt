package at.hannibal2.skyhanni.features.rift.everywhere.motes

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class RiftMotesOrb {
    private val config get() = RiftAPI.config.motesOrbsConfig

    // TODO USE SH-REPO
    private val pattern = "§5§lORB! §r§dPicked up §r§5+.* Motes§r§d.*".toPattern()

    private var motesOrbs = emptyList<MotesOrb>()

    class MotesOrb(
        var location: LorenzVec,
        var counter: Int = 0,
        var startTime: Long = System.currentTimeMillis(),
        var lastTime: Long = System.currentTimeMillis(),
        var isOrb: Boolean = false,
        var pickedUp: Boolean = false,
    )

    @SubscribeEvent
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        val location = event.location.add(-0.5, 0.0, -0.5)

        if (event.type == EnumParticleTypes.SPELL_MOB) {
            val orb =
                motesOrbs.find { it.location.distance(location) < 3 } ?: MotesOrb(location).also {
                    motesOrbs = motesOrbs.editCopy { add(it) }
                }

            orb.location = location
            orb.lastTime = System.currentTimeMillis()
            orb.counter++
            orb.pickedUp = false
            if (config.hideParticles && orb.isOrb) {
                event.isCanceled = true
            }
        }
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        pattern.matchMatcher(event.message) {
            motesOrbs.minByOrNull { it.location.distanceToPlayer() }?.let {
                it.pickedUp = true
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return

        motesOrbs = motesOrbs.editCopy { removeIf { System.currentTimeMillis() > it.lastTime + 2000 } }

        for (orb in motesOrbs) {
            val ageInSeconds = (System.currentTimeMillis() - orb.startTime).toDouble() / 1000
            if (ageInSeconds < 0.5) continue

            val particlesPerSecond = (orb.counter.toDouble() / ageInSeconds).round(1)
            if (particlesPerSecond < 60 || particlesPerSecond > 90) continue
            orb.isOrb = true

            if (System.currentTimeMillis() > orb.lastTime + 300) {
                orb.pickedUp = true
            }

            val location = orb.location

            if (orb.pickedUp) {
                event.drawDynamicText(location.add(0.0, 0.5, 0.0), "§7Motes Orb", 1.5, ignoreBlocks = false)
                event.drawWaypointFilled(location, LorenzColor.GRAY.toColor())
            } else {
                event.drawDynamicText(location.add(0.0, 0.5, 0.0), "§dMotes Orb", 1.5, ignoreBlocks = false)
                event.drawWaypointFilled(location, LorenzColor.LIGHT_PURPLE.toColor())
            }
        }
    }

    fun isEnabled() = RiftAPI.inRift() && config.enabled
}
