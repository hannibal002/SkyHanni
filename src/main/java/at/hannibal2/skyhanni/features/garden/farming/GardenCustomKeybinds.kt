package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.Garden
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.mixins.transformers.AccessorKeyBinding
import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.util.*

object GardenCustomKeybinds {
    private val shConfig: Garden get() = SkyHanniMod.feature.garden
    private val mcSettings get() = Minecraft.getMinecraft().gameSettings

    private val map: MutableMap<KeyBinding, () -> Int> = IdentityHashMap()

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
    fun onToolChange(event: GardenToolChangeEvent){
        KeyBinding.unPressAllKeys()
    }



    @JvmStatic
    fun onTick(keyCode: Int, ci: CallbackInfo) {
        if (!isActive()) return
        if (keyCode == 0) return
        val keyBinding = map.entries.firstOrNull { it.value() == keyCode }?.key ?: return
        ci.cancel()
        keyBinding as AccessorKeyBinding
        keyBinding.pressTime_skyhanni++
    }

    @JvmStatic
    fun setKeyBindState(keyCode: Int, pressed: Boolean, ci: CallbackInfo) {
        if (!isActive()) return
        if (keyCode == 0) return
        val keyBinding = map.entries.firstOrNull { it.value() == keyCode }?.key ?: return
        ci.cancel()
        keyBinding as AccessorKeyBinding
        keyBinding.pressed_skyhanni = pressed
    }
}