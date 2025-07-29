package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.DamageIndicatorDeathEvent
import at.hannibal2.skyhanni.events.SlayerQuestCompleteEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark

@SkyHanniModule
object SlayerSpawnandKillMessage {

    private val config get() = SlayerApi.config

    @HandleEvent
    fun onDamageIndicatorDeathEvent(event: DamageIndicatorDeathEvent) {
        val entityData = event.data
        if (entityData.bossType.isSlayer && config.TimeToKillMessage) {
            val killTimeMessage = when (config.CompactKillMessage) {
                false -> "It took ${entityData.timeToKill}§e to kill ${entityData.bossType.fullName}."
                true -> "${entityData.bossType.fullName}§e Took ${entityData.timeToKill}."
            }
            ChatUtils.chat(killTimeMessage)
        }
    }

    @HandleEvent
    fun onSlayerQuestCompleteEvent(event: SlayerQuestCompleteEvent) {
        if (SlayerApi.questStartTime != SimpleTimeMark.farPast() && config.fullQuestTime) {
            val completeQuestTime = "Quest Took ${(SimpleTimeMark.now() - SlayerApi.questStartTime).inWholeSeconds}s To Complete"
            ChatUtils.chat(completeQuestTime)
        }
    }
}
