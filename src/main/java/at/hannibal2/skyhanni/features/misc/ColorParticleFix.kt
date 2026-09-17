package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ParticleChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.core.particles.ColorParticleOption
import net.minecraft.util.ARGB

/**
 * Minecraft 1.8.9 stores RGB values for particles with customizable colors in the
 * `xSpeedIn`/`ySpeedIn`/`zSpeedIn` fields (`xDist`/`yDist`/`zDist` on modern).
 * Modern has dedicated color fields, but Hypixel does not use them, leading to
 * certain particles rendering with incorrect colors. We fix them here.
 *
 * Note: The method is named `setRBGColorF` in MCP, but this is a misspelling.
 * It is clear from the source code that the colors are stored as XYZ=RGB.
 *
 * Note: Each channel is read inverted (`1 - offset`).
 */
@SkyHanniModule
object ColorParticleFix {
    private val config get() = SkyHanniMod.feature.misc

    @HandleEvent(onlyOnSkyblock = true)
    private fun onParticleChange(event: ParticleChangeEvent) {
        if (!config.fixColorParticles) return
        val particleOptions = event.particleOptions
        if (particleOptions is ColorParticleOption) {
            particleOptions.color = ARGB.colorFromFloat(
                particleOptions.alpha,
                (1 - event.packet.xDist),
                (1 - event.packet.yDist),
                (1 - event.packet.zDist),
            )
        }
        event.particleOptions = particleOptions
    }
}
