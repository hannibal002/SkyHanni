package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import io.github.notenoughupdates.moulconfig.observer.Property
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.SignEditScreen
import org.lwjgl.glfw.GLFW
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GardenCustomKeybinds {

    private val config get() = GardenApi.config.keyBind
    private val mcSettings get() = Minecraft.getInstance().options
    private val versionAllowsDuplicateKeybinds by lazy { PlatformUtils.isMcBelow("1.21.9") }

    private var map: Map<KeyMapping, Int> = emptyMap()
    private var lastWindowOpenTime = SimpleTimeMark.farPast()
    private var lastDuplicateKeybindsWarnTime = SimpleTimeMark.farPast()
    private var hasDisallowedDuplicateKeybinds = false

    @JvmStatic
    fun isKeyDown(keyBinding: KeyMapping, cir: CallbackInfoReturnable<Boolean>) {
        if (!isActive()) return
        val override = map[keyBinding] ?: run {
            if (map.containsValue(keyBinding.key.value)) {
                cir.returnValue = false
            }
            return
        }

        cir.returnValue = override.isKeyHeld()
    }

    @JvmStatic
    fun isKeyPressed(keyBinding: KeyMapping, cir: CallbackInfoReturnable<Boolean>) {
        if (!isActive()) return
        val override = map[keyBinding] ?: run {
            if (map.containsValue(keyBinding.key.value)) {
                cir.returnValue = false
            }
            return
        }
        cir.returnValue = override.isKeyClicked()
    }

    @HandleEvent
    fun onTick() {
        if (!isEnabled()) return
        val screen = Minecraft.getInstance().screen ?: return
        if (screen !is SignEditScreen) return
        lastWindowOpenTime = SimpleTimeMark.now()
    }

    @HandleEvent(SecondPassedEvent::class)
    fun onSecondPassed() {
        if (!isEnabled()) return
        if (!hasDisallowedDuplicateKeybinds || lastDuplicateKeybindsWarnTime.passedSince() < 30.seconds) return
        ChatUtils.chatAndOpenConfig(
            "Duplicate Custom Keybinds aren't allowed!",
            GardenApi.config::keyBind,
        )
        lastDuplicateKeybindsWarnTime = SimpleTimeMark.now()
    }

    @HandleEvent(ConfigLoadEvent::class)
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
        checkDuplicateKeybinds()
        lastDuplicateKeybindsWarnTime = SimpleTimeMark.farPast()
        KeyMapping.releaseAll()
    }

    private fun checkDuplicateKeybinds() {
        hasDisallowedDuplicateKeybinds = !versionAllowsDuplicateKeybinds &&
            map.values
                .filter { it != GLFW.GLFW_KEY_UNKNOWN }
                .let { values -> values.size != values.toSet().size }
    }

    private fun isEnabled(): Boolean =
        GardenApi.inGarden() &&
            config.enabled &&
            !(GardenApi.onUnfarmablePlot && config.excludeBarn)

    private fun isActive(): Boolean =
        isEnabled() &&
            GardenApi.toolInHand != null &&
            !hasDisallowedDuplicateKeybinds &&
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
