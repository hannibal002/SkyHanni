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
    private val lastServers = mutableMapOf<String, SimpleTimeMark>()

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled() || HypixelData.serverId == lastServerId) return

        val id = HypixelData.serverId ?: return
        lastServers.entries.removeIf { it.value.passedSince() > config.warnTime.seconds }
        lastServers[id]?.passedSince()?.let {
            ChatUtils.chat("§7You already joined this server §b${it.format()}§7 ago.")
        }
        ChatUtils.debug("Adding $id to last servers.")
        lastServers[id] = SimpleTimeMark.now()
        lastServerId = id
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
