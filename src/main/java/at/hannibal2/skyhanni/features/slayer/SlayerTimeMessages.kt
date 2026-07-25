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
    private val templates get() = config.templates

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
        if (isNewPersonalBest) {
            ProfileStorageData.playerSpecific?.slayerPersonalBests?.set(bossType, data.timeToKill)
        }

        val messages = buildTimeMessages(
            isNewPersonalBest,
            currentPb,
            compact,
            bossDisplayName,
            data.timeToKillString,
        )

        messages.forEach(ChatUtils::chat)
    }

    private fun buildTimeMessages(
        isNewPersonalBest: Boolean,
        currentPb: Duration?,
        compact: Boolean,
        bossDisplayName: String,
        timeToKill: String,
    ): List<String> = buildList {
        if (config.timeToKill) {
            add(
                formatTemplate(
                    if (compact) templates.compactTimeToKill else templates.timeToKill,
                    bossDisplayName,
                    timeToKill,
                    null,
                ),
            )
        }

        if (config.timeToKillPersonalBests) {
            val currentPbDisplay = currentPb?.format(showMilliSeconds = true)

            val template = if (isNewPersonalBest) {
                if (currentPbDisplay == null) {
                    if (compact) {
                        templates.compactFirstPersonalBest
                    } else {
                        templates.firstPersonalBest
                    }
                } else {
                    if (compact) {
                        templates.compactNewPersonalBest
                    } else {
                        templates.newPersonalBest
                    }
                }
            } else {
                if (compact) {
                    templates.compactPersonalBest
                } else {
                    templates.personalBest
                }
            }

            add(
                formatTemplate(
                    template,
                    bossDisplayName,
                    timeToKill,
                    currentPbDisplay,
                ),
            )
        }
    }

    @HandleEvent
    fun onSlayerQuestComplete() {
        val startTime = SlayerApi.questStartTime
        if (!config.questComplete || startTime.isFarPast()) return

        val duration = startTime.passedSince().format()

        ChatUtils.chat(
            formatTemplate(
                if (config.compact) templates.compactQuestComplete else templates.questComplete,
                boss = "",
                time = duration,
                previous = null,
            ),
        )
    }

    private fun formatTemplate(
        template: String,
        boss: String,
        time: String,
        previous: String?,
    ): String = template
        .replace("&", "§")
        .replace("{boss}", boss)
        .replace("{time}", time)
        .replace("{previous}", previous.orEmpty())

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(132, "slayer.timeToKillMessage", "slayer.slayerTimeMessages.timeToKill")
        event.move(132, "slayer.questCompleteMessage", "slayer.slayerTimeMessages.questComplete")
        event.move(132, "slayer.compactTimeMessage", "slayer.slayerTimeMessages.compact")
    }
}
