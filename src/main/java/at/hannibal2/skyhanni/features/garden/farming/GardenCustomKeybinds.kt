package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.mixins.transformers.AccessorKeyBinding
import at.hannibal2.skyhanni.utils.OSUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.client.settings.KeyBinding
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.util.IdentityHashMap

object GardenCustomKeybinds {
    private val shConfig get() = SkyHanniMod.feature.garden
    private val mcSettings get() = Minecraft.getMinecraft().gameSettings

    private val map: MutableMap<KeyBinding, () -> Int> = IdentityHashMap()
    private var lastWindowOpenTime = 0L

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

    private fun isActive(): Boolean {
        if (!isEnabled()) return false
        if (GardenAPI.toolInHand == null) return false

        if (Minecraft.getMinecraft().currentScreen != null) {
            if (Minecraft.getMinecraft().currentScreen is GuiEditSign) {
                lastWindowOpenTime = System.currentTimeMillis()
            }
            return false
        }

        // TODO remove workaround
        if (System.currentTimeMillis() < lastWindowOpenTime + 300) return false

        return true
    }

    @JvmStatic
    fun isKeyDown(keyBinding: KeyBinding, cir: CallbackInfoReturnable<Boolean>) {
        if (!isActive()) return
        val override = map[keyBinding] ?: return
        val keyCode = override()
        cir.returnValue = OSUtils.isKeyHeld(keyCode)
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
}