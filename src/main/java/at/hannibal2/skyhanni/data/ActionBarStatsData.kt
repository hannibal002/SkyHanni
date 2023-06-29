package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.LorenzActionBarEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class ActionBarStatsData {
    private val healthPattern = Pattern.compile("§c(?<health>[\\d,]+)/[\\d,]+❤.*")
    private val defensePattern = Pattern.compile(".*§a(?<defense>[\\d,]+)§a❈.*")
    private val manaPattern = Pattern.compile(".*§b(?<mana>[\\d,]+)/[\\d,]+✎.*")
    private val riftTimePattern = Pattern.compile("§[a7](?<riftTime>[\\dms ]+)ф.*")

    companion object {
        var groups = mutableMapOf("health" to "", "riftTime" to "", "defense" to "", "mana" to "")
    }

    @SubscribeEvent
    fun onActionBar(event: LorenzActionBarEvent) {
        readGroups(event.message)
    }

    private fun readGroups(message: String) {
        if (!LorenzUtils.inSkyBlock) return

        val healthMatcher = healthPattern.matcher(message)
        val defenseMatcher = defensePattern.matcher(message)
        val manaMatcher = manaPattern.matcher(message)
        val riftTimeMatcher = riftTimePattern.matcher(message)

        if (healthMatcher.matches()) {
            groups["health"] = healthMatcher.group("health")
        }
        if (defenseMatcher.matches()) {
            groups["defense"] = defenseMatcher.group("defense")
        }
        if (manaMatcher.matches()) {
            groups["mana"] = manaMatcher.group("mana")
        }
        if (riftTimeMatcher.matches()) {
            groups["riftTime"] = riftTimeMatcher.group("riftTime")
        } else {
            groups["riftTime"] = ""
        }
    }
}
