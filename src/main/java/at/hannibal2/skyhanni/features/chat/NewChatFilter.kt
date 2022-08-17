package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class NewChatFilter {

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.isOnHypixel) return

//        val blockReason = block(event.message)
//        if (blockReason != "") {
//            event.blockedReason = blockReason
//        }
    }
}