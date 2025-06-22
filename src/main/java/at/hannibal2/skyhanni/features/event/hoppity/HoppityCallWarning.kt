package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.PurseApi
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.CFApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SpecialColor.toSpecialColorInt
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import net.minecraft.client.renderer.GlStateManager
import java.time.Instant
import kotlin.math.sin
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object HoppityCallWarning {

    // <editor-fold desc="Patterns">
    /**
     * Test messages (and the real ones from Hypixel) have a space at the end of
     * them that the IDE kills. So it's "§r§e ✆ "
     *
     * REGEX-TEST: §e✆ §r§bHoppity§r§e ✆
     * REGEX-TEST: §e✆ §r§aHoppity§r§e ✆
     */
    private val initHoppityCallPattern by CFApi.patternGroup.pattern(
        "hoppity.call.init",
        "§e✆ §r(?:§a|§b)Hoppity§r§e ✆.*",
    )

    /**
     * REGEX-TEST: §e[NPC] §aHoppity§f: §b✆ §f§rWhat's up, §boBlazin§f?
     */
    private val pickupHoppityCallPattern by CFApi.patternGroup.pattern(
        "hoppity.call.pickup",
        "§e\\[NPC] §aHoppity§f: §b✆ §f§rWhat's up, .*§f\\?",
    )
    // </editor-fold>

    private val config get() = HoppityEggsManager.config.hoppityCallWarning
    private var warningSound = SoundUtils.createSound("note.pling", 1f)
    private var activeWarning = false
    private var nextWarningTime: Instant? = null
    private var finalWarningTime: Instant? = null
    private val callLength = 7.seconds
    private var commandSentTimer = SimpleTimeMark.farPast()

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        val soundProperty = config.hoppityCallSound
        ConditionalUtils.onToggle(soundProperty) {
            warningSound = SoundUtils.createSound(soundProperty.get(), 1f)
        }
        nextWarningTime = null
        finalWarningTime = null
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onChat(event: SkyHanniChatEvent) {
        if (!isEnabled()) return
        if (initHoppityCallPattern.matches(event.message)) startWarningUser()
        if (pickupHoppityCallPattern.matches(event.message)) stopWarningUser()
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
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
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
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
            shiftedRandomAlpha or (config.flashColor.toSpecialColorInt() and 0xFFFFFF),
        )
        GlStateManager.color(1F, 1F, 1F, 1F)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onCommandSend(event: MessageSendToServerEvent) {
        if (!HoppityApi.pickupOutgoingCommandPattern.matches(event.message)) return
        if (!config.ensureCoins || commandSentTimer.passedSince() < 5.seconds) return
        if (PurseApi.getPurse() >= config.coinThreshold) return

        commandSentTimer = SimpleTimeMark.now()
        event.cancel()
        ChatUtils.clickToActionOrDisable(
            "§cBlocked picking up Hoppity without enough coins!",
            config::ensureCoins,
            actionName = "open bank menu",
            // TODO if no booster cookie active, suggest to warp to hub/path find to bank. ideally into an utils
            action = { HypixelCommands.bank() },
        )
    }

    @HandleEvent
    fun onWorldChange() {
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
