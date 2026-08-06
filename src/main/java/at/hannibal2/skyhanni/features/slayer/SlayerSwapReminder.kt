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

    private val config get() = SlayerApi.config.swapReminder

    private var hasRemindedForCurrentBoss = false

    @HandleEvent(onlyOnSkyblock = true)
    private fun onBossHealthChange(event: BossHealthChangeEvent) {
        if (!isActive()) return
        if (hasRemindedForCurrentBoss) return

        val mob = event.entityData.mob
        if (mob.category != MobCategory.SLAYER || !mob.belongsToPlayer()) return

        val currentHealth = event.health.toDouble()
        val maxHealth = event.maxHealth.toDouble()

        // Ignore uninitialized or dead mob health states
        if (maxHealth <= 0 || currentHealth <= 0) return

        val hpPercentage = (currentHealth / maxHealth) * 100.0
        if (hpPercentage > config.hpThreshold) return

        hasRemindedForCurrentBoss = true

        val formattedTitle = config.titleText.replace('&', '§')

        TitleManager.sendTitle(
            titleText = formattedTitle,
            duration = 2.seconds,
        )

        if (config.playSound) {
            SoundUtils.playPlingSound()
        }
    }

    @HandleEvent
    private fun onMobDeSpawn(event: MobEvent.DeSpawn.SkyblockMob) {
        if (event.mob.category != MobCategory.SLAYER || !event.mob.belongsToPlayer()) return

        hasRemindedForCurrentBoss = false
    }

    private fun isActive() = config.enabled && SlayerApi.isInBossFight()
}
