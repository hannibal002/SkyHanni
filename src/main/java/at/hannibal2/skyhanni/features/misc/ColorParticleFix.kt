package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ParticleChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import net.minecraft.core.particles.ColorParticleOption
import net.minecraft.util.ARGB

@SkyHanniModule
object ColorParticleFix {
    private val config get() = SkyHanniMod.feature.misc

    @HandleEvent
    fun onParticleChange(event: ParticleChangeEvent) {
        val particleOptions = event.particleOptions
        if (!isEnabled()) return
        if (particleOptions is ColorParticleOption) {
            particleOptions.color = ARGB.colorFromFloat(
                particleOptions.getAlpha(),
                (1 - event.packet.getXDist()),
                (1 - event.packet.getZDist()),
                (1 - event.packet.getYDist())
            )
        }
        event.particleOptions = particleOptions
    }
    /*
    1.8 has, some, questionable, ways of working out colour for particles where it uses the offset to calculate the Colouring,
    this is done as RBG with XYZ, so in modern since it uses ARGB we go to XZY.
     */

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.fixColorParticles
}
