package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object LockMouseLook {
    private val config get() = SkyHanniMod.feature.misc
    private var lockedMouse = false
    private const val lockedPosition = -1F / 3F

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        if (lockedMouse) toggleLock()
        val gameSettings = Minecraft.getMinecraft().gameSettings
        if (gameSettings.mouseSensitivity == lockedPosition) {
            gameSettings.mouseSensitivity = SkyHanniMod.feature.storage.savedMouseSensitivity
            LorenzUtils.chat("§bMouse rotation is now unlocked because you left it locked.")
        }
    }

    fun toggleLock() {
        val gameSettings = Minecraft.getMinecraft().gameSettings ?: return
        lockedMouse = !lockedMouse

        if (lockedMouse) {
            SkyHanniMod.feature.storage.savedMouseSensitivity = gameSettings.mouseSensitivity
            gameSettings.mouseSensitivity = lockedPosition
            if (config.lockMouseLookChatMessage) {
                LorenzUtils.chat("§bMouse rotation is now locked. Type /shmouselock to unlock your rotation")
            }
        } else {
            gameSettings.mouseSensitivity = SkyHanniMod.feature.storage.savedMouseSensitivity
            if (config.lockMouseLookChatMessage) {
                LorenzUtils.chat("§bMouse rotation is now unlocked.")
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
}
