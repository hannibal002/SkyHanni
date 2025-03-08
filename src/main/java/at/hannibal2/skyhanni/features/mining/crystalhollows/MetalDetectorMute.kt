package at.hannibal2.skyhanni.features.mining.crystalhollows

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MiningApi
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName

@SkyHanniModule
object MetalDetectorMute {
    private val METAL_DETECTOR = "DWARVEN_METAL_DETECTOR".toInternalName()

    private val config get() = SkyHanniMod.feature.mining.metalDetector.muteMetalDetectorSound

    private var currentItem: NeuInternalName? = null
    private var oldItem: NeuInternalName? = null

    @HandleEvent(onlyOnIsland = IslandType.CRYSTAL_HOLLOWS)
    fun onItemChange(event: ItemInHandChangeEvent) {
        oldItem = event.oldItem
        currentItem = event.newItem
    }

    @HandleEvent(onlyOnIsland = IslandType.CRYSTAL_HOLLOWS)
    fun onSound(event: PlaySoundEvent) {
        if (!isEnabled()) return
        if (oldItem != METAL_DETECTOR && currentItem != METAL_DETECTOR) return
        if (event.soundName == "note.harp") {
            event.cancel()
        }
    }

    fun isEnabled() = config && MiningApi.inMinesOfDivan()
}
