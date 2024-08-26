package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object LastServers {

    private val config get() = SkyHanniMod.feature.misc.lastServers
    private var lastServerId: String? = null
    private var lastServers = mutableMapOf<String, SimpleTimeMark>()

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return

        val serverId = HypixelData.serverId ?: return

        if (lastServerId == serverId) return

        lastServers.entries.removeIf { it.value.passedSince() > config.warnTime.seconds }

        lastServers[serverId]?.passedSince()?.let {
            ChatUtils.chat("§7You already joined this server §e${it.format()}§7 ago.")
        }

        ChatUtils.debug("Adding $serverId to last servers.")

        lastServers[serverId] = SimpleTimeMark.now()
        lastServerId = serverId
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
