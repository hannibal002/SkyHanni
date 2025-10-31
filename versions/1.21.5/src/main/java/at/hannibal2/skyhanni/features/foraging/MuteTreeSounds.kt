package at.hannibal2.hanni.features.foraging

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.PlaySoundEvent
import at.hannibal2.hanni.hannimodule.HanniModule

@HanniModule
object MuteTreeSounds {
    val config get() = HanniMod.feature.foraging.trees

    @HandleEvent(onlyOnSkyblock = true)
    fun onPlaySound(event: PlaySoundEvent) {
        if (event.soundName == "entity.creaking.death" && config.muteBreaking) {
            if (IslandType.GALATEA.isCurrent() && !config.muteBreakingOnGalatea) return
            event.cancel()
        }
    }
}
