package at.hannibal2.skyhanni.features.commands.tabcomplete

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.GetFromSackAPI
import at.hannibal2.skyhanni.api.GetFromSackAPI.commands
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ChatUtils.isCommand
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object GetFromSacksTabComplete {

    private val config get() = SkyHanniMod.feature.commands.tabComplete

    fun handleTabComplete(command: String): List<String>? {
        if (!isEnabled()) return null
        if (command !in commands) return null

        return GetFromSackAPI.sackList.map { it.asString() }
    }

    @SubscribeEvent
    fun onMessageSendToServer(event: MessageSendToServerEvent) {
        if (!isEnabled()) return

        if (!event.isCommand(GetFromSackAPI.commandsWithSlash)) return

        val rawName = event.splitMessage[1]
        val realName = rawName.asInternalName()
        if (realName.asString() == rawName) return
        if (realName !in GetFromSackAPI.sackList) return
        event.isCanceled = true
        ChatUtils.sendMessageToServer(event.message.replace(rawName, realName.asString()))
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.gfsSack
}
