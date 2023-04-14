package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RenderRealOverlayEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.ResettingTimer
import at.hannibal2.skyhanni.utils.SoundUtils
import io.github.moulberry.moulconfig.ChromaColour
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.EnumChatFormatting.GREEN
import net.minecraft.util.EnumChatFormatting.RED
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import org.lwjgl.input.Keyboard
import kotlin.math.sin
import kotlin.time.Duration.Companion.seconds

/*
 * I disclaim any and all responsibility over the legality of this module. Blame HsFearless
 */
class IdleWarning {

    val config get() = SkyHanniMod.feature.garden
    val lastGainedCrops = ResettingTimer()
    var lastCropAmount = 0

    fun isSlackingOff() = lastGainedCrops.hasPassed(config.idleNotifierTimeout.toDouble().seconds)

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        if (!config.idleNotifier) {
            lastGainedCrops.reset()
            return
        }
        val player = Minecraft.getMinecraft().thePlayer ?: return
        val heldItem = player.heldItem
        if (heldItem == null || !GardenAPI.isTool(heldItem)) {
            lastGainedCrops.reset()
            return
        }
        val count = GardenAPI.readCounter(heldItem)
        if (lastCropAmount != count) {
            lastCropAmount = count
            lastGainedCrops.reset()
        }
        if (isSlackingOff() && config.idleNotifierStrategy.isAudio)
            SoundUtils.playBeepSound()
    }

    @SubscribeEvent
    fun onKeybind(event: KeyInputEvent) {
        if (!Keyboard.getEventKeyState()) return
        val key = if (Keyboard.getEventKey() == 0) Keyboard.getEventCharacter().code + 256 else Keyboard.getEventKey()
        if (key == config.idleNotifierKeybinding) {
            config.idleNotifier = !config.idleNotifier
            LorenzUtils.chat("Idle Notifier ${if (config.idleNotifier) "${GREEN}Enabled" else "${RED}Disabled"}")
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onRender(event: RenderRealOverlayEvent) {
        if (!config.idleNotifier || !isSlackingOff()) return
        if (!config.idleNotifierStrategy.isVisual) return
        val mc = Minecraft.getMinecraft()
        val alpha = ((1 + sin(System.currentTimeMillis().toDouble() / 1000)) * 255 / 4).toInt().coerceIn(0..255)
        Gui.drawRect(
            0, 0, mc.displayWidth, mc.displayHeight,
            (alpha shl 24) or (ChromaColour.specialToChromaRGB(config.idleNotifierColor) and 0xFFFFFF)
        )
        GlStateManager.color(1F, 1F, 1F, 1F)
    }
}