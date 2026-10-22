package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.features.fishing.FishingApi
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.pests.PestApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.InputKeys
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import com.mojang.blaze3d.platform.InputConstants
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

    private var map: Map<KeyMapping, InputConstants.Key> = emptyMap()
    private val pressedToggleKeys = mutableMapOf<KeyMapping, InputConstants.Key>()
    private var lastWindowOpenTime = SimpleTimeMark.farPast()
    private var wasActive = false

    @JvmStatic
    fun isKeyDown(keyBinding: KeyMapping, isDown: Boolean, cir: CallbackInfoReturnable<Boolean>) {
        if (!updateActiveState()) return
        val override = map[keyBinding] ?: run {
            if (map.isUsedAsOverride(keyBinding)) {
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
            if (map.isUsedAsOverride(keyBinding)) {
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
                    fun add(keyBinding: KeyMapping, property: Property<InputConstants.Key>) {
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

    private fun KeyMapping.isRemappedFrom(override: InputConstants.Key): Boolean =
        key != override

    private fun KeyMapping.updateToggleState(override: InputConstants.Key, isDown: Boolean): Boolean {
        if (!override.isKeyHeld()) {
            pressedToggleKeys.remove(this, override)
            return isDown
        }
        if (pressedToggleKeys[this] == override) return isDown

        pressedToggleKeys[this] = override
        setDown(true)
        return !isDown
    }

    private fun KeyMapping.consumeToggleClick(override: InputConstants.Key): Boolean {
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

    private fun Map<KeyMapping, InputConstants.Key>.isUsedAsOverride(value: KeyMapping): Boolean =
        values.any { it == value.key }

    @JvmStatic
    fun disableAll() {
        with(config) {
            attack.set(InputConstants.UNKNOWN)
            useItem.set(InputConstants.UNKNOWN)
            left.set(InputConstants.UNKNOWN)
            right.set(InputConstants.UNKNOWN)
            forward.set(InputConstants.UNKNOWN)
            back.set(InputConstants.UNKNOWN)
            jump.set(InputConstants.UNKNOWN)
            sneak.set(InputConstants.UNKNOWN)
        }
    }

    @JvmStatic
    fun defaultAll() {
        with(config) {
            attack.set(InputKeys.MOUSE_BUTTON_LEFT)
            useItem.set(InputKeys.MOUSE_BUTTON_RIGHT)
            left.set(InputKeys.KEY_A)
            right.set(InputKeys.KEY_D)
            forward.set(InputKeys.KEY_W)
            back.set(InputKeys.KEY_S)
            jump.set(InputKeys.KEY_SPACE)
            sneak.set(InputKeys.KEY_LSHIFT)
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
