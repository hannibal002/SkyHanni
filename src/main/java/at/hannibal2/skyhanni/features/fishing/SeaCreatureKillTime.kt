package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.fishing.SeaCreatureEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format

@SkyHanniModule
object SeaCreatureKillTime {
    private val config get() = SkyHanniMod.feature.fishing.seaCreatureKillTimer

    @HandleEvent
    fun onSeaCreatureDeath(event: SeaCreatureEvent.Death) {
        if (!config) return
        val seaCreature = event.seaCreature
        ChatUtils.debug("Sea Creature Dead ${seaCreature.name}, ${seaCreature.lastUpdate.passedSince()}")
        if (!seaCreature.isRare) return
        val time = seaCreature.spawnTime.passedSince()
        if (event.seenDeath) {
            ChatUtils.chat("${seaCreature.displayName}§3 took §b${time.format(showMilliSeconds = true)}§e to die.")
        } else {
            val minTime = seaCreature.lastUpdate.passedSince()
            val message = "${seaCreature.displayName}§e took between " +
                "§b${minTime.format(showMilliSeconds = true)} §eand " +
                "§b${time.format(showMilliSeconds = true)}§e to die."
            ChatUtils.chat(message)
        }
    }
}
