package at.hannibal2.skyhanni.features.mining.crystalhollows

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.MiningApi
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName

@SkyHanniModule
object MetalDetectorMute {
    private val METAL_DETECTOR = "DWARVEN_METAL_DETECTOR".toInternalName()

    private val config get() = SkyHanniMod.feature.mining.metalDetector.muteMetalDetectorSound

    @HandleEvent(onlyOnSkyblock = true)
    fun onSound(event: PlaySoundEvent) {
        if (!isEnabled()) return
        if (InventoryUtils.itemInHandId != METAL_DETECTOR) return
        if (event.soundName == "note.harp") {
            event.cancel()
        }
    }

    fun isEnabled() = MiningApi.inCustomMiningIsland() && config
}
