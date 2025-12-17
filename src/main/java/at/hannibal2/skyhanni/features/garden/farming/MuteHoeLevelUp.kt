package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object MuteHoeLevelUp {

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onSound(event: PlaySoundEvent) {
        if (!SkyHanniMod.feature.garden.hoeLevelDisplay.muteHoeSounds) return
        if (event.pitch == 1.4920635f && event.soundName == "portal.travel") event.cancel()
    }
}
