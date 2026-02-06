package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object MuteFusionMachine {

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onSound(event: PlaySoundEvent) {
        if (!SkyHanniMod.feature.foraging.muteFusionMachine) return
        if (event.soundName == "entity.firework_rocket.blast" && event.volume == 20f) {
            event.cancel()
        }
    }

}
