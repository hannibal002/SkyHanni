package at.hannibal2.skyhanni.features.itemabilities.abilitycooldown

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.itemability.ItemAbilityCooldownNotificationConfig.NotificationSound
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@SkyHanniModule
object ItemAbilityCoolDownNotification {
    private val config get() = SkyHanniMod.feature.inventory.itemAbilities.abilityCooldownNotifications
    private val triggeredNotifications = mutableSetOf<String>()

    @HandleEvent(priority = HandleEvent.LOW)
    fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled()) return

        if (event.isMod(10)) {
            checkAbilityCooldownNotifications()
        }
    }

    @HandleEvent(priority = HandleEvent.LOW)
    fun onWorldChange() {
        triggeredNotifications.clear()
    }

    private fun checkAbilityCooldownNotifications() {
        val enabledAbilities = config.enabledAbilities
        val threshold = config.notificationThreshold.seconds

        for (ability in enabledAbilities) {
            val isOnCooldown = ability.isOnCooldown()
            val notificationId = ability.name

            if (!isOnCooldown) {
                triggeredNotifications.remove(notificationId)
                continue
            }

            if (notificationId in triggeredNotifications) {
                continue
            }

            val remainingTime = ability.getRemainingCooldown()
            if (remainingTime <= threshold) {
                triggeredNotifications.add(notificationId)
                val messageTemplate = if (remainingTime <= 100.milliseconds) {
                    config.readyMessage
                } else {
                    config.soonMessage
                }
                val message = messageTemplate
                    .replace("&", "§")
                    .replace("{ability}", ability.displayName)
                TitleManager.sendTitle(message, duration = config.titleDuration.toDouble().toDuration(DurationUnit.SECONDS))

                playNotificationSound(config.soundType)
            }
        }
    }

    private fun playNotificationSound(soundType: NotificationSound) {
        when (soundType) {
            NotificationSound.None -> {}
            NotificationSound.PLING -> SoundUtils.playPlingSound()
            NotificationSound.CLICK -> SoundUtils.playClickSound()
            NotificationSound.BEEP -> SoundUtils.playBeepSound()
            NotificationSound.ERROR -> SoundUtils.playErrorSound()
        }
    }

    private fun isEnabled(): Boolean = SkyBlockUtils.inSkyBlock && !config.enabledAbilities.isEmpty()
}
