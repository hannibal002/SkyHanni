package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox_nea
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

@SkyHanniModule
object PrecisionMiningHighlight {

    var lastParticle: LorenzVec? = null
    var lookingAt: Boolean = false

    @SubscribeEvent
    fun onParticle(event: ReceiveParticleEvent) {
        if (!(IslandType.CRYSTAL_HOLLOWS.isInIsland() || IslandType.DWARVEN_MINES.isInIsland() || IslandType.MINESHAFT.isInIsland())) return

        if (event.type == EnumParticleTypes.VILLAGER_HAPPY) {
            lookingAt = true
            return
        }
        if (event.type != EnumParticleTypes.CRIT) return
        lookingAt = false
        lastParticle = event.location
    }

    @SubscribeEvent
    fun onRender(event: LorenzRenderWorldEvent) {
        val p = lastParticle ?: return
        val axisAlignedBB = p.add(x = -0.1, y = -0.1, z = -0.1).axisAlignedTo(p.clone().add(0.1, 0.1, 0.1))

        event.drawFilledBoundingBox_nea(axisAlignedBB, if (lookingAt) Color.GREEN else Color.CYAN)
    }
}
