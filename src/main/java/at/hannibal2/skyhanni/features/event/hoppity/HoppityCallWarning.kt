package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryAPI
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
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
import net.minecraft.util.IChatComponent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard
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
     * REGEX-TEST: §a✆ RING... §r §r§2§l[PICK UP]
     * REGEX-TEST: §a✆ RING... RING... §r §r§2§l[PICK UP]
     * REGEX-TEST: §a✆ RING... RING... RING... §r §r§2§l[PICK UP]
     */
    private val callRingPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "hoppity.call.ring",
        "§a✆ (?:RING\\.{3} ){1,3}§r §r§2§l\\[PICK UP]"
    )

    /**
     * REGEX-TEST: eaf78cc9-260d-407f-b1df-efea83e5038a
     * REGEX-TEST: 2bf7445f-2f86-406a-b629-deb5e6e03faa
     */
    private val cbUuidPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "hoppity.call.uuid",
        "[a-f0-9]{8}-(?:[a-f0-9]{4}-){3}[a-f0-9]{12}"
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
    private var acceptUUID: String? = null

    @SubscribeEvent
    fun onKeybind(event: GuiKeyPressEvent) {
        if (acceptUUID == null) return
        if (config.acceptHotkey == Keyboard.KEY_NONE) return
        if (!config.acceptHotkey.isKeyHeld()) return
        if (lastAcceptSent.passedSince() < 3.seconds) return
        lastAcceptSent = SimpleTimeMark.now()
        HypixelCommands.cb(acceptUUID!!)
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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onChat(event: LorenzChatEvent) {
        if (callRingPattern.matches(event.message) && acceptUUID == null) acceptUUID = extractPickupUuid(event)
        if (!isEnabled()) return
        if (initHoppityCallPattern.matches(event.message)) startWarningUser()
        if (pickupHoppityCallPattern.matches(event.message)) stopWarningUser()
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

    private fun extractPickupUuid(event: LorenzChatEvent): String? {
        val siblings = event.chatComponent.siblings
        if (siblings.size < 2) return null
        if (!siblings[0].chatStyle.chatClickEvent.value.startsWith("/cb")) return null
        ChatUtils.chat("§Style event value: §b${siblings[0].chatStyle.chatClickEvent.value}")
        val uuid = siblings[0].chatStyle.chatClickEvent.value.substring(4)
        return if (cbUuidPattern.matches(uuid)) uuid else null
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
        acceptUUID = null
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
