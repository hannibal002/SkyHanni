package at.hannibal2.skyhanni.features.itemabilities.abilitycooldown

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.itemability.ItemAbilityCooldownNotificationConfig.NotificationSound
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.itemabilities.ItemAbilityActivateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@SkyHanniModule
object ItemAbilityCooldownNotification {
    private val config get() = SkyHanniMod.feature.inventory.itemAbilities.abilityCooldownNotifications
    private val COOLDOWN_READY_GRACE_PERIOD = 100.milliseconds
    private var worldSessionId = 0

    private data class DisplayAbility(
        val ability: ItemAbility,
        val startTime: SimpleTimeMark,
    )

    private var currentDisplay: DisplayAbility? = null

    @HandleEvent
    fun onAbilityActivate(event: ItemAbilityActivateEvent) {
        if (!isEnabled()) return
        val ability = event.ability
        if (ability !in config.enabledAbilities) return
        val lastActivate = ability.lastActivation
        val delayTime = (ability.getRemainingCooldown() - thresholdDuration).coerceAtLeast(0.seconds)
        val worldSessionIdAtSchedule = worldSessionId
        DelayedRun.runDelayed(delayTime) {
            if (!isEnabled()) return@runDelayed
            if (ability.lastActivation != lastActivate) return@runDelayed
            if (worldSessionId != worldSessionIdAtSchedule) return@runDelayed
            updateCurrentDisplay(ability)
        }
    }

    @HandleEvent(priority = HandleEvent.LOW)
    fun onWorldChange() {
        worldSessionId++
        currentDisplay = null
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        val display = currentDisplay ?: return
        if (display.startTime.passedSince() > titleDuration) {
            currentDisplay = null
            return
        }

        val message = buildNotificationMessage(display.ability)
        val alertText = Renderable.text(message)

        config.position.renderRenderable(
            alertText,
            posLabel = "Ability Cooldown Notification",
        )
    }

    private fun buildNotificationMessage(ability: ItemAbility): String {
        val template = if (ability.getRemainingCooldown() <= COOLDOWN_READY_GRACE_PERIOD) {
            config.readyMessage
        } else {
            config.soonMessage
        }

        return template
            .replace("{ability}", ability.displayName)
            .replace("{time}", ability.getDurationText())
            .replace("&", "§")
    }

    private fun updateCurrentDisplay(ability: ItemAbility) {
        currentDisplay = DisplayAbility(
            ability = ability,
            startTime = SimpleTimeMark.now(),
        )
        playNotificationSound(config.soundType)
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

    private val titleDuration: Duration
        get() {
        return config.titleDuration.toDouble().toDuration(DurationUnit.SECONDS)
    }

    private val thresholdDuration: Duration
        get() {
            return config.notificationThreshold.toDouble().toDuration(DurationUnit.SECONDS)
        }

    private fun isEnabled(): Boolean {
        return SkyBlockUtils.inSkyBlock &&
            config.enabled &&
            config.enabledAbilities.isNotEmpty()
    }
}
