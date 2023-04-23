package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.Garden
import at.hannibal2.skyhanni.features.garden.GardenAPI
import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import org.lwjgl.input.Keyboard
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import kotlin.reflect.KProperty

class GardenCustomKeybinds {

//    private val cache = mutableMapOf<KeyBinding, Int>()
//    private val map = mutableMapOf<KeyBinding, () -> Int>()

//    init {
//        map[mcSettings.keyBindAttack] = { shConfig.keyBindAttack }
//        map[mcSettings.keyBindLeft] = { shConfig.keyBindLeft }
//        map[mcSettings.keyBindRight] = { shConfig.keyBindRight }
//        map[mcSettings.keyBindForward] = { shConfig.keyBindForward }
//        map[mcSettings.keyBindBack] = { shConfig.keyBindBack }
//        map[mcSettings.keyBindJump] = { shConfig.keyBindJump }
//        map[mcSettings.keyBindSneak] = { shConfig.keyBindSneak }

//        Runtime.getRuntime().addShutdownHook(Thread { reset() })
//    }

    @SubscribeEvent
    fun onKeyBindPressed(event: InputEvent.KeyInputEvent) {
        if (!isEnabled()) return

        ignoreCheck = true
        if (Keyboard.getEventKeyState()) {
            val key =
                if (Keyboard.getEventKey() == 0) Keyboard.getEventCharacter().code + 256 else Keyboard.getEventKey()

            out@ for ((mcBinding, skyHanniBinding) in map) {
                val description = mcBinding.keyDescription
                if (key == skyHanniBinding()) {
                    if (mcBinding.keyCode == key) {
                        val bindings = map.filter { it.value() == mcBinding.keyCode }.keys
                        if (bindings.size > 1) {
                            for (binding in bindings) {
                                if (bindings.contains(mcBinding)) {
                                    println("dupe: $description")
                                    currentState[mcBinding] = -1L
                                    continue@out
                                }
                            }
                        }
                    }

                    currentState[mcBinding] = System.currentTimeMillis()
//                    println("currentState set for $description")
                }
            }
        }
        ignoreCheck = false
    }

    companion object {
        private val shConfig: Garden get() = SkyHanniMod.feature.garden
        private val mcSettings get() = Minecraft.getMinecraft().gameSettings

        private val map = mutableMapOf<KeyBinding, () -> Int>()
        private val currentState = mutableMapOf<KeyBinding, Long>()
//        private var ignoreCheck = false


        var ignoreCheck by object : ThreadLocal<Boolean>()  {
            override fun initialValue(): Boolean {
                return false
            }
        }

        private operator fun <T> ThreadLocal<T>.setValue(t: Any?, property: KProperty<*>, any: T) {
            this.set(any)
        }

        private operator fun <T> ThreadLocal<T>.getValue(t: Any?, property: KProperty<*>): T {
            return get()
        }

        init {
            map[mcSettings.keyBindAttack] = { shConfig.keyBindAttack }
            map[mcSettings.keyBindLeft] = { shConfig.keyBindLeft }
            map[mcSettings.keyBindRight] = { shConfig.keyBindRight }
            map[mcSettings.keyBindForward] = { shConfig.keyBindForward }
            map[mcSettings.keyBindBack] = { shConfig.keyBindBack }
            map[mcSettings.keyBindJump] = { shConfig.keyBindJump }
            map[mcSettings.keyBindSneak] = { shConfig.keyBindSneak }

            for (mcBinding in map.keys) {
                currentState[mcBinding] = 0L
            }
        }

        fun isKeyDown(keyBinding: KeyBinding, cir: CallbackInfoReturnable<Boolean>) {
            checkKey(keyBinding, cir)
        }

        fun isPressed(keyBinding: KeyBinding, cir: CallbackInfoReturnable<Boolean>) {
            if (!isEnabled()) return
//            val returnValue = cir.returnValue ?: return
//            if (returnValue) {
//                if (Minecraft.getMinecraft().thePlayer.isSneaking) {
//                    println("isPressed: ${keyBinding.keyDescription}")
//                }
//            }
            if (ignoreCheck) {
                println("ignored check")
                return
            } else {
                println("not ignored check")
            }
            checkKey(keyBinding, cir)
        }

        private fun checkKey(keyBinding: KeyBinding, cir: CallbackInfoReturnable<Boolean>) {
            if (!isEnabled()) return
            val time = currentState[keyBinding] ?: return

            if (time == -1L) {
                cir.returnValue = false
                return
            }

            val keyDescription = keyBinding.keyDescription
            if (time != 0L) {
                val diff = System.currentTimeMillis() - time
                val result = diff < 50

                if (result) {
                    println("clicking $keyDescription")
                    cir.returnValue = true
                }

            }
        }

        private fun isEnabled() = GardenAPI.inGarden() && shConfig.keyBindEnabled && GardenAPI.toolInHand != null
    }

//    @SubscribeEvent
//    fun onGardenToolChange(event: GardenToolChangeEvent) {
//        update()
//    }

//    private fun update() {
//        if (isEnabled() && GardenAPI.toolInHand != null) {
//            applyCustomKeybinds()
//        } else {
//            reset()
//        }
//    }

//    @SubscribeEvent
//    fun onWorldChange(event: WorldEvent.Load) {
//        reset()
//    }

//    private fun applyCustomKeybinds() {
//        val alreadyBoundedKeys = mutableListOf<Int>()
//        var counter = 0
//        for ((mcBinding, skyHanniBinding) in map) {
//            val newKeyCode = skyHanniBinding()
//            if (newKeyCode == mcBinding.keyCode) continue
//
//            disableAlreadyExistingKeybinds(newKeyCode, alreadyBoundedKeys)
//
//            if (!cache.containsKey(mcBinding)) {
//                cache[mcBinding] = mcBinding.keyCode
//                mcBinding.unpressKeyIfDown()
//            }
//
//            mcBinding.keyCode = newKeyCode
//            alreadyBoundedKeys.add(mcBinding.keyCodeDefault)
//            counter++
//        }
//
//        if (counter > 0) {
//            KeyBinding.resetKeyBindingArrayAndHash()
//        }
//    }

//    private fun disableAlreadyExistingKeybinds(newKeyCode: Int, alreadyBoundedKeys: MutableList<Int>) {
//        if (newKeyCode == 0) return
//        for (keyBinding in mcSettings.keyBindings) {
//            if (keyBinding.keyCode != newKeyCode) continue
//            if (alreadyBoundedKeys.contains(keyBinding.keyCodeDefault)) continue
//            keyBinding.unpressKeyIfDown()
//            cache[keyBinding] = keyBinding.keyCode
//            keyBinding.keyCode = 0
//        }
//    }

//    private fun reset() {
//        var counter = 0
//        for ((key, keyCode) in cache) {
//            if (key.keyCode != keyCode) {
//                key.unpressKeyIfDown()
//                counter++
//                key.keyCode = keyCode
//            }
//        }
//        cache.clear()
//        if (counter > 0) {
//            KeyBinding.resetKeyBindingArrayAndHash()
//        }
//    }

//    private fun KeyBinding.unpressKeyIfDown() {
//        try {
//
//            if (KeybindHelper.isKeyDown(keyCode)) {
//                (this as AccessorKeyBinding).skyhanni_unpressKey()
//            }
//        } catch (_: IllegalStateException) {
//        }
//    }
}
