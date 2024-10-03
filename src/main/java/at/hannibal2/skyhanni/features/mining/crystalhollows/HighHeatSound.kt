package at.hannibal2.skyhanni.features.mining.crystalhollows

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.MiningAPI
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LocationUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object HighHeatSound {
    private val config get() = SkyHanniMod.feature.mining

    @SubscribeEvent
    fun onSound(event: PlaySoundEvent) {
        if (!isEnabled()) return
        if (!(LocationUtils.playerLocation().y <= 65.0 && MiningAPI.heat >= 90)) return

        if (event.soundName == "mob.wolf.panting" && event.pitch == 0.0f && event.volume == 1.0f) event.cancel()
    }

    private fun isEnabled() = config.muteHighHeat && MiningAPI.inCrystalHollows()
}
