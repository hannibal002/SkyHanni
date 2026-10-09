package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.CFApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import java.time.Instant
import kotlin.math.sin
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object HoppityCallWarning {
    // <editor-fold desc="Patterns">
    /**
     * WRAPPED-REGEX-TEST: "✆ Hoppity ✆ "
     */
    private val initHoppityCallPattern by CFApi.patternGroup.pattern(
        "hoppity.call.init.colorless",
        "✆ Hoppity ✆.*",
    )

    /**
     * REGEX-TEST: [NPC] Hoppity: ✆ What's up, oBlazin?
     */
    private val pickupHoppityCallPattern by CFApi.patternGroup.pattern(
        "hoppity.call.pickup.colorless",
        "\\[NPC] Hoppity: ✆ What's up, .*\\?",
    )
    // </editor-fold>

    private val config get() = HoppityEggsManager.config.hoppityCallWarning
    private var warningSound = SoundUtils.createSound("block.note_block.pling", 1f, isWarning = true)
    private var activeWarning = false
    private var nextWarningTime: Instant? = null
    private var finalWarningTime: Instant? = null
    private val callLength = 7.seconds

    @HandleEvent
    private fun onConfigLoad(event: ConfigLoadEvent) {
        val soundProperty = config.hoppityCallSound
        ConditionalUtils.onToggle(soundProperty) {
            warningSound = SoundUtils.createSound(soundProperty.get(), 1f, isWarning = true)
        }
        nextWarningTime = null
        finalWarningTime = null
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!isEnabled()) return
        if (initHoppityCallPattern.matches(event.cleanMessage)) startWarningUser()
        if (pickupHoppityCallPattern.matches(event.cleanMessage)) stopWarningUser()
    }

    @HandleEvent
    private fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        if (!activeWarning) return
        if (nextWarningTime == null || finalWarningTime == null) return
        val currentTime = Instant.now()
        if (currentTime.isAfter(nextWarningTime)) {
            SoundUtils.repeatSound(100, 10, warningSound)
            nextWarningTime = currentTime.plusMillis(100)
        }
        if (currentTime >= finalWarningTime) stopWarningUser()
    }

    @HandleEvent
    private fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled() || !activeWarning) return
        // Calculate a fluctuating alpha value based on the sine of time, for a smooth oscillation
        val randomizationAlphaDouble = ((2 + sin(Instant.now().toEpochMilli().toDouble() / 1000)) * 255 / 4)
        // Ensure the alpha value is an integer and within the valid range (0-255)
        val randomizationAlphaInt = randomizationAlphaDouble.toInt().coerceIn(0..255)
        // Shift the alpha value 24 bits to the left to position it in the color's alpha channel.
        val shiftedRandomAlpha = randomizationAlphaInt shl 24
        GuiRenderUtils.drawRect(
            0,
            0,
            GuiScreenUtils.displayWidth,
            GuiScreenUtils.displayHeight,
            // Apply the shifted alpha and combine it with the RGB components of flashColor.
            shiftedRandomAlpha or (config.flashColor.toColor().rgb and 0xFFFFFF),
        )
    }

    @HandleEvent
    private fun onWorldChange() {
        stopWarningUser()
    }

    private fun startWarningUser() {
        if (activeWarning) return
        activeWarning = true
        SoundUtils.repeatSound(100, 10, warningSound)
        val currentTime = Instant.now()
        nextWarningTime = currentTime.plusMillis(100)
        finalWarningTime = finalWarningTime ?: currentTime.plusMillis(callLength.inWholeMilliseconds)
    }

    private fun stopWarningUser() {
        activeWarning = false
        finalWarningTime = null
        nextWarningTime = null
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled
}
