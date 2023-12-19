package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
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
        if (!LorenzUtils.inSkyBlock) return
        if (isManualToggle) return
        if (!config.enabled) return
        if (LorenzUtils.skyBlockIsland != IslandType.GARDEN) return
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
        LorenzUtils.chat("§bMouse sensitivity is now restored.")
        gameSettings!!.mouseSensitivity = SkyHanniMod.feature.storage.savedMouseloweredSensitivity
    }

    private fun toggleSens() {
        gameSettings = Minecraft.getMinecraft().gameSettings ?: return
        if (!isToggled) {
            SkyHanniMod.feature.storage.savedMouseloweredSensitivity = gameSettings!!.mouseSensitivity
            val newSens = ((SkyHanniMod.feature.storage.savedMouseloweredSensitivity+(1F / 3F))/config.divisorSens)-(1F / 3F)
            gameSettings!!.mouseSensitivity = newSens
        } else {
            gameSettings!!.mouseSensitivity = SkyHanniMod.feature.storage.savedMouseloweredSensitivity
        }
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
        if (isManualToggle) {
            SkyHanniMod.feature.storage.savedMouseloweredSensitivity = gameSettings!!.mouseSensitivity
            val newSens = ((SkyHanniMod.feature.storage.savedMouseloweredSensitivity+(1F / 3F))/config.divisorSens)-(1F / 3F)
            gameSettings!!.mouseSensitivity = newSens
            LorenzUtils.chat("§bMouse sensitivity is now lowered. Type /shmouselower to restore your sensitivity.")
        } else {
            gameSettings!!.mouseSensitivity = SkyHanniMod.feature.storage.savedMouseloweredSensitivity
            LorenzUtils.chat("§bMouse sensitivity is now restored.")
        }
    }
}
