package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.render.gui.GuiScreenOpenEvent
import at.hannibal2.skyhanni.features.fishing.FishingApi.isFishingRod
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.GardenApi.isFarmingTool
import at.hannibal2.skyhanni.features.inventory.EquipmentApi
import at.hannibal2.skyhanni.features.inventory.EquipmentSlot
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.MouseCompat
import com.mojang.blaze3d.platform.InputConstants
import io.github.notenoughupdates.moulconfig.observer.Property
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.ToggleKeyMapping
import net.minecraft.client.gui.screens.inventory.SignEditScreen
import net.minecraft.world.item.Items
import org.lwjgl.glfw.GLFW
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object GardenCustomKeybinds {

    private val SQUEAKY_MOUSEMAT = "SQUEAKY_MOUSEMAT".toInternalName()
    private val SUNS_GRASP = "SUNS_GRASP".toInternalName()

    private val config get() = GardenApi.config.keyBind
    private val mcSettings get() = Minecraft.getInstance().options

    private var map: Map<KeyMapping, InputConstants.Key> = emptyMap()
    private val originalKeys = mutableMapOf<KeyMapping, InputConstants.Key>()
    private var lastWindowOpenTime = SimpleTimeMark.farPast()
    private var wasActive = false
    private var mappingsApplied = false
    private var refreshStateOnNextApply = true

    @JvmStatic
    fun originalKeyName(keyBinding: KeyMapping): String? =
        originalKeys[keyBinding]?.name

    @JvmStatic
    fun onMouseGrabRestoringKeyState() {
        if (!isActive()) return
        wasActive = true
        applyMappings(refreshState = false)
        refreshStateOnNextApply = true
    }

    @HandleEvent
    fun onGuiOpen(event: GuiScreenOpenEvent) {
        if (event.gui != null) {
            val wasUsingCustomMappings = mappingsApplied
            restoreMappings(refreshState = false)
            wasActive = false
            if (wasUsingCustomMappings) {
                refreshStateOnNextApply = false
            }
        }
    }

    @HandleEvent
    fun onTick() {
        if (isEnabled()) {
            val screen = Minecraft.getInstance().screen
            if (screen is SignEditScreen) {
                lastWindowOpenTime = SimpleTimeMark.now()
            }
        }
        updateActiveState()
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
        val active = mappingsApplied
        restoreMappings(refreshState = false)
        with(config) {
            with(mcSettings) {
                map = buildMap {
                    fun add(keyBinding: KeyMapping, property: Property<Int>) {
                        put(keyBinding, property.get().toInputKey())
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
        if (active) applyMappings()
    }

    private fun updateActiveState(): Boolean {
        val active = isActive()
        if (wasActive == active) return active

        wasActive = active
        if (active) {
            applyMappings(refreshState = refreshStateOnNextApply)
            refreshStateOnNextApply = true
        } else {
            restoreMappings()
        }
        return active
    }

    private fun applyMappings(refreshState: Boolean = true) {
        if (mappingsApplied) return
        for ((keyBinding, override) in map) {
            originalKeys[keyBinding] = keyBinding.key
            keyBinding.setKey(override)
        }
        mappingsApplied = true
        KeyMapping.resetMapping()
        if (refreshState) refreshState(map.keys)
    }

    private fun restoreMappings(refreshState: Boolean = true) {
        if (!mappingsApplied) return
        val affectedKeys = originalKeys.keys.toList()
        for ((keyBinding, originalKey) in originalKeys) {
            keyBinding.setKey(originalKey)
        }
        mappingsApplied = false
        originalKeys.clear()
        KeyMapping.resetMapping()
        if (refreshState) refreshState(affectedKeys)
    }

    private fun refreshState(keyBindings: Iterable<KeyMapping>) {
        for (keyBinding in keyBindings) {
            if (keyBinding.isToggle()) continue
            keyBinding.isDown = keyBinding.key.isDown()
        }
    }

    private fun KeyMapping.isToggle(): Boolean = this is ToggleKeyMapping && needsToggle.asBoolean

    private fun InputConstants.Key.isDown(): Boolean = when (type) {
        InputConstants.Type.KEYSYM -> InputConstants.isKeyDown(Minecraft.getInstance().window, value)
        InputConstants.Type.MOUSE -> MouseCompat.isButtonDown(value)
        else -> false
    }

    private fun Int.toInputKey(): InputConstants.Key = when {
        this == GLFW.GLFW_KEY_UNKNOWN -> InputConstants.UNKNOWN
        this in 0..5 -> InputConstants.Type.MOUSE.getOrCreate(this)
        else -> InputConstants.Type.KEYSYM.getOrCreate(this)
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
            (config.fishingRod && internalName.isFishingRod()) ||
            // TODO confirm why we check for item air here. getItemInHand should return null if there is no item there.
            (config.sunsGrasp && wearingSunsGrasp && heldItem.item == Items.AIR)
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
