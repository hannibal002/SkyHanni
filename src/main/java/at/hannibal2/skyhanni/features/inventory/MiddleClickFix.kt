package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SkyBlockUtils

@SkyHanniModule
object MiddleClickFix {
    private val config get() = SkyHanniMod.feature.inventory

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.middleClickFix
}
