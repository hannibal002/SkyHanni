package at.hannibal2.skyhanni.features.commands.tabcomplete

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.utils.SkyBlockUtils

object CropTimeTabComplete {

    private val config get() = SkyHanniMod.feature.misc.commands.tabComplete

    fun handleTabComplete(command: String ): List<String>? {
        if(!isEnabled()) return null
        if (command != "shcroptime") return null

        return CropType.entries.map { it.name.lowercase() }
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.shCropTime
}
