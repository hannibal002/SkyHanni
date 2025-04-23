package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.garden.SensitivityReducerConfig
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.mixins.hooks.MouseSensitivityHook
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.client.Minecraft

@SkyHanniModule
object SensitivityReducer {
    private val config get() = SkyHanniMod.feature.garden.sensitivityReducer

    private var inBarn: Boolean = false
    private var onGround: Boolean = false

    private var shouldBeActive = false //like isActive, but doesn't get skipped by ground or barn checks
    private val isActive get() = isAutoActive || isManualActive
    private val isAutoActive get() = MouseSensitivityHook.state == MouseSensitivityHook.MouseSensitivityState.AUTO_REDUCED
    private val isManualActive get() = MouseSensitivityHook.state == MouseSensitivityHook.MouseSensitivityState.MANUAL_REDUCED
    private val isMouseLocked get() = MouseSensitivityHook.state == MouseSensitivityHook.MouseSensitivityState.LOCKED

    @HandleEvent
    fun onTick() {
        if (!GardenApi.inGarden()) {
            if (isAutoActive) autoToggle()
            return
        }
        if (isMouseLocked) return

        updatePlayerStatus()
        autoToggleIfNeeded()
    }

    private fun updatePlayerStatus() {
        if (GardenApi.onBarnPlot && !inBarn) {
            inBarn = true
            tryAutoToggle(false)
        } else if (!GardenApi.onBarnPlot && inBarn) {
            inBarn = false
            tryAutoToggle(true)
        }
        if (MinecraftCompat.localPlayer.onGround && !onGround) {
            onGround = true
            tryAutoToggle(true)
        } else if (!MinecraftCompat.localPlayer.onGround && onGround) {
            onGround = false
            tryAutoToggle(false)
        }
    }

    private fun tryAutoToggle(enable: Boolean) {
        if (!isAutoActive) return

        if (!isActive) {
            shouldBeActive = true
            MouseSensitivityHook.setMouseSensitivityState(MouseSensitivityHook.MouseSensitivityState.AUTO_REDUCED)
        } else {
            shouldBeActive = false
            MouseSensitivityHook.setMouseSensitivityState(MouseSensitivityHook.MouseSensitivityState.DEFAULT)
        }
    }

    private fun autoToggleIfNeeded() {
        when (config.mode) {
            SensitivityReducerConfig.Mode.OFF -> toggleIfCondition { false }
            SensitivityReducerConfig.Mode.TOOL ->  toggleIfCondition(::isHoldingTool)
            SensitivityReducerConfig.Mode.KEYBIND -> toggleIfCondition(::isHoldingKey)
        }
    }

    private fun toggleIfCondition(check: () -> Boolean) {
        val conditionMet = check()

        if (conditionMet && !isActive) autoToggle()
        else if (isActive && !conditionMet) autoToggle()
    }

    private fun autoToggle() {
        if (config.onlyPlot.get() && inBarn) return
        if (config.onGround.get() && !onGround) return
        if (!isActive) {
            shouldBeActive = true
            MouseSensitivityHook.setMouseSensitivityState(MouseSensitivityHook.MouseSensitivityState.AUTO_REDUCED)
        } else {
            shouldBeActive = false
            MouseSensitivityHook.setMouseSensitivityState(MouseSensitivityHook.MouseSensitivityState.DEFAULT)
        }
    }

    private fun manualToggle() {
        if (!isActive) {
            shouldBeActive = true
            MouseSensitivityHook.setMouseSensitivityState(MouseSensitivityHook.MouseSensitivityState.MANUAL_REDUCED)
            ChatUtils.chat("§bMouse sensitivity is now lowered. Type /shsensreduce to restore your sensitivity.")
        } else {
            shouldBeActive = false
            MouseSensitivityHook.setMouseSensitivityState(MouseSensitivityHook.MouseSensitivityState.DEFAULT)
            ChatUtils.chat("§bMouse sensitivity is now restored.")
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shsensreduce") {
            description = "Lowers the mouse sensitivity for easier small adjustments (for farming)"
            category = CommandCategory.USERS_ACTIVE
            callback { manualToggle() }
        }
    }

    @HandleEvent(eventType = GuiRenderEvent.GuiOverlayRenderEvent::class)
    fun onRenderOverlay() {
        if (!isActive) return
        if (!config.showGui) return
        config.position.renderString("§eSensitivity Lowered", posLabel = "Sensitivity Lowered")
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(80, "garden.sensitivityReducerConfig", "garden.sensitivityReducer")
        event.move(81, "garden.sensitivityReducer.showGUI", "garden.sensitivityReducer.showGui")
    }

    private fun isHoldingTool(): Boolean = GardenApi.toolInHand != null
    private fun isHoldingKey(): Boolean = config.keybind.isKeyHeld() && Minecraft.getMinecraft().currentScreen == null
}
