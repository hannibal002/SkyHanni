package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.data.PurseAPI
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.PreProfileSwitchEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.TabListData
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class InformationGetter {
    fun getInformation(){
        // Gets some values from the tablist
        for (line in TabListData.getTabList()) {
            when {
                line.startsWith(" Gems: §r§a") -> gems = line.removePrefix(" Gems: §r§a")
                line.startsWith(" Bank: §r§6") -> bank = line.removePrefix(" Bank: §r§6")
                line.startsWith(" §r§fMithril Powder: §r§2") -> mithrilPowder =
                    line.removePrefix(" §r§fMithril Powder: §r§2")
                line.startsWith(" §r§fGemstone Powder: §r§d") -> gemstonePowder =
                    line.removePrefix(" §r§fGemstone Powder: §r§d")
            }
        }

        // Gets some values from the scoreboard
        for (line in ScoreboardData.sidebarLinesFormatted) {
            when {
                line.startsWith(" §7⏣ ") || line.startsWith(" §5ф ") -> location = line
                line.startsWith("Purse: §6") || line.startsWith("Piggy: §6") -> purse = line.removePrefix("Purse: §6").removePrefix("Piggy: §6")
                line.startsWith("Motes: §d") -> motes = line.removePrefix("Motes: §d")
                extractLobbyCode(line) is String -> lobbyCode =
                    extractLobbyCode(line)?.substring(1) ?: "<hidden>" //removes first char (number of color code)
                line.startsWith("Heat: ") -> heat = line.removePrefix("Heat: ")
                line.startsWith("Bits: §b") -> bits = line.removePrefix("Bits: §b")
                line.startsWith("Copper: §c") -> copper = line.removePrefix("Copper: §c")
            }
        }
    }

    @SubscribeEvent
    fun onProfileSwitch(event: ProfileJoinEvent) {
        // Reset Bits - We need this bc if another profile has 0 bits, it won't show the bits line
        bits = "0"
    }
}
