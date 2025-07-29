package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.DamageIndicatorDeathEvent
import at.hannibal2.skyhanni.events.SlayerQuestCompleteEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.inPartialSeconds

@SkyHanniModule
object SlayerSpawnAndKillMessage {

    private val config get() = SlayerApi.config

    @HandleEvent
    fun onDamageIndicatorDeathEvent(event: DamageIndicatorDeathEvent) {
        val (bossType, timeToKill) = with(event.data) { bossType to timeToKill }
        if (!config.timeToKillMessage || !bossType.isSlayer) return
        val killTimeMessage = if (config.compactKillMessage)
            "It took $timeToKill§e to kill ${bossType.fullName}."
        else "${bossType.shortName}§e took $timeToKill."

        ChatUtils.chat(killTimeMessage)
    }

    @HandleEvent
    fun onSlayerQuestCompleteEvent(event: SlayerQuestCompleteEvent) {
        val startTime = SlayerApi.questStartTime
        if (!config.fullQuestTime || startTime.isFarPast()) return

        val duration = startTime.passedSince().inPartialSeconds
        ChatUtils.chat("Slayer Quest took ${duration}s to complete.")
    }
}
