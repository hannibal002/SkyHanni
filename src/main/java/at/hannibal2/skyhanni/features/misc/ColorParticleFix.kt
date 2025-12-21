package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SkyBlockUtils

@SkyHanniModule
object ColorParticleFix {
    private val config get() = SkyHanniMod.feature.misc

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.fixColorParticles
}
