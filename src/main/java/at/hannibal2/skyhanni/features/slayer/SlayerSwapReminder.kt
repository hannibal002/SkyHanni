package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.mob.Mob.Companion.belongsToPlayer
import at.hannibal2.skyhanni.data.mob.MobCategory
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.BossHealthChangeEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SoundUtils
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SlayerSwapReminder {

    private val config get() = SlayerApi.config.slayerSwapReminder

    private var hasAlertedForCurrentBoss = false

    @HandleEvent(onlyOnSkyblock = true)
    private fun onBossHealthChange(event: BossHealthChangeEvent) {
        if (!isActive()) return
        if (hasAlertedForCurrentBoss) return

        val mob = event.entityData.mob
        if (mob.category != MobCategory.SLAYER || !mob.belongsToPlayer()) return

        val currentHealth = event.health.toDouble()
        val maxHealth = event.maxHealth.toDouble()

        // Ignore uninitialized / zero health states when boss first spawns
        if (maxHealth <= 0 || currentHealth <= 0) return

        val hpPercentage = (currentHealth / maxHealth) * 100.0
        if (hpPercentage > config.hpThreshold) return

        hasAlertedForCurrentBoss = true

        val formattedTitle = config.titleText.replace('&', '§')

        TitleManager.sendTitle(
            titleText = formattedTitle,
            duration = 2.seconds,
            addType = TitleManager.TitleAddType.FORCE_FIRST
        )

        if (config.playSound) {
            SoundUtils.playPlingSound()
        }
    }

    @HandleEvent
    private fun onMobDeSpawn(event: MobEvent.DeSpawn.SkyblockMob) {
        val mob = event.mob
        if (mob.category != MobCategory.SLAYER || !mob.belongsToPlayer()) return

        hasAlertedForCurrentBoss = false
    }

    private fun isActive() = config.enabled && SlayerApi.isInBossFight()
}
