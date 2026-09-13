package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.features.fishing.FishingApi
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.pests.PestApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.InputCode
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import io.github.notenoughupdates.moulconfig.observer.Property
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.ToggleKeyMapping
import net.minecraft.client.gui.screens.inventory.SignEditScreen
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object GardenCustomKeybinds {

    private val config get() = GardenApi.config.keyBind
    private val mcSettings get() = Minecraft.getInstance().options

    private var map: Map<KeyMapping, InputCode> = emptyMap()
    private val pressedToggleKeys = mutableMapOf<KeyMapping, InputCode>()
    private var lastWindowOpenTime = SimpleTimeMark.farPast()
    private var wasActive = false

    @JvmStatic
    fun isKeyDown(keyBinding: KeyMapping, isDown: Boolean, cir: CallbackInfoReturnable<Boolean>) {
        if (!updateActiveState()) return
        val override = map[keyBinding] ?: run {
            if (map.contains(keyBinding)) {
                cir.returnValue = false
            }
            return
        }

        cir.returnValue = when {
            !keyBinding.isToggle() -> override.isKeyHeld()
            keyBinding.isRemappedFrom(override) -> keyBinding.updateToggleState(override, isDown)
            else -> isDown
        }
    }

    @JvmStatic
    fun isKeyPressed(keyBinding: KeyMapping, cir: CallbackInfoReturnable<Boolean>) {
        if (!updateActiveState()) return
        val override = map[keyBinding] ?: run {
            if (map.contains(keyBinding)) {
                cir.returnValue = false
            }
            return
        }
        cir.returnValue = if (keyBinding.isToggle() && keyBinding.isRemappedFrom(override)) {
            keyBinding.consumeToggleClick(override)
        } else {
            override.isKeyClicked()
        }
    }

    @HandleEvent
    private fun onTick() {
        if (!isEnabled()) return
        val screen = MinecraftCompat.screen ?: return
        if (screen !is SignEditScreen) return
        lastWindowOpenTime = SimpleTimeMark.now()
    }

    @HandleEvent
    private fun onConfigLoad() {
        with(config) {
            ConditionalUtils.onToggle(attack, useItem, left, right, forward, back, jump, sneak) {
                update()
            }
            update()
        }
    }

    private fun update() {
        pressedToggleKeys.clear()
        wasActive = false
        with(config) {
            with(mcSettings) {
                map = buildMap {
                    fun add(keyBinding: KeyMapping, property: Property<InputCode>) {
                        put(keyBinding, property.get())
                    }
                    add(keyAttack, attack)
                    add(keyUse, useItem)
                    add(keyLeft, left)
                    add(keyRight, right)
                    add(keyUp, forward)
                    add(keyDown, back)
                    add(keyJump, jump)
                    add(keyShift, sneak)
                }
            }
        }
        KeyMapping.releaseAll()
    }

    private fun updateActiveState(): Boolean {
        val active = isActive()
        if (wasActive == active) return active

        wasActive = active
        pressedToggleKeys.clear()
        if (active) primePressedToggleKeys()
        return active
    }

    private fun primePressedToggleKeys() {
        for ((keyBinding, override) in map) {
            if (keyBinding.isToggle() && keyBinding.isRemappedFrom(override) && override.isKeyHeld()) {
                pressedToggleKeys[keyBinding] = override
            }
        }
    }

    private fun KeyMapping.isToggle(): Boolean =
        this is ToggleKeyMapping && needsToggle.asBoolean

    private fun KeyMapping.isRemappedFrom(override: InputCode): Boolean =
        key.value != override.value

    private fun KeyMapping.updateToggleState(override: InputCode, isDown: Boolean): Boolean {
        if (!override.isKeyHeld()) {
            pressedToggleKeys.remove(this, override)
            return isDown
        }
        if (pressedToggleKeys[this] == override) return isDown

        pressedToggleKeys[this] = override
        setDown(true)
        return !isDown
    }


    private fun KeyMapping.consumeToggleClick(override: InputCode): Boolean {
        if (!override.isKeyHeld()) {
            pressedToggleKeys.remove(this, override)
            return false
        }
        if (pressedToggleKeys[this] == override) return false

        pressedToggleKeys[this] = override
        isDown = true
        return true
    }

    private fun isEnabled(): Boolean =
        GardenApi.inGarden() &&
            config.enabled &&
            !(GardenApi.onUnfarmablePlot && config.excludeBarn)

    private fun isHoldingTool(): Boolean =
        GardenApi.hasFarmingToolInHand() ||
            (config.mousemat && GardenApi.hasMousematInHand()) ||
            (config.vacuum && PestApi.hasVacuumInHand()) ||
            (config.fishingRod && FishingApi.holdingRod) ||
            (config.sunsGrasp && GardenApi.hasActiveSunsGrasp())

    private fun isActive(): Boolean =
        isEnabled() &&
            isHoldingTool() &&
            !hasGuiOpen() &&
            lastWindowOpenTime.passedSince() > 300.milliseconds

    private fun hasGuiOpen() = MinecraftCompat.screen != null

    private fun Map<KeyMapping, InputCode>.contains(value: KeyMapping): Boolean =
        values.any { it.value == value.key.value }

    @JvmStatic
    fun disableAll() {
        with(config) {
            attack.set(InputCode.UNKNOWN)
            useItem.set(InputCode.UNKNOWN)
            left.set(InputCode.UNKNOWN)
            right.set(InputCode.UNKNOWN)
            forward.set(InputCode.UNKNOWN)
            back.set(InputCode.UNKNOWN)
            jump.set(InputCode.UNKNOWN)
            sneak.set(InputCode.UNKNOWN)
        }
    }

    @JvmStatic
    fun defaultAll() {
        with(config) {
            attack.set(InputCode.LEFT_MOUSE)
            useItem.set(InputCode.RIGHT_MOUSE)
            left.set(InputCode.KEY_A)
            right.set(InputCode.KEY_D)
            forward.set(InputCode.KEY_W)
            back.set(InputCode.KEY_S)
            jump.set(InputCode.KEY_SPACE)
            sneak.set(InputCode.KEY_LSHIFT)
        }
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.keyBindEnabled", "garden.keyBind.enabled")
        event.move(3, "garden.keyBindAttack", "garden.keyBind.attack")
        event.move(3, "garden.keyBindUseItem", "garden.keyBind.useItem")
        event.move(3, "garden.keyBindLeft", "garden.keyBind.left")
        event.move(3, "garden.keyBindRight", "garden.keyBind.right")
        event.move(3, "garden.keyBindForward", "garden.keyBind.forward")
        event.move(3, "garden.keyBindBack", "garden.keyBind.back")
        event.move(3, "garden.keyBindJump", "garden.keyBind.jump")
        event.move(3, "garden.keyBindSneak", "garden.keyBind.sneak")
    }
}
