package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.mixins.transformers.AccessorKeyBinding
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.util.IdentityHashMap

object GardenCustomKeybinds {
    private val shConfig get() = SkyHanniMod.feature.garden.keyBind
    private val mcSettings get() = Minecraft.getMinecraft().gameSettings

    private val map: MutableMap<KeyBinding, () -> Int> = IdentityHashMap()
    private var lastWindowOpenTime = 0L

    init {
        map[mcSettings.keyBindAttack] = { shConfig.attack }
        map[mcSettings.keyBindUseItem] = { shConfig.useItem }
        map[mcSettings.keyBindLeft] = { shConfig.left }
        map[mcSettings.keyBindRight] = { shConfig.right }
        map[mcSettings.keyBindForward] = { shConfig.forward }
        map[mcSettings.keyBindBack] = { shConfig.back }
        map[mcSettings.keyBindJump] = { shConfig.jump }
        map[mcSettings.keyBindSneak] = { shConfig.sneak }
    }

    private fun isEnabled() = GardenAPI.inGarden() && shConfig.enabled

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
        cir.returnValue = keyCode.isKeyHeld()
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

    @SubscribeEvent
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