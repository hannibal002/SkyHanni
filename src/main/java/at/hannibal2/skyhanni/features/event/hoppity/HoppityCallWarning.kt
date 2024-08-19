package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryAPI
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColorInt
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.sin
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object HoppityCallWarning {

    /**
     * REGEX-TEST: §e✆ §r§bHoppity§r§e ✆
     * REGEX-TEST: §e✆ §r§aHoppity§r§e ✆
     */
    private val initHoppityCallPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "hoppity.call.init",
        "§e✆ §r(?:§a|§b)Hoppity§r§e ✆"
    )

    /**
     * REGEX-TEST: §e[NPC] §aHoppity§f: §b✆ §f§rWhat's up, §boBlazin§f?
     */
    private val pickupHoppityCallPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "hoppity.call.pickup",
        "§e\\[NPC] §aHoppity§f: §b✆ §f§rWhat's up, .*§f\\?"
    )

    private val config get() = HoppityEggsManager.config.hoppityCallWarning
    private var warningSound = SoundUtils.createSound("note.pling", 1f)
    private var activeWarning = false
    private var nextWarningTime = 0L
    private var finalWarningTime = 0L
    private const val CALL_LENGTH_MS = 7000
    private var lastAcceptSent = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onKeybind(event: GuiKeyPressEvent) {
        if (!config.acceptHotkey.isKeyHeld()) return
        if (lastAcceptSent.passedSince() < 3.seconds) return
        lastAcceptSent = SimpleTimeMark.now()
        // Call-back? Hoppity ID: eaf78cc9-260d-407f-b1df-efea83e5038a
        HypixelCommands.cb("eaf78cc9-260d-407f-b1df-efea83e5038a")
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        val soundProperty = config.hoppityCallSound
        ConditionalUtils.onToggle(soundProperty) {
            warningSound = SoundUtils.createSound(soundProperty.get(), 1f)
        }
        nextWarningTime = 0L
        finalWarningTime = 0L
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        val message = event.message
        if (initHoppityCallPattern.matches(message)) startWarningUser()
        if (pickupHoppityCallPattern.matches(message)) stopWarningUser()
    }

    @SubscribeEvent
    fun onTick(event: SecondPassedEvent) {
        if (!isEnabled()) return
        if (!activeWarning) return
        if (nextWarningTime == 0L || finalWarningTime == 0L) return
        val currentTime = System.currentTimeMillis()
        if (currentTime >= nextWarningTime) {
            SoundUtils.repeatSound(100, 10, warningSound)
            nextWarningTime = currentTime + 100
        }
        if (currentTime >= finalWarningTime) stopWarningUser()
    }

    @SubscribeEvent
    fun onRender(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!activeWarning) return
        val minecraft = Minecraft.getMinecraft()
        val alpha = ((2 + sin(System.currentTimeMillis().toDouble() / 1000)) * 255 / 4).toInt().coerceIn(0..255)
        Gui.drawRect(
            0,
            0,
            minecraft.displayWidth,
            minecraft.displayHeight,
            (alpha shl 24) or (config.flashColor.toChromaColorInt() and 0xFFFFFF),
        )
        GlStateManager.color(1F, 1F, 1F, 1F)
    }

    private fun startWarningUser() {
        if(activeWarning) return
        activeWarning = true
        SoundUtils.repeatSound(100, 10, warningSound)
        val currentTime = System.currentTimeMillis()
        nextWarningTime = currentTime + 100
        if (finalWarningTime == 0L) finalWarningTime = currentTime + CALL_LENGTH_MS
    }

    private fun stopWarningUser() {
        activeWarning = false
        finalWarningTime = 0L
        nextWarningTime = 0L
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
