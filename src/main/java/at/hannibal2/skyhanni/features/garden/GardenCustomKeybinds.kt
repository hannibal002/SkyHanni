package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.garden.customkeybinds.KeyBindCrop
import at.hannibal2.skyhanni.mixins.transformers.AccessorKeyBinding
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.util.IdentityHashMap
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object GardenCustomKeybinds {

    private val config get() = GardenAPI.config.keyBind
    private val mcSettings get() = Minecraft.getMinecraft().gameSettings

    private val map: MutableMap<KeyBinding, () -> Int> = IdentityHashMap()
    private var currentCropName: String = ""
    private var lastWindowOpenTime = SimpleTimeMark.farPast()
    private var lastDuplicateKeybindsWarnTime = SimpleTimeMark.farPast()


    private fun isEnabled() = GardenAPI.inGarden() && config.enabled && !(GardenAPI.onBarnPlot && config.excludeBarn)

    private fun isActive(): Boolean {
        if (!isEnabled()) return false
        if (GardenAPI.toolInHand == null) return false

        if (Minecraft.getMinecraft().currentScreen != null) {
            if (Minecraft.getMinecraft().currentScreen is GuiEditSign) {
                lastWindowOpenTime = SimpleTimeMark.now()
            }
            return false
        }

        // TODO remove workaround
        if (lastWindowOpenTime.passedSince() < 300.milliseconds) return false

        // TODO re-enable this check
//         val areDuplicates = map.values
//             .map { it() }
//             .filter { it != Keyboard.KEY_NONE }
//             .let { values -> values.size != values.toSet().size }
//         if (areDuplicates) {
//             if (lastDuplicateKeybindsWarnTime.passedSince() > 30.seconds) {
//                 ChatUtils.chatAndOpenConfig(
//                     "Duplicate Custom Keybinds aren't allowed!",
//                     GardenAPI.config::keyBind
//                 )
//                 lastDuplicateKeybindsWarnTime = SimpleTimeMark.now()
//             }
//             return false
//         }

        return true
    }

    @JvmStatic
    fun isKeyDown(keyBinding: KeyBinding, cir: CallbackInfoReturnable<Boolean>) {
        if (!isActive()) return
        val cropInHand: CropType = GardenAPI.cropInHand ?: return
        val cropName: String = cropInHand.simpleName
        println(cropName)

        // Reset all keybinds to default
        val clazz: Class<*> = config.keyBind.javaClass

        // Get all public fields of the class
        val fields = clazz.fields

        // Iterate through each field
        for (field in fields) {
            if (field.type == KeyBindCrop::class.java) {
                try {
                    val cropClassInstance = field[config.keyBind] as KeyBindCrop
//                     if (cropClassInstance.cropType == cropName) {
//                         map[mcSettings.keyBindAttack] = { cropClassInstance.attack }
//                         map[mcSettings.keyBindUseItem] = { cropClassInstance.useItem }
//                         map[mcSettings.keyBindLeft] = { cropClassInstance.left }
//                         map[mcSettings.keyBindRight] = { cropClassInstance.right }
//                         map[mcSettings.keyBindForward] = { cropClassInstance.forward }
//                         map[mcSettings.keyBindBack] = { cropClassInstance.back }
//                         map[mcSettings.keyBindJump] = { cropClassInstance.jump }
//                         map[mcSettings.keyBindSneak] = { cropClassInstance.sneak }
//
//                         val override = map[keyBinding] ?: return
//
//                         val keyCode = override()
//
//                         currentCropName = cropName
//
//                         cir.returnValue = keyCode.isKeyHeld()

//                         return
//                     }
                } catch (e: IllegalAccessException) {
                    // pass
                }
            }
        }

    }

    // Cancel the CallbackInfo and increment the press time
    @JvmStatic
    fun onTick(keyCode: Int, ci: CallbackInfo) {
        if (!isActive()) return
        if (keyCode == 0) return

        // Reset all keybinds to default
        val clazz: Class<*> = config.keyBind.javaClass

        // Get all public fields of the class
        val fields = clazz.fields

        // Iterate through each field
        for (field in fields) {
            if (field.type == KeyBindCrop::class.java) {
                try {
                    val cropClassInstance = field[config.keyBind] as KeyBindCrop
//                     if (cropClassInstance.cropType == currentCropName) {
//                         map[mcSettings.keyBindAttack] = { cropClassInstance.attack }
//                         map[mcSettings.keyBindUseItem] = { cropClassInstance.useItem }
//                         map[mcSettings.keyBindLeft] = { cropClassInstance.left }
//                         map[mcSettings.keyBindRight] = { cropClassInstance.right }
//                         map[mcSettings.keyBindForward] = { cropClassInstance.forward }
//                         map[mcSettings.keyBindBack] = { cropClassInstance.back }
//                         map[mcSettings.keyBindJump] = { cropClassInstance.jump }
//                         map[mcSettings.keyBindSneak] = { cropClassInstance.sneak }
//
//                         for ((outerKey, keyPress) in map) {
//                             if (keyPress() != keyCode) continue
//                             ci.cancel()
//                             outerKey as AccessorKeyBinding
//                             outerKey.pressTime_skyhanni++
//                         }

//                         return
//                     }
                } catch (e: IllegalAccessException) {
                    // pass
                }
            }
        }
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
