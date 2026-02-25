package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object MutePhantom {

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onSound(event: PlaySoundEvent) {
        if (!SkyHanniMod.feature.foraging.mutePhantoms) return
        if (event.soundName.contains("entity.phantom.")) {
            event.cancel()
        }
    }

}
