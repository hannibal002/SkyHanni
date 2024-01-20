package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import kotlinx.coroutines.processNextEventInCurrentThread
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object SensReducer {
    private val config get() = SkyHanniMod.feature.garden.sensReducerConfig
    private val storage get() = SkyHanniMod.feature.storage
    private var isToggled = false
    private var isManualToggle = false
    private val gameSettings get() = Minecraft.getMinecraft().gameSettings

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!GardenAPI.inGarden()) {
//             if (isToggled) {          //TODO look into a fix for onWorldChange
//                 isToggled = false     //this is a temp unideal fix
//                 restoreSensitivity()
//             }
            return
        }
        if (isManualToggle) return
        if (!config.enabled) return
        if (isHoldingTool() && !isToggled) {
            toggleSens()
            isToggled = true
        } else if (!isHoldingTool() && isToggled) {
            toggleSens()
            isToggled = false
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!(isToggled || isManualToggle)) return
        if (!config.showLower) return
        config.loweredSensPosition.renderString("§eSensitivity Lowered)", posLabel = "Sensitivity Lowered")
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        if (isManualToggle) return
        if (!isToggled) return
        isToggled = false
        restoreSensitivity()
    }

    private fun toggleSens() {
        if (!isToggled) {
            lowerSensitivity()
        } else restoreSensitivity()
    }

    private fun isHoldingTool(): Boolean {
        return GardenAPI.toolInHand != null
    }

    fun manualToggle() {
        if (isToggled) {
            LorenzUtils.chat("This command is disabled while holding a farming tool.")
            return
        }
        isManualToggle = !isManualToggle
        if (isManualToggle) {
            lowerSensitivity(true)
        } else restoreSensitivity(true)
    }

    private fun lowerSensitivity(showMessage: Boolean = false) {
        storage.savedMouseloweredSensitivity = gameSettings.mouseSensitivity
        val newSens =
            ((storage.savedMouseloweredSensitivity + (1F / 3F)) / config.divisorSens) - (1F / 3F)
        gameSettings?.mouseSensitivity = newSens
        if (showMessage) LorenzUtils.chat("§bMouse sensitivity is now lowered. Type /shsensreduce to restore your sensitivity.")
    }

    private fun restoreSensitivity(showMessage: Boolean = false) {
        gameSettings?.mouseSensitivity = SkyHanniMod.feature.storage.savedMouseloweredSensitivity
        if (showMessage) LorenzUtils.chat("§bMouse sensitivity is now restored.")
    }
}
