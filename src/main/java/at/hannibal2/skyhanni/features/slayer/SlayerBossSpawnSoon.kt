package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.data.SlayerAPI
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/*
Feature from SBA
*/
class SlayerBossSpawnSoon {

    private val config get() = SkyHanniMod.feature.slayer.slayerBossWarning
    private val pattern = "\\D+(?<progress>[0-9.k]+)/(?<total>[0-9.k]+) (?:Kills|Combat XP).*".toPattern()
    private var lastCompletion: Float = 0.0f
    private var warned = false

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (event.isMod(5)) {
            for (line in ScoreboardData.sidebarLinesFormatted) {
                pattern.matchMatcher(line.removeColor()) {
                    val progressMatcher = Regex("[^.0-9\\-]").toPattern().matcher(group("progress"))
                    val totalMatcher = Regex("[^.0-9\\-]").toPattern().matcher(group("total"))
                    val progress = progressMatcher.replaceAll("").toFloat() * if (group("progress").contains("k")) 1000 else 1
                    val total = totalMatcher.replaceAll("").toFloat() * if (group("total").contains("k")) 1000 else 1
                    val completion = progress / total
                    if (completion > (config.percent.toFloat() / 100)) {
                        if (!warned || (config.repeat && completion != lastCompletion)) {
                            SoundUtils.playBeepSound()
                            TitleUtils.sendTitle("§cSlayer boss soon!", 2_000)
                            warned = true
                        }
                    } else {
                        warned = false
                    }
                    lastCompletion = completion
                }
            }
        }
    }

    fun isEnabled() = config.enabled && SlayerAPI.hasActiveSlayerQuest()
}