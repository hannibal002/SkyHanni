package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object QuiverNotification {
    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!SkyHanniMod.configManager.features.inventory.quiverAlert) return
        val message = event.message.removeColor()
        if (message.startsWith("You only have") && message.endsWith("arrows left in your Quiver!")) {
            val number = message.split(" ")[3]
            TitleManager.sendTitle("§c$number arrows left!", 3.seconds, 3.6, 7.0)
            sound()
        }
    }

    fun sound() {
        CoroutineScope(Dispatchers.Default).launch {
            repeat(30) {
                delay(100)
                SoundUtils.createSound("note.pling", 1f, 1f).playSound()
            }
        }
    }
}
