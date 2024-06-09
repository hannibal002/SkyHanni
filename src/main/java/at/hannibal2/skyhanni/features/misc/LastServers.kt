package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object LastServers {

    private val config get() = SkyHanniMod.feature.misc.lastServers

    private var lastServers: MutableMap<String, SimpleTimeMark> = mutableMapOf()

    @SubscribeEvent
    fun onWorldSwitch(event: LorenzWorldChangeEvent) {
        if (!isEnabled()) return

        DelayedRun.runDelayed(5.seconds) {
            val serverId = HypixelData.serverId ?: return@runDelayed

            val lastTime = lastServers[serverId]?.passedSince()
            if (lastTime != null) {
                if (lastTime <= config.warnTime.seconds) {
                    ChatUtils.chat("§7You joined this server §e${lastTime.format()}§7 ago.")
                }
            }

            ChatUtils.chat("Adding $serverId to last servers.")

            lastServers[serverId] = SimpleTimeMark.now()
            lastServers.entries.removeIf { it.value.passedSince() > config.warnTime.seconds }
        }
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
