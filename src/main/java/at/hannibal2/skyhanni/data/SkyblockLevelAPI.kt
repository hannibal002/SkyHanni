package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.LorenzActionBarEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SkyblockLevelAPI {
    companion object {
        var currentLvl: Int = 0
        var xpSource = ""
        var currentProgress = 0.0
    }

    @SubscribeEvent
    fun onActionBarUpdate (event: LorenzActionBarEvent){
        val info = extractInfo(event.message)

        if (info.isNotEmpty()) {
            currentLvl = info[0].toInt()
            xpSource = info[1]
            currentProgress = info[2].toDouble()
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
