package at.hannibal2.skyhanni.features.commands.tabcomplete

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.jsonobjects.repo.SacksJson
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object GetFromSacksTabComplete {
    private val config get() = SkyHanniMod.feature.commands.tabComplete
    private var sackList = emptyList<String>()
    private val commands = arrayOf("gfs", "getfromsacks")

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        sackList = event.getConstant<SacksJson>("Sacks").sackList
    }

    fun handleTabComplete(command: String): List<String>? {
        if (!isEnabled()) return null
        if (command !in commands) return null

        return sackList.map { it.replace(" ", "_") }
    }

    @SubscribeEvent
    fun onMessageSendToServer(event: MessageSendToServerEvent) {
        if (!isEnabled()) return

        val message = event.message
        if (!commands.any { message.startsWith("/$it ") }) return

        val rawName = message.split(" ")[1]
        val realName = rawName.replace("_", " ")
        if (realName == rawName) return
        if (realName !in sackList) return
        event.isCanceled = true
        LorenzUtils.sendMessageToServer(message.replace(rawName, realName))
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.gfsSack
}
