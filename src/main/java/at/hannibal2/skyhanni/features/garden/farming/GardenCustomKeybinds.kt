package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.features.fishing.FishingApi.isFishingRod
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.GardenApi.isFarmingTool
import at.hannibal2.skyhanni.features.garden.pests.PestApi
import at.hannibal2.skyhanni.features.inventory.EquipmentApi
import at.hannibal2.skyhanni.features.inventory.EquipmentSlot
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import io.github.notenoughupdates.moulconfig.observer.Property
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.ToggleKeyMapping
import net.minecraft.client.gui.screens.inventory.SignEditScreen
import net.minecraft.world.item.Items
import org.lwjgl.glfw.GLFW
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object GardenCustomKeybinds {

    private val SQUEAKY_MOUSEMAT = "SQUEAKY_MOUSEMAT".toInternalName()
    private val SUNS_GRASP = "SUNS_GRASP".toInternalName()

    private val config get() = GardenApi.config.keyBind
    private val mcSettings get() = Minecraft.getInstance().options

    private var map: Map<KeyMapping, Int> = emptyMap()
    private val pressedToggleKeys = mutableMapOf<KeyMapping, Int>()
    private var lastWindowOpenTime = SimpleTimeMark.farPast()
    private var wasActive = false

    @JvmStatic
    fun isKeyDown(keyBinding: KeyMapping, isDown: Boolean, cir: CallbackInfoReturnable<Boolean>) {
        if (!updateActiveState()) return
        val override = map[keyBinding] ?: run {
            if (map.containsValue(keyBinding.key.value)) {
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
            if (map.containsValue(keyBinding.key.value)) {
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
    fun onTick() {
        if (!isEnabled()) return
        val screen = Minecraft.getInstance().screen ?: return
        if (screen !is SignEditScreen) return
        lastWindowOpenTime = SimpleTimeMark.now()
    }

    @HandleEvent
    fun onConfigLoad() {
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
                    fun add(keyBinding: KeyMapping, property: Property<Int>) {
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
        this is ToggleKeyMapping && needsToggle.getAsBoolean()

    private fun KeyMapping.isRemappedFrom(override: Int): Boolean =
        key.value != override

    private fun KeyMapping.updateToggleState(override: Int, isDown: Boolean): Boolean {
        if (!override.isKeyHeld()) {
            pressedToggleKeys.remove(this, override)
            return isDown
        }
        if (pressedToggleKeys[this] == override) return isDown

        pressedToggleKeys[this] = override
        setDown(true)
        return !isDown
    }

    private fun KeyMapping.consumeToggleClick(override: Int): Boolean {
        if (!override.isKeyHeld()) {
            pressedToggleKeys.remove(this, override)
            return false
        }
        if (pressedToggleKeys[this] == override) return false

        pressedToggleKeys[this] = override
        setDown(true)
        return true
    }

    private fun isEnabled(): Boolean =
        GardenApi.inGarden() &&
            config.enabled &&
            !(GardenApi.onUnfarmablePlot && config.excludeBarn)

    private fun isHoldingTool(): Boolean = InventoryUtils.getItemInHand()?.let { heldItem ->
        val internalName = heldItem.getInternalName()

        val wearingSunsGrasp = EquipmentApi.getEquipment(EquipmentSlot.GLOVES)?.getInternalName() == SUNS_GRASP

        return internalName.isFarmingTool() ||
            (config.mousemat && internalName == SQUEAKY_MOUSEMAT) ||
            (config.vacuum && PestApi.hasVacuumInHand()) ||
            (config.fishingRod && internalName.isFishingRod()) ||
            (config.sunsGrasp && wearingSunsGrasp && heldItem.isEmpty)
    } ?: false

    private fun isActive(): Boolean =
        isEnabled() &&
            isHoldingTool() &&
            !hasGuiOpen() &&
            lastWindowOpenTime.passedSince() > 300.milliseconds

    private fun hasGuiOpen() = Minecraft.getInstance().screen != null

    @JvmStatic
    fun disableAll() {
        with(config) {
            attack.set(GLFW.GLFW_KEY_UNKNOWN)
            useItem.set(GLFW.GLFW_KEY_UNKNOWN)
            left.set(GLFW.GLFW_KEY_UNKNOWN)
            right.set(GLFW.GLFW_KEY_UNKNOWN)
            forward.set(GLFW.GLFW_KEY_UNKNOWN)
            back.set(GLFW.GLFW_KEY_UNKNOWN)
            jump.set(GLFW.GLFW_KEY_UNKNOWN)
            sneak.set(GLFW.GLFW_KEY_UNKNOWN)
        }
    }

    @JvmStatic
    fun defaultAll() {
        with(config) {
            attack.set(KeyboardManager.LEFT_MOUSE)
            useItem.set(KeyboardManager.RIGHT_MOUSE)
            left.set(GLFW.GLFW_KEY_A)
            right.set(GLFW.GLFW_KEY_D)
            forward.set(GLFW.GLFW_KEY_W)
            back.set(GLFW.GLFW_KEY_S)
            jump.set(GLFW.GLFW_KEY_SPACE)
            sneak.set(GLFW.GLFW_KEY_LEFT_SHIFT)
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
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
