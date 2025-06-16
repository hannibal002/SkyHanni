package at.hannibal2.skyhanni.features.mining.crystalhollows

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.MiningApi
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LocationUtils

@SkyHanniModule
object HighHeatSound {
    private val config get() = SkyHanniMod.feature.mining

    @HandleEvent
    fun onSound(event: PlaySoundEvent) {
        if (!isEnabled()) return
        if (LocationUtils.playerLocation().y > 65.0 || MiningApi.heat < 90) return

        if (event.soundName == "mob.wolf.panting" && event.pitch == 0.0f && event.volume == 1.0f) event.cancel()
    }

    private fun isEnabled() = config.muteHighHeat && MiningApi.inCrystalHollows()
}
