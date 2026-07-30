package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.mob.Mob.Companion.belongsToPlayer
import at.hannibal2.skyhanni.data.mob.MobCategory
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SoundUtils
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SlayerSwapReminder{

    private val config get() = SlayerApi.config.rodSwapAlert

    private var hasAlertedForCurrentBoss = false

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick(event: SkyHanniTickEvent) {
        if (!isActive()) return
        if (hasAlertedForCurrentBoss) return

        val myBoss = getMySlayerBoss() ?: return

        val health = myBoss.health.toDouble()
        val maxHealth = myBoss.maxHealth.toDouble()

        if (maxHealth <= 0) return

        val hpPercentage = (health / maxHealth) * 100.0
        val threshold = config.hpThreshold

        if (hpPercentage <= threshold) {
            hasAlertedForCurrentBoss = true

            // Convert custom user '&' color codes to '§'
            val formattedTitle = config.titleText.replace('&', '§')

            // Send title with FORCE_FIRST so it renders instantly over any waiting titles
            TitleManager.sendTitle(
                titleText = formattedTitle,
                duration = 2.seconds,
                addType = TitleManager.TitleAddType.FORCE_FIRST
            )

            if (config.playSound) {
                SoundUtils.playPlingSound()
            }
        }
    }

    @HandleEvent
    fun onMobDeSpawn(event: MobEvent.DeSpawn.SkyblockMob) {
        val mob = event.mob
        if (mob.category == MobCategory.SLAYER && mob.belongsToPlayer()) {
            hasAlertedForCurrentBoss = false

            // Uses TitleManager's conditionallyStopTitle to clear the active rod swap alert on boss despawn
            val targetTitle = config.titleText.replace('&', '§')
            TitleManager.conditionallyStopTitle { activeText ->
                activeText == targetTitle
            }
        }
    }

    private fun getMySlayerBoss(): Mob? = MobData.skyblockMobs
        .firstOrNull { mob ->
            mob.category == MobCategory.SLAYER && mob.belongsToPlayer()
        }

    private fun isActive() = config.enabled && SlayerApi.isInBossFight()
}
