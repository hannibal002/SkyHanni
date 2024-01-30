package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.afterChange
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import com.sun.org.apache.xpath.internal.operations.Bool
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object SensReducer {
    private val config get() = SkyHanniMod.feature.garden.sensReducerConfig
    private val storage get() = SkyHanniMod.feature.storage
    private var isToggled = false
    private var isManualToggle = false
    private var lastCheckCooldown = SimpleTimeMark.farPast()

    private val mc get() = Minecraft.getMinecraft()
    private val gameSettings = mc.gameSettings

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!GardenAPI.inGarden()) {
            if (isToggled && lastCheckCooldown.passedSince() > 1.seconds) {
                lastCheckCooldown = SimpleTimeMark.now()
                isToggled = false
                restoreSensitivity()
            }
            return
        }
        if (isManualToggle) return
        if (!config.enabled) {
            if (isToggled) restoreSensitivity()
            return
        }
        if (isToggled && config.inGround && !mc.thePlayer.onGround) {
            restoreSensitivity()
            isToggled = false
            return
        }
        if (config.useKeybind) {
            if (config.keybind.isKeyHeld() && !isToggled)  toggle(true)
            else if (!config.keybind.isKeyHeld() && isToggled) toggle(false)
        } else {
            if (isHoldingTool() && !isToggled) toggle(true)
            else if (!isHoldingTool() && isToggled) toggle(false)
        }
    }

    @SubscribeEvent
    fun onConfigInit(event: ConfigLoadEvent) {
        config.reducingFactor.afterChange {
            reloadSensitivity()
        }
    }

    private fun reloadSensitivity() {
        if (isToggled || isManualToggle) {
            restoreSensitivity()
            lowerSensitivity()
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!(isToggled || isManualToggle)) return
        if (!config.showGUI) return
        config.position.renderString("§eSensitivity Lowered", posLabel = "Sensitivity Lowered")
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
        val divisor = config.reducingFactor.get()
        LorenzUtils.debug("dividing by $divisor")
        val newSens =
            ((storage.savedMouseloweredSensitivity + (1F / 3F)) / divisor) - (1F / 3F)
        gameSettings?.mouseSensitivity = newSens
        if (showMessage) LorenzUtils.chat("§bMouse sensitivity is now lowered. Type /shsensreduce to restore your sensitivity.")
    }

    private fun restoreSensitivity(showMessage: Boolean = false) {
        gameSettings?.mouseSensitivity = SkyHanniMod.feature.storage.savedMouseloweredSensitivity
        if (showMessage) LorenzUtils.chat("§bMouse sensitivity is now restored.")
    }

    private fun toggle(on: Boolean) {
        if (config.inPlot && GardenAPI.onBarnPlot) return
        if (config.inGround && !Minecraft.getMinecraft().thePlayer.onGround) return
        if (!isToggled) {
            lowerSensitivity()
        } else restoreSensitivity()
        isToggled = on
    }
}
