package at.hannibal2.hanni.features.mining.crystalhollows

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.MiningApi
import at.hannibal2.hanni.events.PlaySoundEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.LocationUtils

@HanniModule
object HighHeatSound {
    private val config get() = HanniMod.feature.mining

    @HandleEvent
    fun onSound(event: PlaySoundEvent) {
        if (!isEnabled()) return
        if (LocationUtils.playerLocation().y > 65.0 || MiningApi.heat < 90) return

        if (event.soundName == "mob.wolf.panting" && event.pitch == 0.0f && event.volume == 1.0f) event.cancel()
    }

    private fun isEnabled() = config.muteHighHeat && MiningApi.inCrystalHollows()
}
