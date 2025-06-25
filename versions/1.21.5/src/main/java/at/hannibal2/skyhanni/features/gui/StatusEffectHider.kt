package at.hannibal2.skyhanni.features.gui

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.SkyBlockUtils

object StatusEffectHider {

    private val config get() = SkyHanniMod.feature.gui

    @JvmStatic
    fun shouldHide(): Boolean = SkyBlockUtils.inSkyBlock && config.hideStatusEffects
}
