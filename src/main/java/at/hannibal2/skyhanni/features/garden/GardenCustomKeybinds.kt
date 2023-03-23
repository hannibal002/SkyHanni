package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.KeybindHelper
import at.hannibal2.skyhanni.config.features.Garden
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.mixins.transformers.AccessorKeyBinding
import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenCustomKeybinds {
    private val shConfig: Garden get() = SkyHanniMod.feature.garden
    private val mcSettings get() = Minecraft.getMinecraft().gameSettings

    private val cache = mutableMapOf<KeyBinding, Int>()
    private val map = mutableMapOf<KeyBinding, () -> Int>()

    init {
        map[mcSettings.keyBindAttack] = { shConfig.keyBindAttack }
        map[mcSettings.keyBindLeft] = { shConfig.keyBindLeft }
        map[mcSettings.keyBindRight] = { shConfig.keyBindRight }
        map[mcSettings.keyBindForward] = { shConfig.keyBindForward }
        map[mcSettings.keyBindBack] = { shConfig.keyBindBack }
        map[mcSettings.keyBindJump] = { shConfig.keyBindJump }
        map[mcSettings.keyBindSneak] = { shConfig.keyBindSneak }

        Runtime.getRuntime().addShutdownHook(Thread { reset() })
    }

    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        update()
    }

    private fun update() {
        if (isEnabled() && GardenAPI.toolInHand != null) {
            applyCustomKeybinds()
        } else {
            reset()
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        reset()
    }

    private fun applyCustomKeybinds() {
        val alreadyBoundedKeys = mutableListOf<Int>()
        var counter = 0
        for ((mcBinding, skyHanniBinding) in map) {
            val newKeyCode = skyHanniBinding()
            if (newKeyCode == mcBinding.keyCode) continue

            disableAlreadyExistingKeybinds(newKeyCode, alreadyBoundedKeys)

            if (!cache.containsKey(mcBinding)) {
                cache[mcBinding] = mcBinding.keyCode
                mcBinding.unpressKeyIfDown()
            }

            mcBinding.keyCode = newKeyCode
            alreadyBoundedKeys.add(mcBinding.keyCodeDefault)
            counter++
        }

        if (counter > 0) {
            KeyBinding.resetKeyBindingArrayAndHash()
        }
    }

    private fun disableAlreadyExistingKeybinds(newKeyCode: Int, alreadyBoundedKeys: MutableList<Int>) {
        if (newKeyCode == 0) return
        for (keyBinding in mcSettings.keyBindings) {
            if (keyBinding.keyCode != newKeyCode) continue
            if (alreadyBoundedKeys.contains(keyBinding.keyCodeDefault)) continue
            keyBinding.unpressKeyIfDown()
            cache[keyBinding] = keyBinding.keyCode
            keyBinding.keyCode = 0
        }
    }

    private fun reset() {
        var counter = 0
        for ((key, keyCode) in cache) {
            if (key.keyCode != keyCode) {
                key.unpressKeyIfDown()
                counter++
                key.keyCode = keyCode
            }
        }
        cache.clear()
        if (counter > 0) {
            KeyBinding.resetKeyBindingArrayAndHash()
        }
    }

    private fun KeyBinding.unpressKeyIfDown() {
        if (KeybindHelper.isKeyDown(keyCode)) {
            (this as AccessorKeyBinding).skyhanni_unpressKey()
        }
    }

    private fun isEnabled() = GardenAPI.inGarden() && shConfig.keyBindEnabled
}
