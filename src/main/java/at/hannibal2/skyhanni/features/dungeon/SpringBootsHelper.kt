package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object SpringBootsHelper {

    private var index = 0

    @SubscribeEvent
    fun onSound(event: PlaySoundEvent) {
        if (!(event.soundName == "note.pling" || event.soundName == "note.hat" || event.soundName == "fireworks.launch")) return
        if (InventoryUtils.getBoots()?.getInternalName()?.asString() != "SPRING_BOOTS") return
        if (event.soundName == "fireworks.launch") {
            index = 0
            println("reset index")
            return
        }
        if (!Minecraft.getMinecraft().thePlayer.isSneaking) return
        index += 1
        if (index == 15) {
            KeyBinding.setKeyBindState(Minecraft.getMinecraft().gameSettings.keyBindSneak.keyCode, false)
            println("unsneaked")

        }
        println("index = $index")
    }
}
