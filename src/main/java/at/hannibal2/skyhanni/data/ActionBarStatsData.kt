package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.LorenzActionBarEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ActionBarStatsData {
    // TODO USE SH-REPO
    private val patterns = mapOf(
        "health" to "§[c6](?<health>[\\d,]+)/[\\d,]+❤.*".toPattern(),
        "defense" to ".*§a(?<defense>[\\d,]+)§a❈.*".toPattern(),
        "mana" to ".*§b(?<mana>[\\d,]+)/[\\d,]+✎.*".toPattern(),
        "riftTime" to "§[a7](?<riftTime>[\\dms ]+)ф.*".toPattern(),
    )

    var groups = mutableMapOf("health" to "", "riftTime" to "", "defense" to "", "mana" to "")
    var actionBar = ""

    @SubscribeEvent
    fun onActionBar(event: LorenzActionBarEvent) {
        if (!LorenzUtils.inSkyBlock) return

        actionBar = event.message

        for ((groupName, pattern) in patterns) {
            pattern.matchMatcher(event.message) {
                groups[groupName] = group(groupName)
            }
        }
    }
}
