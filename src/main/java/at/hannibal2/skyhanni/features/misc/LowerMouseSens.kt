package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object LowerMouseSens {
    private val config get() = SkyHanniMod.feature.garden.MouseSensConfig
    private var isToggled = false
    private var isManualToggle = false
    private var gameSettings = Minecraft.getMinecraft().gameSettings ?: null

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!GardenAPI.inGarden()) return
        if (isManualToggle) return
        if (!config.enabled) return
        if (isHoldingTool() && !isToggled) {
            toggleSens()
            isToggled = true
        } else if (!isHoldingTool() && isToggled){
            toggleSens()
            isToggled = false
        }
    }
    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!(isToggled || isManualToggle)) return
        if (!config.showLower) return
        config.loweredMouseDisplay.renderString("§eSensitivity Lowered", posLabel = "Sensitivity Lowered")
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        if (!isToggled) return
        isToggled = false
        gameSettings!!.mouseSensitivity = SkyHanniMod.feature.storage.savedMouseloweredSensitivity
    }

    private fun toggleSens() {
        gameSettings = Minecraft.getMinecraft().gameSettings ?: return
        if (!isToggled) {
            lowerSensitivity()
        } else restoreSensitivity()
    }

    private fun isHoldingTool(): Boolean {
        return GardenAPI.toolInHand != null
    }

    fun manualToggle() {
        if (isToggled) {
            LorenzUtils.chat("The manual toggle is disabled if the feature is enabled from holding a tool.")
            return
        }
        gameSettings = Minecraft.getMinecraft().gameSettings ?: return
        isManualToggle = !isManualToggle
        if (isManualToggle) { lowerSensitivity(true)
        } else restoreSensitivity(true)
    }

    private fun lowerSensitivity(showMessage: Boolean = false) {
        SkyHanniMod.feature.storage.savedMouseloweredSensitivity = gameSettings!!.mouseSensitivity
        val newSens = ((SkyHanniMod.feature.storage.savedMouseloweredSensitivity+(1F / 3F))/config.divisorSens)-(1F / 3F)
        gameSettings!!.mouseSensitivity = newSens
        if (showMessage) LorenzUtils.chat("§bMouse sensitivity is now lowered. Type /shmouselower to restore your sensitivity.")
    }
    private fun restoreSensitivity(showMessage: Boolean = false) {
        gameSettings!!.mouseSensitivity = SkyHanniMod.feature.storage.savedMouseloweredSensitivity
        if (showMessage) LorenzUtils.chat("§bMouse sensitivity is now restored.")
    }
}
