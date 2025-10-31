package at.hannibal2.hanni.features.slayer

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.SlayerApi
import at.hannibal2.hanni.data.mob.Mob.Companion.belongsToPlayer
import at.hannibal2.hanni.events.DamageIndicatorDeathEvent
import at.hannibal2.hanni.events.SlayerQuestCompleteEvent
import at.hannibal2.hanni.features.combat.damageindicator.BossType
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.TimeUtils.format

@HanniModule
object SlayerTimeMessages {

    private val config get() = SlayerApi.config

    @HandleEvent
    fun onDamageIndicatorDeathEvent(event: DamageIndicatorDeathEvent) {
        val (bossType, timeToKill) = with(event.data) { bossType to timeToKill }
        if (!config.timeToKillMessage || !bossType.isSlayer || !event.data.entity.belongsToPlayer()) return

        // TODO fix tara 5 part 2 times by adding part 1 times to it
        if (event.data.bossType == BossType.SLAYER_SPIDER_5_1) return
        ChatUtils.chat(
            if (config.compactTimeMessage)
                "${bossType.shortName}§e took §b$timeToKill§e."
            else
                "It took §b$timeToKill§e to kill ${bossType.fullName}.",
        )
    }

    @HandleEvent
    fun onSlayerQuestCompleteEvent(event: SlayerQuestCompleteEvent) {
        val startTime = SlayerApi.questStartTime
        if (!config.questCompleteMessage || startTime.isFarPast()) return

        val duration = startTime.passedSince().format()

        ChatUtils.chat(
            if (config.compactTimeMessage)
                "Quest took §b$duration§e in total."
            else
                "Slayer quest took §b$duration§e to complete.",
        )
    }
}
