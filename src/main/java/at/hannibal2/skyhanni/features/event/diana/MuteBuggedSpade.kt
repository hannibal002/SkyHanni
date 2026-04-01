package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object MuteBuggedSpade {

    private val config get() = SkyHanniMod.feature.event.diana

    @HandleEvent
    fun onPlaySound(event: PlaySoundEvent) {
        if (!config.muteBuggedSpade || !DianaApi.isDoingDiana()) return
        val isRealMusic = event.pitch == 1f && event.volume == 1f && event.location.isZero()
        if (event.soundName == "music.overworld.desert" && !isRealMusic) {
            event.cancel()
        }
    }
}
