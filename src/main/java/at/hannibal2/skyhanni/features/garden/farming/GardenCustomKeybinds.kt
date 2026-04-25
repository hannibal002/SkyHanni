package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.features.fishing.FishingApi.isFishingRod
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.GardenApi.isFarmingTool
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import com.mojang.blaze3d.platform.InputConstants
import io.github.notenoughupdates.moulconfig.observer.Property
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.SignEditScreen
import org.lwjgl.glfw.GLFW
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object GardenCustomKeybinds {

    private val SQUEAKY_MOUSEMAT = "SQUEAKY_MOUSEMAT".toInternalName()

    private val config get() = GardenApi.config.keyBind
    private val mcSettings get() = Minecraft.getInstance().options

    private var map: Map<KeyMapping, Int> = emptyMap()
    private var lastWindowOpenTime = SimpleTimeMark.farPast()

    @JvmStatic
    fun shouldCancelKeyInput(key: InputConstants.Key, pressed: Boolean): Boolean {
        if (!isActive()) return false
        var handled = false
        for ((keyBinding, override) in map) {
            if (override == keyBinding.key.value) continue
            if (override == GLFW.GLFW_KEY_UNKNOWN) {
                if (key.value == keyBinding.key.value) {
                    handled = true
                }
                continue
            }
            if (key.value == override) {
                keyBinding.isDown = pressed
                handled = true
                continue
            }
            if (key.value == keyBinding.key.value) {
                handled = true
            }
        }
        return handled
    }

    @JvmStatic
    fun shouldCancelKeyClick(key: InputConstants.Key): Boolean {
        if (!isActive()) return false
        for ((keyBinding, override) in map) {
            if (override == keyBinding.key.value) continue
            if (key.value == keyBinding.key.value) return true
            if (override != GLFW.GLFW_KEY_UNKNOWN && key.value == override) return true
        }
        return false
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

    private fun isEnabled(): Boolean =
        GardenApi.inGarden() &&
            config.enabled &&
            !(GardenApi.onUnfarmablePlot && config.excludeBarn)

    private fun isHoldingTool() = InventoryUtils.getItemInHand()?.getInternalNameOrNull()?.let { heldItem ->
        heldItem.isFarmingTool() ||
            (config.mousemat && heldItem == SQUEAKY_MOUSEMAT) ||
            (config.fishingRod && heldItem.isFishingRod())
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
