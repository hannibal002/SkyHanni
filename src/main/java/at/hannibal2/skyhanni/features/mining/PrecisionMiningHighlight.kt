package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandTypeTags
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.TimeUtils.ticks
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import net.minecraft.client.Minecraft
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.EnumParticleTypes
import java.awt.Color

@SkyHanniModule
object PrecisionMiningHighlight {

    private val config get() = SkyHanniMod.feature.mining.highlightPrecisionMiningParticles

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
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
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
