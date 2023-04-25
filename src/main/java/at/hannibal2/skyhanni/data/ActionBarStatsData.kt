package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.LorenzActionBarEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class ActionBarStatsData {
    private val pattern =
        Pattern.compile("..((?:\\d|,)*)\\/(?:\\d|,)*(.) *..((?:\\d|,)*)..(. \\w*) *..((?:\\d|,)*)\\/(?:\\d|,)*(.*)")
// Sample input: §c2,817/2,817❤     §a703§a❈ Defense     §b3,479/3,479✎ Mana
// Returns the following groups: 1 = 2,817; 2 = ❤; 3 = 703; 4 = ❈ Defense; 5 = 3,479; 6 = ✎ Mana

    companion object {
        var groups = listOf<String>()
    }

    @SubscribeEvent
    fun onActionBar(event: LorenzActionBarEvent) {
        groups = readGroups(event.message)
    }

    private fun readGroups(message: String): List<String> {
        if (!LorenzUtils.inSkyBlock) return emptyList()

        val matcher = pattern.matcher(message)
        if (!matcher.matches()) return emptyList()

        val list = mutableListOf<String>()
        for (i in 1..matcher.groupCount()) {
            list.add(matcher.group(i))
        }
        return list
    }
}
