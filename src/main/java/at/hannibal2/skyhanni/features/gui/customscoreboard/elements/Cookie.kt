package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.BitsAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.utils.TimeUtils.format

object Cookie : Element() {
    override fun getDisplayPair() = listOf(
        "§dCookie Buff§f: " + (BitsAPI.cookieBuffTime?.let {
            if (!BitsAPI.hasCookieBuff()) "§cNot Active" else it.timeUntil().format(maxUnits = 2)
        } ?: "§cOpen SbMenu!"),
    )

    override fun showWhen() = BitsAPI.hasCookieBuff() && informationFilteringConfig.hideEmptyLines

    override val configLine = "§dCookie Buff§f: 3d 17h"
}
