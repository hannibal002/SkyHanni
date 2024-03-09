package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.features.garden.SensitivityReducer
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object LockMouseLook {

    private val config get() = SkyHanniMod.feature.misc
    private val storage get() = SkyHanniMod.feature.storage
    var lockedMouse = false
    private const val lockedPosition = -1F / 3F

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        if (lockedMouse) toggleLock()
        val gameSettings = Minecraft.getMinecraft().gameSettings
        if (gameSettings.mouseSensitivity == lockedPosition) {
            gameSettings.mouseSensitivity = storage.savedMouselockedSensitivity
            ChatUtils.chat("§bMouse rotation is now unlocked because you left it locked.")
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!event.message.startsWith("§aTeleported you to §r§aPlot")) return
        if (lockedMouse) toggleLock()
    }

    fun toggleLock() {
        lockedMouse = !lockedMouse

        val gameSettings = Minecraft.getMinecraft().gameSettings ?: return
        var mouseSensitivity = gameSettings.mouseSensitivity
        if (SensitivityReducer.isEnabled()) mouseSensitivity = SensitivityReducer.doTheMath(mouseSensitivity, true)

        if (lockedMouse) {
            storage.savedMouselockedSensitivity = mouseSensitivity
            gameSettings.mouseSensitivity = lockedPosition
            if (config.lockMouseLookChatMessage) {
                ChatUtils.chat("§bMouse rotation is now locked. Type /shmouselock to unlock your rotation")
            }
        } else {
            if (!SensitivityReducer.isEnabled()) gameSettings.mouseSensitivity = storage.savedMouselockedSensitivity
            else gameSettings.mouseSensitivity = SensitivityReducer.doTheMath(storage.savedMouselockedSensitivity)
            if (config.lockMouseLookChatMessage) {
                ChatUtils.chat("§bMouse rotation is now unlocked.")
            }
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!lockedMouse) return
        config.lockedMouseDisplay.renderString("§eMouse Locked", posLabel = "Mouse Locked")
    }

    fun autoDisable() {
        if (lockedMouse) {
            toggleLock()
        }
    }

    @SubscribeEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Mouse Lock")

        if (!lockedMouse) {
            event.addIrrelevant("not enabled")
            return
        }

        event.addData {
            add("Stored Sensitivity: ${storage.savedMouselockedSensitivity}")
        }
    }
}
