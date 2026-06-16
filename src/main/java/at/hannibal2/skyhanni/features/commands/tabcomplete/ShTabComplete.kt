package at.hannibal2.skyhanni.features.commands.tabcomplete

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.utils.SkyBlockUtils

object ShTabComplete {

    private val config get() = SkyHanniMod.feature.misc.commands.tabComplete

    fun handleTabComplete(command: String): List<String>? {
        if (!isEnabled()) return null
        if (command != "sh" && command != "skyhanni") return null

        return CommandCategory.entries.map { it.name.lowercase() }
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.shCategories
}
