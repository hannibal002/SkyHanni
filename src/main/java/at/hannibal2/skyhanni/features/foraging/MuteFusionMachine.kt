package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object MuteFusionMachine {

    private val sounds = setOf(
        "entity.firework_rocket.blast",
        "entity.firework_rocket.blast_far",
    )

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onPlaySound(event: PlaySoundEvent) {
        if (!SkyHanniMod.feature.foraging.muteFusionMachine) return
        if (event.soundName in sounds && event.volume == 20f) {
            event.cancel()
        }
    }

}
