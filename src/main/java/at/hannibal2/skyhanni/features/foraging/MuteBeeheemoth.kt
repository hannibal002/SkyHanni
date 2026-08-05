package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object MuteBeeheemoth {
    @HandleEvent(onlyOnIsland = IslandType.TORRHUS_CANYON)
    private fun onPlaySound(event: PlaySoundEvent) {
        if (!isEnabled()) return
        when (event.soundName) {
            "entity.bee.hurt" ->
                if ((event.pitch == 1.0f || event.pitch == 1.2f) && event.volume == 1.0f) {
                    event.cancel()
                }

            "entity.bee.loop_aggressive" ->
                if (event.pitch == 1.0f && event.volume == 0.8f) {
                    event.cancel()
                }
        }
    }

    private fun isEnabled() = SkyHanniMod.feature.foraging.muteBeeheemoth
}
