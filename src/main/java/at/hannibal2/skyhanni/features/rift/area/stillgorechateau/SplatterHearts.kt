package at.hannibal2.skyhanni.features.rift.area.stillgorechateau

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.VectorUtils.axisAlignedTo
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.phys.Vec3
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object SplatterHearts {
    private val config get() = RiftApi.config.area.stillgoreChateau
    private var lastHearts = SimpleTimeMark.farPast()

    private var shownHearts = emptySet<Vec3>()
    private val currentHearts = mutableSetOf<Vec3>()

    @HandleEvent
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        if (event.type != ParticleTypes.HEART) return
        if (event.count != 3 || event.speed != 0f) return

        if (lastHearts.passedSince() > 50.milliseconds) {
            shownHearts = currentHearts.toSet()
            currentHearts.clear()
        }
        lastHearts = SimpleTimeMark.now()
        currentHearts += event.location
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        if (lastHearts.passedSince() > 300.milliseconds) return
        shownHearts.forEach {
            val pos = it.add(-0.5, 0.3, -0.5)
            val aabb = pos.axisAlignedTo(pos.add(1.0))
            // TODO add chroma color support via config
            event.drawFilledBoundingBox(aabb, LorenzColor.RED.addOpacity(100).toChromaColor())
        }
    }

    private fun isEnabled() = RiftApi.inRift() && RiftApi.inStillgoreChateau() && config.highlightSplatterHearts
}
