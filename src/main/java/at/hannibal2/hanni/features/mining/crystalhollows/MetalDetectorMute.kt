package at.hannibal2.hanni.features.mining.crystalhollows

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.MiningApi
import at.hannibal2.hanni.events.ItemInHandChangeEvent
import at.hannibal2.hanni.events.PlaySoundEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.NeuInternalName
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalName

@HanniModule
object MetalDetectorMute {
    private val METAL_DETECTOR = "DWARVEN_METAL_DETECTOR".toInternalName()

    private val config get() = HanniMod.feature.mining.metalDetector.muteMetalDetectorSound

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
