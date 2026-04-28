package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.mob.Mob.Companion.belongsToPlayer
import at.hannibal2.skyhanni.events.DamageIndicatorDeathEvent
import at.hannibal2.skyhanni.features.combat.damageindicator.BossType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import kotlin.time.Duration

@SkyHanniModule
object SlayerTimeMessages {

    private val config get() = SkyHanniMod.feature.slayer.slayerTimeMessages

    @HandleEvent
    fun onDamageIndicatorDeathEvent(event: DamageIndicatorDeathEvent) {
        val data = event.data
        val bossType = data.bossType
        if (!bossType.isSlayer || !data.entity.belongsToPlayer()) return
        if (bossType == BossType.SLAYER_SPIDER_5_1) return

        val compact = config.compact

        val bossDisplayName = if (compact) bossType.shortName else bossType.fullName

        val currentPb = ProfileStorageData.playerSpecific?.slayerPersonalBests?.get(bossType)

        val isNewPersonalBest = data.timeToKill < (currentPb ?: Duration.INFINITE)
        if (isNewPersonalBest) ProfileStorageData.playerSpecific?.slayerPersonalBests?.set(bossType, data.timeToKill)

        val messages = buildTimeMessages(isNewPersonalBest, currentPb, compact, bossDisplayName, data.timeToKillString)

        messages.forEach { ChatUtils.chat(it) }
    }

    private fun buildTimeMessages(
        isNewPersonalBest: Boolean,
        currentPb: Duration?,
        compact: Boolean,
        bossDisplayName: String,
        timeToKill: String,
    ): List<String> = buildList {
        if (config.timeToKill) add(
            if (compact) "$bossDisplayName §etook §b$timeToKill"
            else "It took §b$timeToKill§e to kill $bossDisplayName",
        )

        if (config.timeToKillPersonalBests) {
            val currentPbDisplay = currentPb?.format(showMilliSeconds = true)

            add(
                if (isNewPersonalBest) {
                    when (currentPbDisplay) {
                        null -> {
                            if (compact) "§e§lNEW PB! $bossDisplayName §ein §a$timeToKill"
                            else "§e§lNEW PERSONAL BEST! §a$timeToKill §efor $bossDisplayName"
                        }
                        else -> {
                            if (compact) "§e§lNEW PB! $bossDisplayName §ein §c$currentPbDisplay §e-> §a$timeToKill"
                            else "§e§lNEW PERSONAL BEST! §a$timeToKill §7(Previous $currentPbDisplay) §efor $bossDisplayName"
                        }
                    }
                } else {
                    if (compact) "$bossDisplayName §ePB: §6$currentPbDisplay"
                    else "$bossDisplayName §ePersonal best: §6$currentPbDisplay"
                },
            )
        }
    }

    @HandleEvent
    fun onSlayerQuestComplete() {
        val startTime = SlayerApi.questStartTime
        if (!config.questComplete || startTime.isFarPast()) return

        val duration = startTime.passedSince().format()

        ChatUtils.chat(
            if (config.compact)
                "Quest took §b$duration§e in total."
            else
                "Slayer quest took §b$duration§e to complete.",
        )
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(132, "slayer.timeToKillMessage", "slayer.slayerTimeMessages.timeToKill")
        event.move(132, "slayer.questCompleteMessage", "slayer.slayerTimeMessages.questComplete")
        event.move(132, "slayer.compactTimeMessage", "slayer.slayerTimeMessages.compact")
    }
}
