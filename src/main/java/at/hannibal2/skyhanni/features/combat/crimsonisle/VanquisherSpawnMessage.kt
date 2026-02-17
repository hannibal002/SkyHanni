package at.hannibal2.skyhanni.features.combat.crimsonisle

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.events.combat.VanquisherEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LocationUtils

@SkyHanniModule
object VanquisherSpawnMessage {
    val isEnabled get() = SkyHanniMod.feature.crimsonIsle.shareVanquishersInParty

    @HandleEvent
    fun onOwnVanquisherSpawn(event: VanquisherEvent.OwnSpawn) {
        if (!isEnabled) return
        if (!PartyApi.isInParty()) return

        HypixelCommands.partyChat("I spawned a Vanquisher at ${LocationUtils.playerLocation().toCleanString()}")
    }
}
