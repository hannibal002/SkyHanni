package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.DamageIndicatorDeathEvent
import at.hannibal2.skyhanni.events.SlayerQuestCompleteEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format

@SkyHanniModule
object SlayerTimeMessages {

    private val config get() = SlayerApi.config

    @HandleEvent
    fun onDamageIndicatorDeathEvent(event: DamageIndicatorDeathEvent) {
        val (bossType, timeToKill) = with(event.data) { bossType to timeToKill }
        if (!config.timeToKillMessage || !bossType.isSlayer) return

        ChatUtils.chat(
            if (config.compactTimeMessage)
                "It took §b$timeToKill§e to kill ${bossType.fullName}."
            else
                "${bossType.shortName}§e took §b$timeToKill§e.",
        )
    }

    @HandleEvent
    fun onSlayerQuestCompleteEvent(event: SlayerQuestCompleteEvent) {
        val startTime = SlayerApi.questStartTime
        if (!config.fullQuestTime || startTime.isFarPast()) return

        val duration = startTime.passedSince().format()

        ChatUtils.chat(
            if (config.compactTimeMessage)
                "Slayer took §b$duration§e in total."
            else
                "Slayer quest took §b$duration§es to complete.",
        )
    }
}
