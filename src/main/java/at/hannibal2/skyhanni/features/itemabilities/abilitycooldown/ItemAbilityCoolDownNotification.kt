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

    private data class DisplayAbility(
        val ability: ItemAbility,
        val activation: SimpleTimeMark,
        val startTime: SimpleTimeMark,
    )
    private var currentDisplay: DisplayAbility? = null

    @HandleEvent(priority = HandleEvent.LOW)
    fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled()) return

        if (event.isMod(10)) {
            checkAbilityCooldownNotifications()
        }
    }

    @HandleEvent(priority = HandleEvent.LOW)
    fun onWorldChange() {
        currentDisplay = null
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        val display = currentDisplay ?: return

        val lifetime = config.titleDuration.toDouble().toDuration(DurationUnit.SECONDS)
        val elapsed = display.startTime.passedSince()

        if (elapsed > lifetime) {
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
        val template = if (ability.getRemainingCooldown() <= 100.milliseconds) {
            config.readyMessage
        } else {
            config.soonMessage
        }

        return template
            .replace("{ability}", ability.displayName)
            .replace("{time}", ability.getDurationText())
            .replace("&", "§")
    }

    private fun checkAbilityCooldownNotifications() {
        val threshold = config.notificationThreshold.seconds

        for (ability in config.enabledAbilities) {

            val onCooldown = ability.isOnCooldown()

            if (!onCooldown) continue
            val remaining = ability.getRemainingCooldown()
            if (remaining > threshold) continue

            val activation = ability.lastActivation

            currentDisplay = DisplayAbility(
                ability = ability,
                activation = activation,
                startTime = SimpleTimeMark.now(),
            )

            playNotificationSound(config.soundType)
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

    private fun isEnabled(): Boolean {
        return SkyBlockUtils.inSkyBlock &&
            config.enabled &&
            config.enabledAbilities.isNotEmpty()
    }
}
