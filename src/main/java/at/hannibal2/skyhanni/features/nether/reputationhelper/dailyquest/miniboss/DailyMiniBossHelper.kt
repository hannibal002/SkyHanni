package at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.miniboss

import at.hannibal2.skyhanni.data.HyPixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.nether.reputationhelper.CrimsonIsleReputationHelper
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class DailyMiniBossHelper(private val reputationHelper: CrimsonIsleReputationHelper) {

    private val miniBossesDoneToday = mutableMapOf<CrimsonMiniBoss, Boolean>()
    private val miniBossesPatterns = mutableMapOf<CrimsonMiniBoss, Pattern>()

    fun init() {
        for (miniBoss in reputationHelper.miniBosses) {
            miniBossesDoneToday[miniBoss] = false
            val patterns = " *§r§6§l${miniBoss.displayName.uppercase()} DOWN!"
            println("patterns: '$patterns'")
            miniBossesPatterns[miniBoss] = Pattern.compile(patterns)
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!HyPixelData.skyBlock) return
        if (LorenzUtils.skyBlockIsland != IslandType.CRIMSON_ISLE) return

        val message = event.message
        for (entry in miniBossesPatterns) {
            val pattern = entry.value
            if (pattern.matcher(message).matches()) {
                finished(entry.key)
            }
            //§r§6§BLADESOUL DOWN!
        }
        //                           §r§6§lMAGMA BOSS DOWN!
        //                           §r§6§lMAGE OUTLAW DOWN!
    }

    private fun finished(miniBoss: CrimsonMiniBoss) {
        LorenzUtils.debug("Detected mini boss death: ${miniBoss.displayName}")
        reputationHelper.questHelper.finishMiniBoss(miniBoss)
        miniBossesDoneToday[miniBoss] = true
        reputationHelper.update()
    }

    fun render(display: MutableList<String>) {
        val done = miniBossesDoneToday.count { it.value }
//        val sneaking = Minecraft.getMinecraft().thePlayer.isSneaking
//        if (done != 5 || sneaking) {
        if (done != 5) {
            display.add("")
            display.add("Daily Bosses ($done/5 killed)")
            for (entry in miniBossesDoneToday) {
                display.add(renderQuest(entry.key, entry.value))
            }
        }
    }

    private fun renderQuest(miniBoss: CrimsonMiniBoss, doneToday: Boolean): String {
        val color = if (doneToday) "§7Done" else "§bTodo"
        val displayName = miniBoss.displayName
        return "$displayName: $color"
    }
}