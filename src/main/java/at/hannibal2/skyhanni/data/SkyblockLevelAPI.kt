package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.LorenzActionBarEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class SkyblockLevelAPI {
    companion object {
        var currentLevel: Int = 0
        var xpSource = ""
        var currentProgress = ""
    }

    @SubscribeEvent
    fun onActionBarUpdate (event: LorenzActionBarEvent){
        val info = extractInfo(event.message)

        if (info.isNotEmpty()) {
            currentLevel = info[0].toInt()
            xpSource = info[1]
            currentProgress = info[2]
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent){
        if (currentLevel == 0) return

        if (event.phase == TickEvent.Phase.END) {
            val player = LorenzUtils.getPlayerName()
            val tabData = TabListData.getTabList()
            val levelRegex = Regex("""\[(\d{1,3})] $player""")
            for (line in tabData) {
                if (line.contains(player)) {
                    val colorlessLine = line.removeColor()
                    currentLevel = levelRegex.find(colorlessLine)!!.groupValues[1].toInt()
                    break
                }
            }
        }
    }

    private fun extractInfo(inputString: String): List<String> {
        val regexPattern = """§b\+(\d+) SkyBlock XP §7\(([^§]+)§7\)§b \((\d+)/100\)"""
        val regex = Regex(regexPattern)

        val matchResult = regex.find(inputString)

        return if (matchResult != null) {
            val (xpValue, source, progress) = matchResult.destructured
            listOf(xpValue, source, progress)
        } else {
            emptyList()
        }
    }
}
