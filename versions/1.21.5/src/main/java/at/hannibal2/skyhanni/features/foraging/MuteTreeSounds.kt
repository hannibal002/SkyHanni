package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object MuteTreeSounds {
    val config get() = SkyHanniMod.feature.foraging

    @HandleEvent(onlyOnSkyblock = true)
    fun onPlaySound(event: PlaySoundEvent) {
        if (event.soundName == "entity.creaking.death" && config.muteTreeBreaking) {
            if (IslandType.GALATEA.isCurrent() && !config.muteTreeBreakingOnGalatea) return
            event.cancel()
        }
    }
}
