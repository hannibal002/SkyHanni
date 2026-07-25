package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.mob.Mob.Companion.belongsToPlayer
import at.hannibal2.skyhanni.data.title.TitleManager
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

        val currentPb = ProfileStorageData.playerSpecific?.slayerPersonalBests?.get(bossType)
        val isNewPersonalBest = data.timeToKill < (currentPb ?: Duration.INFINITE)

        if (isNewPersonalBest) {
            ProfileStorageData.playerSpecific?.slayerPersonalBests?.set(bossType, data.timeToKill)
        }

        val slayerTimeData = createSlayerTimeData(
            bossType = bossType,
            timeToKill = data.timeToKillString,
            currentPb = currentPb,
            isNewPersonalBest = isNewPersonalBest,
        )

        buildChatMessages(slayerTimeData).forEach(ChatUtils::chat)
    }

    private fun buildChatMessages(
        slayerTimeData: SlayerTimeData,
    ): List<String> = buildList {
        val compact = config.compact

        if (config.timeToKill) {
            add(
                (if (compact) templates.compactTimeToKill else templates.timeToKill)
                    .format(slayerTimeData),
            )
        }

        if (config.timeToKillPersonalBests) {
            val template = when {
                slayerTimeData.isNewPersonalBest && slayerTimeData.currentPbDisplay == null -> {
                    if (compact) templates.compactFirstPersonalBest else templates.firstPersonalBest
                }

                slayerTimeData.isNewPersonalBest -> {
                    if (compact) templates.compactNewPersonalBest else templates.newPersonalBest
                }

                else -> {
                    if (compact) templates.compactPersonalBest else templates.personalBest
                }
            }

            add(template.format(slayerTimeData))
        }
    }

    @HandleEvent
    fun onSlayerQuestComplete() {
        val startTime = SlayerApi.questStartTime
        if (!config.questComplete || startTime.isFarPast()) return

        val slayerTimeData = createSlayerTimeData(
            bossType = null,
            timeToKill = startTime.passedSince().format(),
            currentPb = null,
        )

        ChatUtils.chat(
            (if (config.compact) templates.compactQuestComplete else templates.questComplete)
                .format(slayerTimeData),
        )

        showTitle(slayerTimeData)
    }

    private fun showTitle(slayerTimeData: SlayerTimeData) {
        if (!config.titles) return
        if (!slayerTimeData.isNewPersonalBest) return
        TitleManager.sendTitle(
            titleText = templates.title.format(slayerTimeData),
            subtitleText = templates.subtitle
                .format(slayerTimeData)
                .takeIf { it.isNotBlank() },
        )
    }

    private fun createSlayerTimeData(
        bossType: BossType?,
        timeToKill: String,
        currentPb: Duration?,
        isNewPersonalBest: Boolean = false,
    ): SlayerTimeData = SlayerTimeData(
        bossDisplayName = if (bossType == null) {
            ""
        } else if (config.compact) {
            bossType.shortName
        } else {
            bossType.fullName
        },
        timeToKill = timeToKill,
        currentPbDisplay = currentPb?.format(showMilliSeconds = true),
        isNewPersonalBest = isNewPersonalBest,
    )

    private fun String.format(data: SlayerTimeData): String = this
        .replace("&", "§")
        .replace("{boss}", data.bossDisplayName)
        .replace("{time}", data.timeToKill)
        .replace("{previous}", data.currentPbDisplay.orEmpty())

    private data class SlayerTimeData(
        val bossDisplayName: String,
        val timeToKill: String,
        val currentPbDisplay: String?,
        val isNewPersonalBest: Boolean,
    )

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(132, "slayer.timeToKillMessage", "slayer.slayerTimeMessages.timeToKill")
        event.move(132, "slayer.questCompleteMessage", "slayer.slayerTimeMessages.questComplete")
        event.move(132, "slayer.compactTimeMessage", "slayer.slayerTimeMessages.compact")
    }
}
