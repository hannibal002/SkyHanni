package at.hannibal2.skyhanni.features.itemabilities.abilitycooldown

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.itemability.ItemAbilityCooldownNotificationConfig.NotificationSound
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@SkyHanniModule
object ItemAbilityCoolDownNotification {
    private val config get() = SkyHanniMod.feature.inventory.itemAbilities.abilityCooldownNotifications
    private val triggeredNotifications = mutableSetOf<String>()
    private var currentNotificationAbility: ItemAbility? = null
    private var notificationStartTime: SimpleTimeMark = SimpleTimeMark.farPast()

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
        currentNotificationAbility = null
        notificationStartTime = SimpleTimeMark.farPast()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        val ability = currentNotificationAbility ?: return

        // Clear notification if duration has elapsed
        if (notificationStartTime.passedSince() > config.titleDuration.toDouble().toDuration(DurationUnit.SECONDS)) {
            currentNotificationAbility = null
            return
        }

        val message = buildNotificationMessage(ability) ?: return
        val alertText = Renderable.text(message)

        config.position.renderRenderable(alertText, posLabel = "Ability Cooldown Notification")
    }

    private fun buildNotificationMessage(ability: ItemAbility): String? {
        val remainingTime = ability.getRemainingCooldown()
        if (remainingTime < 0.milliseconds) return null

        val messageTemplate = if (remainingTime <= 100.milliseconds) {
            config.readyMessage
        } else {
            config.soonMessage
        }

        return messageTemplate
            .replace("{ability}", ability.displayName)
            .replace("{time}", ability.getDurationText())
            .replace("&", "§")
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

            if (ability.getRemainingCooldown() <= threshold) {
                triggeredNotifications.add(notificationId)
                currentNotificationAbility = ability
                notificationStartTime = SimpleTimeMark.now()

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

    private fun isEnabled(): Boolean = SkyBlockUtils.inSkyBlock && config.enabled && config.enabledAbilities.isNotEmpty()
}
