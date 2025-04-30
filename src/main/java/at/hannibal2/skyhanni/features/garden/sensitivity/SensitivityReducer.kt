package at.hannibal2.skyhanni.features.garden.sensitivity

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.garden.SensitivityReducerConfig
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.sensitivity.MouseSensitivityManager.SensitivityState
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.client.Minecraft

@SkyHanniModule
object SensitivityReducer {
    private val config get() = SkyHanniMod.feature.garden.sensitivityReducer

    private var inBarn: Boolean = false
    private var onGround: Boolean = false

    private var shouldBeActive = false

    private val isActive get() = isAutoActive || isManualActive
    private val isAutoActive get() = SensitivityState.AUTO_REDUCED.isActive()
    private val isManualActive get() = SensitivityState.MANUAL_REDUCED.isActive()

    @HandleEvent
    fun onTick() {
        if (!GardenApi.inGarden()) {
            if (isAutoActive) autoToggle()
            return
        }
        if (SensitivityState.LOCKED.isActive()) return

        updatePlayerStatus()
        autoToggleIfNeeded()
    }

    @HandleEvent(eventType = ConfigLoadEvent::class)
    fun onConfigLoad() {
        config.reducingFactor.afterChange {
            MouseSensitivityManager.destroyCache()
        }
        config.onlyPlot.afterChange {
            autoToggle()
        }
        config.onGround.afterChange {
            autoToggle()
        }
    }

    private fun updatePlayerStatus() {
        val newInBarn = GardenApi.onBarnPlot
        val newOnGround = MinecraftCompat.localPlayer.onGround

        if (inBarn != newInBarn) {
            inBarn = newInBarn
            tryAutoToggle()
        }

        if (onGround != newOnGround) {
            onGround = newOnGround
            tryAutoToggle()
        }
    }

    private fun tryAutoToggle() {
        if (!isAutoActive) return

        if (!isActive) {
            shouldBeActive = true
            MouseSensitivityManager.state = SensitivityState.AUTO_REDUCED
        } else {
            shouldBeActive = false
            MouseSensitivityManager.state = SensitivityState.UNCHANGED
        }
    }

    private fun autoToggleIfNeeded() {
        when (config.mode) {
            SensitivityReducerConfig.Mode.OFF -> toggleIfCondition { false }
            SensitivityReducerConfig.Mode.TOOL -> toggleIfCondition(::isHoldingTool)
            SensitivityReducerConfig.Mode.KEYBIND -> toggleIfCondition(::isHoldingKey)
        }
    }

    private fun toggleIfCondition(check: () -> Boolean) {
        val conditionMet = check()

        if (conditionMet && !isActive) autoToggle()
        else if (isActive && !conditionMet) autoToggle()
    }

    private fun autoToggle() {
        if (config.onlyPlot.get() && inBarn) {
            if (isActive) disable()
            return
        }
        if (config.onGround.get() && !onGround) {
            if (isActive) disable()
            return
        }

        if (isActive) disable()
        else enable()
    }

    private fun disable() {
        shouldBeActive = false
        MouseSensitivityManager.state = SensitivityState.UNCHANGED
    }

    private fun enable() {
        shouldBeActive = true
        MouseSensitivityManager.state = SensitivityState.AUTO_REDUCED
    }

    private fun manualToggle() {
        if (!isActive) {
            shouldBeActive = true
            MouseSensitivityManager.state = SensitivityState.MANUAL_REDUCED
            ChatUtils.chat("§bMouse sensitivity is now lowered. Type /shsensreduce to restore your sensitivity.")
        } else {
            shouldBeActive = false
            MouseSensitivityManager.state = SensitivityState.UNCHANGED
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
