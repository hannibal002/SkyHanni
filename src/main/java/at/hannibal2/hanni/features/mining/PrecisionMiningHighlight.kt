package at.hannibal2.hanni.features.mining

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandTypeTags
import at.hannibal2.hanni.events.ReceiveParticleEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.BlockUtils
import at.hannibal2.hanni.utils.ColorUtils.toChromaColor
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.hanni.utils.TimeUtils.ticks
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import net.minecraft.client.Minecraft
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.EnumParticleTypes
import java.awt.Color

@HanniModule
object PrecisionMiningHighlight {

    private val config get() = HanniMod.feature.mining.highlightPrecisionMiningParticles

    private var lastParticle: AxisAlignedBB? = null
    private var lookingAtParticle: Boolean = false
    private var deleteTime: SimpleTimeMark? = null

    @HandleEvent(onlyOnSkyblock = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        if (!(event.type == EnumParticleTypes.CRIT || event.type == EnumParticleTypes.VILLAGER_HAPPY) ||
            !Minecraft.getMinecraft().gameSettings.keyBindAttack.isKeyDown
        ) return

        val particleBoundingBox = event.location.add(-0.12, -0.12, -0.12)
            .axisAlignedTo(event.location.clone().add(0.12, 0.12, 0.12))

        val blockBoundingBox = BlockUtils.getTargetedBlock()?.let {
            it.axisAlignedTo(it.add(1.0, 1.0, 1.0))
        } ?: return
        if (!blockBoundingBox.intersectsWith(particleBoundingBox)) return

        lookingAtParticle = event.type == EnumParticleTypes.VILLAGER_HAPPY
        lastParticle = particleBoundingBox
        deleteTime = 5.ticks.fromNow()
    }

    @HandleEvent
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        val particleBoundingBox = lastParticle ?: return

        // TODO add chroma color support via config
        val color = if (lookingAtParticle) Color.GREEN else Color.CYAN
        event.drawFilledBoundingBox(particleBoundingBox, color.toChromaColor())
    }

    @HandleEvent
    fun onTick() {
        lastParticle ?: return
        val deletionTime = deleteTime ?: return
        if (deletionTime.isInPast()) {
            deleteTime = null
            lastParticle = null
        }
    }

    fun isEnabled() = IslandTypeTags.CUSTOM_MINING.inAny() && config
}
