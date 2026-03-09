package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.fishing.SeaCreatureEvent
import at.hannibal2.skyhanni.features.fishing.seaCreatureXMLGui.SeaCreatureSettings
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import net.minecraft.ChatFormatting

@SkyHanniModule
object SeaCreatureKillTime {
    private val config get() = SkyHanniMod.feature.fishing

    @HandleEvent
    fun onSeaCreatureDeath(event: SeaCreatureEvent.Death) {
        if (!config.seaCreatureKillTimer) return
        if (SeaCreatureSettings.getConfig(event.name)?.shouldShowKillTime != true) return
        if (!event.seaCreature.isOwn && config.seaCreatureKillTimerOwnMobsOnly) return
        val seaCreature = event.seaCreature
        val time = seaCreature.spawnTime.passedSince()
        if (event.seenDeath) {
            ChatUtils.chat(
                componentBuilder {
                    append(seaCreature.displayName)
                    append(" took ")
                    append(time.format(showMilliSeconds = true)) {
                        withColor("#FFC600")
                    }
                    append(" to die.")
                }
            )
        } else {
            val minTime = seaCreature.lastUpdate.passedSince()
            ChatUtils.chat(
                componentBuilder {
                    append(seaCreature.displayName)
                    append(" took between ")
                    append(minTime.format(showMilliSeconds = true)) {
                        withColor(ChatFormatting.AQUA)
                    }
                    append(" and ")
                    append(time.format(showMilliSeconds = true)) {
                        withColor(ChatFormatting.AQUA)
                    }
                    append(" to die.")
                }
            )
        }
    }
}
