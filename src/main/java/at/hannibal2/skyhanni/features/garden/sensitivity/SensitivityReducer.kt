package at.hannibal2.skyhanni.features.garden.sensitivity

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.garden.SensitivityReducerConfig
import at.hannibal2.skyhanni.features.fishing.FishingApi
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.sensitivity.MouseSensitivityManager.SensitivityState
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import net.minecraft.client.Minecraft

@SkyHanniModule
object SensitivityReducer {

    private val config get() = SkyHanniMod.feature.garden.sensitivityReducer

    private val SQUEAKY_MOUSEMAT = "SQUEAKY_MOUSEMAT".toInternalName()

    val REDUCING_FACTOR_HARD_BOUNDS = 1f..10_000f

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

    @HandleEvent
    fun onConfigLoad() {
        config.reducingFactor.afterChange {
            val coerced = coerceIn(REDUCING_FACTOR_HARD_BOUNDS)
            if (this != coerced) {
                config.reducingFactor.set(coerced)
                ChatUtils.debug(
                    "SensitivityReducer: Fixed invalid reduction factor ($this -> $coerced)",
                )
            }
            MouseSensitivityManager.destroyCache()
        }
        config.enabled.afterChange { autoToggle() }
        config.onlyPlot.afterChange { autoToggle() }
        config.onGround.afterChange { autoToggle() }
    }

    private fun updatePlayerStatus() {
        val newInBarn = GardenApi.onUnfarmablePlot
        val newOnGround = PlayerUtils.onGround()

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
        val shouldReduce = config.mode.any {
            when (it) {
                SensitivityReducerConfig.Mode.TOOL -> isHoldingTool()
                SensitivityReducerConfig.Mode.FISHING_ROD -> isHoldingFishingRod()
                SensitivityReducerConfig.Mode.KEYBIND -> isHoldingKey()
                SensitivityReducerConfig.Mode.MOUSEMAT -> isHoldingMousemat()
            }
        }

        toggleIfCondition { shouldReduce }
    }

    private fun toggleIfCondition(check: () -> Boolean) {
        val conditionMet = check()
        if (conditionMet xor isActive) autoToggle()
    }

    private fun autoToggle() {
        if (!config.enabled.get()) {
            if (isActive) disable()
            return
        }
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
            ChatUtils.chat(
                "§bMouse sensitivity is now lowered. " +
                    "Type /shsensreduce to restore your sensitivity.",
            )
        } else {
            shouldBeActive = false
            MouseSensitivityManager.state = SensitivityState.UNCHANGED
            ChatUtils.chat("§bMouse sensitivity is now restored.")
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shsensreduce") {
            description = "Lowers the mouse sensitivity for easier small adjustments (for farming)"
            category = CommandCategory.USERS_ACTIVE
            simpleCallback(::manualToggle)
        }
    }

    @HandleEvent
    fun onGuiRenderOverlay() {
        if (!isActive) return
        if (!config.showGui) return
        config.position.renderRenderable(
            Renderable.text("§eSensitivity Lowered"),
            posLabel = "Sensitivity Lowered",
        )
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        val base = "garden.sensitivityReducer"
        event.move(80, "garden.sensitivityReducerConfig", base)
        event.move(81, "$base.showGUI", "$base.showGui")
        event.transform(116, "$base.mode") { element ->
            event.add(116, "$base.enabled") {
                JsonPrimitive(element.asString != "OFF")
            }
            val newList = JsonArray()
            when (element.asString) {
                "OFF" -> newList.add("TOOL")
                else -> newList.add(element.asString)
            }
            newList
        }
    }

    private fun isHoldingMousemat(): Boolean = GardenApi.itemInHand?.getInternalName() == SQUEAKY_MOUSEMAT
    private fun isHoldingTool(): Boolean = GardenApi.toolInHand != null
    private fun isHoldingFishingRod(): Boolean = FishingApi.holdingRod
    private fun isHoldingKey(): Boolean = config.keybind.isKeyHeld() && Minecraft.getInstance().screen == null
}
