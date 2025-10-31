package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.HypixelData
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.TimeUtils.format
import kotlin.time.Duration.Companion.seconds

@HanniModule
object LastServers {

    private val config get() = HanniMod.feature.misc.lastServers
    private var lastServerId: String? = null
    private val lastServers = mutableMapOf<String, SimpleTimeMark>()

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return

        val id = HypixelData.serverId ?: return
        // Update the time of the current server if the player is still on the same server.
        // This is necessary because the player can be on the same server for a long time.
        // And if the player leaves the server and joins it again, it still warns the player.
        if (lastServerId == id) {
            lastServers[id] = SimpleTimeMark.now()
            return
        }

        lastServers.entries.removeIf { it.value.passedSince() > config.warnTime.seconds }
        lastServers[id]?.passedSince()?.let {
            ChatUtils.chat("§7You were already on this server §b${it.format()}§7 ago.")
        }
        ChatUtils.debug("Adding $id to last servers.")
        lastServerId = id
        lastServers[id] = SimpleTimeMark.now()
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled
}
