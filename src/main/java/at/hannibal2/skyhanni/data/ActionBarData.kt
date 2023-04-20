package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.LorenzActionBarEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class ActionBarData {
    private val regex = "..((?:\\d|,)*)\\/(?:\\d|,)*(.) *..((?:\\d|,)*)..(. \\w*) *..((?:\\d|,)*)\\/(?:\\d|,)*(.*)"
    private val pattern = Pattern.compile(regex)
// Sample input: §c2,817/2,817❤     §a703§a❈ Defense     §b3,479/3,479✎ Mana
// Returns the following groups: 1 = 2,817; 2 = ❤; 3 = 703; 4 = ❈ Defense; 5 = 3,479; 6 = ✎ Mana

    companion object {
        var groups = arrayListOf<Any>()
    }

    @SubscribeEvent
    fun onActionBar(event: LorenzActionBarEvent) {
        groups = arrayListOf()
        if (!LorenzUtils.inSkyBlock) return

        val matcher = pattern.matcher(event.message)
        if (!matcher.matches()) return

        for (i in 1..matcher.groupCount()) {
            groups.add(matcher.group(i))
        }
    }
}
