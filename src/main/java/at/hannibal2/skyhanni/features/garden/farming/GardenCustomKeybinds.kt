package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.mixins.transformers.AccessorKeyBinding
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.util.IdentityHashMap
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GardenCustomKeybinds {

    private val config get() = GardenAPI.config.keyBind
    private val mcSettings get() = Minecraft.getMinecraft().gameSettings

    private val map: MutableMap<KeyBinding, MutableMap<String, () -> Int>> = IdentityHashMap()
    private var currentCropName: String = ""
    private var lastWindowOpenTime = SimpleTimeMark.farPast()
    private var lastDuplicateKeybindsWarnTime = SimpleTimeMark.farPast()

    init {
        map[mcSettings.keyBindAttack] = mutableMapOf("wheat" to { config.wheat_attack })
        map[mcSettings.keyBindUseItem] = mutableMapOf("wheat" to { config.wheat_useItem })
        map[mcSettings.keyBindLeft] = mutableMapOf("wheat" to { config.wheat_left })
        map[mcSettings.keyBindRight] = mutableMapOf("wheat" to { config.wheat_right })
        map[mcSettings.keyBindForward] = mutableMapOf("wheat" to { config.wheat_forward })
        map[mcSettings.keyBindBack] = mutableMapOf("wheat" to { config.wheat_back })
        map[mcSettings.keyBindJump] = mutableMapOf("wheat" to { config.wheat_jump })
        map[mcSettings.keyBindSneak] = mutableMapOf("wheat" to { config.wheat_sneak })

        map[mcSettings.keyBindAttack]?.put("carrot") { config.carrot_attack }
        map[mcSettings.keyBindUseItem]?.put("carrot") { config.carrot_useItem }
        map[mcSettings.keyBindLeft]?.put("carrot") { config.carrot_left }
        map[mcSettings.keyBindRight]?.put("carrot") { config.carrot_right }
        map[mcSettings.keyBindForward]?.put("carrot") { config.carrot_forward }
        map[mcSettings.keyBindBack]?.put("carrot") { config.carrot_back }
        map[mcSettings.keyBindJump]?.put("carrot") { config.carrot_jump }
        map[mcSettings.keyBindSneak]?.put("carrot") { config.carrot_sneak }

        map[mcSettings.keyBindAttack]?.put("potato") { config.potato_attack }
        map[mcSettings.keyBindUseItem]?.put("potato") { config.potato_useItem }
        map[mcSettings.keyBindLeft]?.put("potato") { config.potato_left }
        map[mcSettings.keyBindRight]?.put("potato") { config.potato_right }
        map[mcSettings.keyBindForward]?.put("potato") { config.potato_forward }
        map[mcSettings.keyBindBack]?.put("potato") { config.potato_back }
        map[mcSettings.keyBindJump]?.put("potato") { config.potato_jump }
        map[mcSettings.keyBindSneak]?.put("potato") { config.potato_sneak }

        map[mcSettings.keyBindAttack]?.put("wart") { config.netherWart_attack }
        map[mcSettings.keyBindUseItem]?.put("wart") { config.netherWart_useItem }
        map[mcSettings.keyBindLeft]?.put("wart") { config.netherWart_left }
        map[mcSettings.keyBindRight]?.put("wart") { config.netherWart_right }
        map[mcSettings.keyBindForward]?.put("wart") { config.netherWart_forward }
        map[mcSettings.keyBindBack]?.put("wart") { config.netherWart_back }
        map[mcSettings.keyBindJump]?.put("wart") { config.netherWart_jump }
        map[mcSettings.keyBindSneak]?.put("wart") { config.netherWart_sneak }

        map[mcSettings.keyBindAttack]?.put("pumpkin") { config.pumpkin_attack }
        map[mcSettings.keyBindUseItem]?.put("pumpkin") { config.pumpkin_useItem }
        map[mcSettings.keyBindLeft]?.put("pumpkin") { config.pumpkin_left }
        map[mcSettings.keyBindRight]?.put("pumpkin") { config.pumpkin_right }
        map[mcSettings.keyBindForward]?.put("pumpkin") { config.pumpkin_forward }
        map[mcSettings.keyBindBack]?.put("pumpkin") { config.pumpkin_back }
        map[mcSettings.keyBindJump]?.put("pumpkin") { config.pumpkin_jump }
        map[mcSettings.keyBindSneak]?.put("pumpkin") { config.pumpkin_sneak }

        map[mcSettings.keyBindAttack]?.put("melon") { config.melon_attack }
        map[mcSettings.keyBindUseItem]?.put("melon") { config.melon_useItem }
        map[mcSettings.keyBindLeft]?.put("melon") { config.melon_left }
        map[mcSettings.keyBindRight]?.put("melon") { config.melon_right }
        map[mcSettings.keyBindForward]?.put("melon") { config.melon_forward }
        map[mcSettings.keyBindBack]?.put("melon") { config.melon_back }
        map[mcSettings.keyBindJump]?.put("melon") { config.melon_jump }
        map[mcSettings.keyBindSneak]?.put("melon") { config.melon_sneak }

        map[mcSettings.keyBindAttack]?.put("cocoa") { config.cocoaBeans_attack }
        map[mcSettings.keyBindUseItem]?.put("cocoa") { config.cocoaBeans_useItem }
        map[mcSettings.keyBindLeft]?.put("cocoa") { config.cocoaBeans_left }
        map[mcSettings.keyBindRight]?.put("cocoa") { config.cocoaBeans_right }
        map[mcSettings.keyBindForward]?.put("cocoa") { config.cocoaBeans_forward }
        map[mcSettings.keyBindBack]?.put("cocoa") { config.cocoaBeans_back }
        map[mcSettings.keyBindJump]?.put("cocoa") { config.cocoaBeans_jump }
        map[mcSettings.keyBindSneak]?.put("cocoa") { config.cocoaBeans_sneak }

        map[mcSettings.keyBindAttack]?.put("cane") { config.sugarCane_attack }
        map[mcSettings.keyBindUseItem]?.put("cane") { config.sugarCane_useItem }
        map[mcSettings.keyBindLeft]?.put("cane") { config.sugarCane_left }
        map[mcSettings.keyBindRight]?.put("cane") { config.sugarCane_right }
        map[mcSettings.keyBindForward]?.put("cane") { config.sugarCane_forward }
        map[mcSettings.keyBindBack]?.put("cane") { config.sugarCane_back }
        map[mcSettings.keyBindJump]?.put("cane") { config.sugarCane_jump }
        map[mcSettings.keyBindSneak]?.put("cane") { config.sugarCane_sneak }

        map[mcSettings.keyBindAttack]?.put("cactus") { config.cactus_attack }
        map[mcSettings.keyBindUseItem]?.put("cactus") { config.cactus_useItem }
        map[mcSettings.keyBindLeft]?.put("cactus") { config.cactus_left }
        map[mcSettings.keyBindRight]?.put("cactus") { config.cactus_right }
        map[mcSettings.keyBindForward]?.put("cactus") { config.cactus_forward }
        map[mcSettings.keyBindBack]?.put("cactus") { config.cactus_back }
        map[mcSettings.keyBindJump]?.put("cactus") { config.cactus_jump }
        map[mcSettings.keyBindSneak]?.put("cactus") { config.cactus_sneak }

        map[mcSettings.keyBindAttack]?.put("mushroom") { config.mushroom_attack }
        map[mcSettings.keyBindUseItem]?.put("mushroom") { config.mushroom_useItem }
        map[mcSettings.keyBindLeft]?.put("mushroom") { config.mushroom_left }
        map[mcSettings.keyBindRight]?.put("mushroom") { config.mushroom_right }
        map[mcSettings.keyBindForward]?.put("mushroom") { config.mushroom_forward }
        map[mcSettings.keyBindBack]?.put("mushroom") { config.mushroom_back }
        map[mcSettings.keyBindJump]?.put("mushroom") { config.mushroom_jump }
        map[mcSettings.keyBindSneak]?.put("mushroom") { config.mushroom_sneak }
    }

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
        val result: MutableMap<String, () -> Int> = map[keyBinding] ?: return
        val override = result[cropName] ?: return
        val keyCode = override()

        currentCropName = cropName

        cir.returnValue = keyCode.isKeyHeld()
    }

    // Cancel the CallbackInfo and increment the press time
    @JvmStatic
    fun onTick(keyCode: Int, ci: CallbackInfo) {
        if (!isActive()) return
        if (keyCode == 0) return

        for ((outerKey, innerMap) in map) {
            if (innerMap[currentCropName] == null) continue
            if (innerMap[currentCropName]!!() != keyCode) continue
            ci.cancel()
            outerKey as AccessorKeyBinding
            outerKey.pressTime_skyhanni++
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
