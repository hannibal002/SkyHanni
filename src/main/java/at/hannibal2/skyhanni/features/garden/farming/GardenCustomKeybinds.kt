package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.Garden
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.mixins.transformers.AccessorKeyBinding
import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import java.util.*

object GardenCustomKeybinds {
    private val shConfig: Garden get() = SkyHanniMod.feature.garden
    private val mcSettings get() = Minecraft.getMinecraft().gameSettings

    private val map: MutableMap<KeyBinding, () -> Int> = IdentityHashMap()
    private val cache: MutableMap<Int, Boolean> = mutableMapOf()

    init {
        map[mcSettings.keyBindAttack] = { shConfig.keyBindAttack }
        map[mcSettings.keyBindUseItem] = { shConfig.keyBindUseItem }
        map[mcSettings.keyBindLeft] = { shConfig.keyBindLeft }
        map[mcSettings.keyBindRight] = { shConfig.keyBindRight }
        map[mcSettings.keyBindForward] = { shConfig.keyBindForward }
        map[mcSettings.keyBindBack] = { shConfig.keyBindBack }
        map[mcSettings.keyBindJump] = { shConfig.keyBindJump }
        map[mcSettings.keyBindSneak] = { shConfig.keyBindSneak }
    }

    private fun isEnabled() = GardenAPI.inGarden() && shConfig.keyBindEnabled

    private fun isActive() = isEnabled() && GardenAPI.toolInHand != null

    @SubscribeEvent
    fun onToolChange(event: GardenToolChangeEvent) {
        if (!isEnabled()) return
        map.forEach { (keyBinding, override) ->
            keyBinding as AccessorKeyBinding
            val keyCode = if (isActive()) override() else keyBinding.keyCode
            keyBinding.pressed_skyhanni = cache[keyCode] ?: false
        }
    }

    @JvmStatic
    fun onTick(keyCode: Int, ci: CallbackInfo) {
        if (keyCode == 0) return
        if (!isActive()) return
        val keyBinding = map.entries.firstOrNull { it.value() == keyCode }?.key ?: return
        ci.cancel()
        keyBinding as AccessorKeyBinding
        keyBinding.pressTime_skyhanni++
    }

    @JvmStatic
    fun setKeyBindState(keyCode: Int, pressed: Boolean, ci: CallbackInfo) {
        if (keyCode == 0) return
        cache[keyCode] = pressed
        if (!isActive()) return
        val keyBinding = map.entries.firstOrNull { it.value() == keyCode }?.key ?: return
        ci.cancel()
        keyBinding as AccessorKeyBinding
        keyBinding.pressed_skyhanni = pressed
    }
}