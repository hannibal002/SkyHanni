package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.CollectionUtils.editCopy
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine_nea
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.time.Duration.Companion.seconds

class PestParticleLine {

    private var lastPestTrackerUse = SimpleTimeMark.farPast()
    private var locations = mapOf<LorenzVec, Color>()

    @SubscribeEvent
    fun onItemClick(event: ItemClickEvent) {
        if (!isEnabled()) return
        if (PestAPI.hasVacuumInHand()) {
            if (event.clickType == ClickType.LEFT_CLICK) {
                lastPestTrackerUse = SimpleTimeMark.now()
                locations = emptyMap()
            }
        }
    }

    @SubscribeEvent
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        val passedSince = lastPestTrackerUse.passedSince()
        if (passedSince > 5.seconds) return

        if (event.type == EnumParticleTypes.REDSTONE) {
            val (a, b, c) = event.offset.toDoubleArray().map { it.toFloat() }
            val color = Color(a, b, c)

            if (locations.isNotEmpty()) {
                val diff = event.location.distance(locations.toList().last().first)
                println("diff: ${diff.round(2)}")
            }

            locations = locations.editCopy {
                put(event.location, color)
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        if (lastPestTrackerUse.passedSince() > 20.seconds) return

        for ((prev, next) in locations.asSequence().zipWithNext()) {
            event.draw3DLine_nea(
                prev.key,
                next.key,
                next.value,
                3,
                false
            )
        }
    }

    // TODO toggle
    fun isEnabled() = GardenAPI.inGarden()
}
