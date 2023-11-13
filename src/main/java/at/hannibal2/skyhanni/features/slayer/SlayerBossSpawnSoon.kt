package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.SlayerAPI
import at.hannibal2.skyhanni.events.SlayerProgressChangeEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class SlayerBossSpawnSoon {

    private val config get() = SkyHanniMod.feature.slayer.slayerBossWarning
    private val pattern = " \\(?(?<progress>[0-9.,k]+)/(?<total>[0-9.,k]+)\\)?.*".toPattern()
    private var lastCompletion = 0f
    private var warned = false

    @SubscribeEvent
    fun onSlayerProgressChange(event: SlayerProgressChangeEvent) {
        if (!isEnabled()) return
        if (!SlayerAPI.isInCorrectArea) return

        val completion = pattern.matchMatcher(event.newProgress.removeColor()) {
            group("progress").formatNumber().toFloat() / group("total").formatNumber().toFloat()
        } ?: return

        if (completion > config.percent / 100.0) {
            if (!warned || (config.repeat && completion != lastCompletion)) {
                SoundUtils.playBeepSound()
                LorenzUtils.sendTitle("§eSlayer boss soon!", 2.seconds)
                warned = true
            }
        } else {
            warned = false
        }
        lastCompletion = completion
    }

    fun isEnabled() = config.enabled && SlayerAPI.hasActiveSlayerQuest()
}
