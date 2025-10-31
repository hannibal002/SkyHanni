package at.hannibal2.hanni.features.rift.area.stillgorechateau

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.ReceiveParticleEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.features.rift.RiftApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ColorUtils.toChromaColor
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import net.minecraft.util.EnumParticleTypes
import kotlin.time.Duration.Companion.milliseconds

@HanniModule
object SplatterHearts {
    private val config get() = RiftApi.config.area.stillgoreChateau
    private var lastHearts = SimpleTimeMark.farPast()

    private var shownHearts = setOf<LorenzVec>()
    private val currentHearts = mutableSetOf<LorenzVec>()

    @HandleEvent
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        if (event.type != EnumParticleTypes.HEART) return
        if (event.count != 3 || event.speed != 0f) return

        if (lastHearts.passedSince() > 50.milliseconds) {
            shownHearts = currentHearts.toSet()
            currentHearts.clear()
        }
        lastHearts = SimpleTimeMark.now()
        currentHearts += event.location
    }

    @HandleEvent
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!isEnabled()) return
        if (lastHearts.passedSince() > 300.milliseconds) return
        shownHearts.forEach {
            val pos = it.add(-0.5, 0.3, -0.5)
            val aabb = pos.axisAlignedTo(pos.add(1, 1, 1))
            // TODO add chroma color support via config
            event.drawFilledBoundingBox(aabb, LorenzColor.RED.addOpacity(100).toChromaColor())
        }
    }

    private fun isEnabled() = RiftApi.inRift() && RiftApi.inStillgoreChateau() && config.highlightSplatterHearts
}
