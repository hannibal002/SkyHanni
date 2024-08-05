package at.hannibal2.skyhanni.features.rift.area.stillgorechateau

import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox_nea
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeLimitedSet
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SplatterHearts {

    private const val MAX_HEARTS = 4
    private var currentMax = 0

    // TODO: add OwnMaxHealthUpdateEvent
    // use that to detect when the thing happens

    private var lastHearts = SimpleTimeMark.farPast()
    private var lastSpawn = SimpleTimeMark.farPast()

    private var spawning = false
    private var spawned = 0

    private var currentIndex = 0

    private data class SplatterHeart(val location: LorenzVec, var index: Int)

    private val hearts = TimeLimitedSet<SplatterHeart>(1500.milliseconds)

    @SubscribeEvent
    fun onParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        if (event.type != EnumParticleTypes.HEART) return
        if (event.count != 3 || event.speed != 0f) return

        if (lastHearts.passedSince() > 2.seconds) {
            println("spawning: true")
            spawning = true
            currentIndex = 0
            spawned = 0
            lastSpawn = SimpleTimeMark.now()
        }
        if (spawning && (spawned >= MAX_HEARTS || lastSpawn.passedSince() > 1.seconds)) {
            spawning = false
            println("spawning: false [${lastSpawn.passedSince()}]")
        }
        ++currentIndex

        lastHearts = SimpleTimeMark.now()
        val pos = event.location

        if (spawning) {
            ++spawned
            hearts += SplatterHeart(pos, spawned)
            println("added heart at $pos ($spawned) [${lastSpawn.passedSince()}]")
        } else {
            val heart = hearts.apply { if (lastSpawn.passedSince() > 2.seconds) filter { it.location.distance(pos) < 0.2 } }
                .minByOrNull { it.index }
                ?: run {
                    println("no heart found at $pos [${lastSpawn.passedSince()}]")
                    return
                }
            hearts -= heart
            hearts += SplatterHeart(pos, currentIndex)
            println("updated heart (${heart.location} -> $pos) (${heart.index} -> $currentIndex) [${lastSpawn.passedSince()}]")
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        hearts.forEach {
            val pos = it.location.add(-0.5, 0.3, -0.5)
            val aabb = pos.axisAlignedTo(pos.add(1, 1, 1))
            event.drawFilledBoundingBox_nea(aabb, LorenzColor.RED.addOpacity(100))
        }
    }

    private fun isEnabled() =
        RiftAPI.inRift() && RiftAPI.inStillgoreChateau() && RiftAPI.config.area.stillgoreChateau.highlightSplatterHearts

}
