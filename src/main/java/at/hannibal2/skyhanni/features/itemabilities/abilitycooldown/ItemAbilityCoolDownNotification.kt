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

    private data class AbilityNotificationState(
        val wasOnCooldown: Boolean,
        val lastNotifiedActivation: SimpleTimeMark,
    )

    private val abilityStates = mutableMapOf<ItemAbility, AbilityNotificationState>()
    private var currentNotificationAbility: ItemAbility? = null
    private var notificationStartTime: SimpleTimeMark = SimpleTimeMark.farPast()
    private var cachedMessageTemplate: String = ""

    @HandleEvent(priority = HandleEvent.LOW)
    fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled()) return

        if (event.isMod(10)) {
            checkAbilityCooldownNotifications()
        }
    }

    @HandleEvent(priority = HandleEvent.LOW)
    fun onWorldChange() {
        abilityStates.clear()
        currentNotificationAbility = null
        notificationStartTime = SimpleTimeMark.farPast()
        cachedMessageTemplate = ""
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        val ability = currentNotificationAbility ?: return

        if (notificationStartTime.passedSince() > config.titleDuration.toDouble().toDuration(DurationUnit.SECONDS)) {
            currentNotificationAbility = null
            return
        }

        val message = buildNotificationMessage(ability) ?: return
        val alertText = Renderable.text(message)

        config.position.renderRenderable(alertText, posLabel = "Ability Cooldown Notification")
    }

    private fun buildNotificationMessage(ability: ItemAbility): String? {
        if (cachedMessageTemplate.isEmpty()) return null
        return cachedMessageTemplate
            .replace("{ability}", ability.displayName)
            .replace("{time}", ability.getDurationText())
            .replace("&", "§")
    }

    private fun checkAbilityCooldownNotifications() {
        val enabledAbilities = config.enabledAbilities
        val threshold = config.notificationThreshold.seconds

        for (ability in enabledAbilities) {
            val currentOnCooldown = ability.isOnCooldown()
            val state = abilityStates[ability]
            val previousOnCooldown = state?.wasOnCooldown ?: currentOnCooldown
            var newLastNotifiedActivation = state?.lastNotifiedActivation ?: SimpleTimeMark.farPast()

            // State transition: from on-cooldown to ready
            if (threshold == 0.seconds && previousOnCooldown && !currentOnCooldown) {
                currentNotificationAbility = ability
                cachedMessageTemplate = config.readyMessage
                notificationStartTime = SimpleTimeMark.now()
                playNotificationSound(config.soundType)
            }
            // Ability is currently on cooldown, and we've crossed the threshold
            else if (currentOnCooldown) {
                val remainingTime = ability.getRemainingCooldown()
                if (remainingTime <= threshold && newLastNotifiedActivation != ability.lastActivation) {
                    currentNotificationAbility = ability
                    cachedMessageTemplate = config.soonMessage
                    notificationStartTime = SimpleTimeMark.now()
                    playNotificationSound(config.soundType)
                    newLastNotifiedActivation = ability.lastActivation
                }
            }

            // Update state for next check
            abilityStates[ability] = AbilityNotificationState(
                wasOnCooldown = currentOnCooldown,
                lastNotifiedActivation = newLastNotifiedActivation,
            )
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
