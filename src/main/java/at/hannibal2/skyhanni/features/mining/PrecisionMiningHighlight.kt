package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.MiningAPI
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox_nea
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.TimeUtils.ticks
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

@SkyHanniModule
object PrecisionMiningHighlight {

    var lastParticle: AxisAlignedBB? = null
    var lookingAt: Boolean = false
    var deleteTime: SimpleTimeMark? = null

    @SubscribeEvent
    fun onParticle(event: ReceiveParticleEvent) {
        if (!MiningAPI.inCustomMiningIsland() || !isEnabled()) return
        if (!(event.type == EnumParticleTypes.CRIT || event.type == EnumParticleTypes.VILLAGER_HAPPY)) return
        
        lookingAt = event.type == EnumParticleTypes.VILLAGER_HAPPY
        lastParticle = event.location.add(x = -0.1, y = -0.1, z = -0.1).axisAlignedTo(event.location.clone().add(0.1, 0.1, 0.1))
        deleteTime = 5.ticks.fromNow()
    }

    @SubscribeEvent
    fun onRender(event: LorenzRenderWorldEvent) {
        val p = lastParticle ?: return

        event.drawFilledBoundingBox_nea(p, if (lookingAt) Color.GREEN else Color.CYAN)
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        lastParticle ?: return
        val d = deleteTime ?: return
        if (d.isInPast()) {
            deleteTime = null
            lastParticle = null
        }
    }

    fun isEnabled() = SkyHanniMod.feature.mining.highlightPrecisionMiningParticles
}
