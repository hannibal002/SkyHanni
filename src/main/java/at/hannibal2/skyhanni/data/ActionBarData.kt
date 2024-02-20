package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.ActionBarUpdateEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ActionBarData {
    private var actionBar = ""

    fun getActionBar() = actionBar

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        actionBar = ""
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onChatReceive(event: ClientChatReceivedEvent) {
        if (event.type.toInt() != 2) return

        val original = event.message
        val message = LorenzUtils.stripVanillaMessage(original.formattedText)
        if (message == actionBar) return
        actionBar = message
        ActionBarUpdateEvent(actionBar).postAndCatch()
    }
}
