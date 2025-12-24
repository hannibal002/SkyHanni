package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object MuteBanshee {

    @HandleEvent(onlyOnIsland = IslandType.BACKWATER_BAYOU)
    fun onSound(event: PlaySoundEvent) {
        if (!SkyHanniMod.feature.fishing.muteBanshee) return
        if (event.pitch <= 0.4920635 && event.soundName == "entity.ghast.warn") event.cancel()
    }
}
