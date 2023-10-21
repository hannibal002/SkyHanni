package at.hannibal2.skyhanni.features.commands.tabcomplete

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.jsonobjects.SackListJson
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object GetFromSacksTabComplete {
    private val config get() = SkyHanniMod.feature.commands.tabComplete
    private var sackList = emptyList<String>()
    private val commands = arrayOf("gfs", "getfromsacks")

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        sackList = event.getConstant<SackListJson>("Sacks").sackList
    }

    fun handleTabComplete(command: String): List<String>? {
        if (!isEnabled()) return null
        if (command !in commands) return null

        return sackList.map { it.replace(" ", "_") }
    }

    @SubscribeEvent
    fun onSendPacket(event: PacketEvent.SendEvent) {
        if (!isEnabled()) return

        val packet = event.packet as? C01PacketChatMessage ?: return
        val message = packet.message
        if (commands.any { message.startsWith("/$it ") }) {
            val rawName = message.split(" ")[1]
            val realName = rawName.replace("_", " ")
            if (realName == rawName) return
            if (realName !in sackList) return
            event.isCanceled = true
            LorenzUtils.sendMessageToServer(message.replace(rawName, realName))
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.gfsSack
}
